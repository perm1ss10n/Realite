package ru.realite.models.service;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.realite.core.api.models.ApplyResult;
import ru.realite.core.api.models.ModelAssetInfo;
import ru.realite.core.api.models.ModelDisplaySpec;
import ru.realite.core.api.models.ModelRendererHint;
import ru.realite.core.api.models.ModelVisualProfile;

public final class ModelWrapperService implements Listener {

    private static final NamespacedKey MODEL_ID_KEY = key("realite:modelId");
    private static final NamespacedKey WRAPPER_ID_KEY = key("realite:wrapperId");
    private static final NamespacedKey WRAPPER_TARGET_KEY = key("realite:wrapperTarget");

    private final JavaPlugin plugin;
    private final Map<UUID, WrapperState> wrappers = new ConcurrentHashMap<>();
    private BukkitTask syncTask;
    private Consumer<UUID> appliedRemover = id -> {
    };

    public ModelWrapperService(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void setAppliedRemover(Consumer<UUID> appliedRemover) {
        this.appliedRemover = Objects.requireNonNull(appliedRemover, "appliedRemover");
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        cleanupExistingWrappers();
        syncTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::syncWrappers,
                5L,
                5L);
    }

    public void stop() {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        clearAll();
        HandlerList.unregisterAll(this);
    }

    public ApplyResult apply(Entity target, ModelAssetInfo assetInfo) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(assetInfo, "assetInfo");

        if (assetInfo.asset().rendererHint() != ModelRendererHint.DISPLAY) {
            return ApplyResult.fail("Model renderer hint is not DISPLAY for model: " + assetInfo.asset().modelId());
        }

        ItemDisplay wrapper = ensureWrapper(target, assetInfo);
        if (wrapper == null) {
            return ApplyResult.fail("Failed to create display wrapper for model: " + assetInfo.asset().modelId());
        }

        UUID wrapperId = wrapper.getUniqueId();
        wrappers.put(target.getUniqueId(), new WrapperState(target.getUniqueId(), wrapperId, assetInfo));
        markTarget(target, assetInfo.asset().modelId(), wrapperId);
        markWrapper(wrapper, assetInfo.asset().modelId(), target.getUniqueId());
        syncWrapper(target, wrapper, assetInfo.asset().visualProfile());
        return ApplyResult.ok();
    }

    public void clear(Entity target) {
        Objects.requireNonNull(target, "target");
        WrapperState state = wrappers.remove(target.getUniqueId());
        if (state != null) {
            removeWrapperEntity(state.wrapperId());
        }
        clearMarkers(target);
        appliedRemover.accept(target.getUniqueId());
    }

    public void clearAll() {
        for (WrapperState state : wrappers.values()) {
            removeWrapperEntity(state.wrapperId());
            Entity target = Bukkit.getEntity(state.targetId());
            if (target != null) {
                clearMarkers(target);
            }
            appliedRemover.accept(state.targetId());
        }
        wrappers.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (wrappers.containsKey(entity.getUniqueId())) {
            clear(entity);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();

        if (wrappers.containsKey(entityId)) {
            clear(entity);
        } else if (isWrapperEntity(entity)) {
            removeWrapperEntity(entityId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            UUID entityId = entity.getUniqueId();
            if (wrappers.containsKey(entityId)) {
                clear(entity);
            } else if (isWrapperEntity(entity)) {
                removeWrapperEntity(entityId);
            }
        }
    }

    private void syncWrappers() {
        for (WrapperState state : wrappers.values()) {
            Entity target = Bukkit.getEntity(state.targetId());
            if (target == null || target.isDead()) {
                removeWrapperEntity(state.wrapperId());
                wrappers.remove(state.targetId());
                appliedRemover.accept(state.targetId());
                continue;
            }

            Entity wrapperEntity = Bukkit.getEntity(state.wrapperId());
            if (!(wrapperEntity instanceof ItemDisplay wrapper)) {
                ItemDisplay recreated = ensureWrapper(target, state.assetInfo());
                if (recreated != null) {
                    wrappers.put(state.targetId(), new WrapperState(
                            state.targetId(),
                            recreated.getUniqueId(),
                            state.assetInfo()));
                    markTarget(target, state.assetInfo().asset().modelId(), recreated.getUniqueId());
                    markWrapper(recreated, state.assetInfo().asset().modelId(), target.getUniqueId());
                } else {
                    wrappers.remove(state.targetId());
                    appliedRemover.accept(state.targetId());
                }
                continue;
            }

            syncWrapper(target, wrapper, state.assetInfo().asset().visualProfile());
        }
    }

    private ItemDisplay ensureWrapper(Entity target, ModelAssetInfo assetInfo) {
        WrapperState existing = wrappers.get(target.getUniqueId());
        if (existing != null) {
            Entity current = Bukkit.getEntity(existing.wrapperId());
            if (current instanceof ItemDisplay display) {
                updateDisplayItem(display, assetInfo.asset().displaySpec());
                return display;
            }
        }

        return target.getWorld().spawn(target.getLocation(), ItemDisplay.class, display -> {
            updateDisplayItem(display, assetInfo.asset().displaySpec());
            display.setPersistent(false);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
            display.setBillboard(Display.Billboard.CENTER);
        });
    }

    private void syncWrapper(Entity target, ItemDisplay wrapper, ModelVisualProfile profile) {
        wrapper.teleport(target.getLocation());
        wrapper.setTransformation(buildTransformation(profile));
    }

    private void updateDisplayItem(ItemDisplay display, ModelDisplaySpec spec) {
        ItemStack stack = new ItemStack(spec.material());
        if (spec.customModelData() != null) {
            var meta = stack.getItemMeta();
            meta.setCustomModelData(spec.customModelData());
            stack.setItemMeta(meta);
        }
        display.setItemStack(stack);
    }

    private Transformation buildTransformation(ModelVisualProfile profile) {
        float scale = (float) profile.scale();
        Vector3f translation = new Vector3f(
                (float) profile.offset().x(),
                (float) profile.offset().y(),
                (float) profile.offset().z());
        return new Transformation(
                translation,
                new Quaternionf(),
                new Vector3f(scale, scale, scale),
                new Quaternionf());
    }

    private void cleanupExistingWrappers() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (var world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    if (isWrapperEntity(entity)) {
                        entity.remove();
                        continue;
                    }
                    if (hasModelMarkers(entity)) {
                        clearMarkers(entity);
                    }
                }
            }
        });
    }

    private void markTarget(Entity target, String modelId, UUID wrapperId) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.set(MODEL_ID_KEY, PersistentDataType.STRING, modelId);
        pdc.set(WRAPPER_ID_KEY, PersistentDataType.STRING, wrapperId.toString());
    }

    private void markWrapper(Entity wrapper, String modelId, UUID targetId) {
        PersistentDataContainer pdc = wrapper.getPersistentDataContainer();
        pdc.set(MODEL_ID_KEY, PersistentDataType.STRING, modelId);
        pdc.set(WRAPPER_ID_KEY, PersistentDataType.STRING, wrapper.getUniqueId().toString());
        pdc.set(WRAPPER_TARGET_KEY, PersistentDataType.STRING, targetId.toString());
    }

    private void clearMarkers(Entity target) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.remove(MODEL_ID_KEY);
        pdc.remove(WRAPPER_ID_KEY);
        pdc.remove(WRAPPER_TARGET_KEY);
    }

    private boolean isWrapperEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(WRAPPER_ID_KEY, PersistentDataType.STRING)
                && entity instanceof ItemDisplay;
    }

    private boolean hasModelMarkers(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(MODEL_ID_KEY, PersistentDataType.STRING)
                || pdc.has(WRAPPER_ID_KEY, PersistentDataType.STRING);
    }

    private void removeWrapperEntity(UUID wrapperId) {
        Entity wrapper = Bukkit.getEntity(wrapperId);
        if (wrapper != null) {
            wrapper.remove();
        }
    }

    private static NamespacedKey key(String raw) {
        NamespacedKey key = NamespacedKey.fromString(raw);
        if (key == null) {
            throw new IllegalStateException("Invalid namespaced key: " + raw);
        }
        return key;
    }

    private record WrapperState(UUID targetId, UUID wrapperId, ModelAssetInfo assetInfo) {
    }
}
