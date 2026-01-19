package ru.realite.models.service;

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
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
import ru.realite.models.config.ModelsConfig;
import ru.realite.models.config.ModelsConfig.AttachmentSpec;
import ru.realite.models.config.ModelsConfig.HorseAppearance;
import ru.realite.models.config.ModelsConfig.ModelDefinition;

public final class ModelWrapperService implements Listener {

    private static final NamespacedKey DISPLAY_MODEL_ID_KEY = key("realite:modelId");
    private static final NamespacedKey DISPLAY_WRAPPER_ID_KEY = key("realite:wrapperId");
    private static final NamespacedKey DISPLAY_WRAPPER_TARGET_KEY = key("realite:wrapperTarget");
    private static final NamespacedKey VANILLA_MODEL_ID_KEY = key("realite:model_id");
    private static final NamespacedKey VANILLA_ATTACHMENT_ID_KEY = key("realite:model_attachment_uuid");
    private static final NamespacedKey VANILLA_MODEL_VERSION_KEY = key("realite:model_version");
    private static final int VANILLA_MODEL_VERSION = 1;

    private final JavaPlugin plugin;
    private final Map<UUID, WrapperState> wrappers = new ConcurrentHashMap<>();
    private final Map<UUID, VanillaState> vanillaModels = new ConcurrentHashMap<>();
    private final Supplier<ModelsConfig> modelsConfigSupplier;
    private BukkitTask syncTask;
    private Consumer<UUID> appliedRemover = id -> {
    };

    public ModelWrapperService(JavaPlugin plugin, Supplier<ModelsConfig> modelsConfigSupplier) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.modelsConfigSupplier = Objects.requireNonNull(modelsConfigSupplier, "modelsConfigSupplier");
    }

    public void setAppliedRemover(Consumer<UUID> appliedRemover) {
        this.appliedRemover = Objects.requireNonNull(appliedRemover, "appliedRemover");
    }

    public void start() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        cleanupExistingWrappers();
        bootstrapVanillaModels();
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
            return ApplyResult.failed("Model renderer hint is not DISPLAY for model: " + assetInfo.asset().modelId());
        }

        ItemDisplay wrapper = ensureWrapper(target, assetInfo);
        if (wrapper == null) {
            return ApplyResult.failed("Failed to create display wrapper for model: " + assetInfo.asset().modelId());
        }

        UUID wrapperId = wrapper.getUniqueId();
        wrappers.put(target.getUniqueId(), new WrapperState(target.getUniqueId(), wrapperId, assetInfo));
        markTarget(target, assetInfo.asset().modelId(), wrapperId);
        markWrapper(wrapper, assetInfo.asset().modelId(), target.getUniqueId());
        syncWrapper(target, wrapper, assetInfo.asset().visualProfile());
        return ApplyResult.applied();
    }

    public ApplyResult applyModel(Entity target, String modelId) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(modelId, "modelId");

        if (!(target instanceof Horse horse)) {
            return ApplyResult.failed("Target must be a horse.");
        }

        ModelDefinition definition = resolveDefinition(modelId).orElse(null);
        if (definition == null) {
            return ApplyResult.failed("Model not found: " + modelId);
        }
        if (!definition.matchesEntity(target.getType())) {
            return ApplyResult.failed("Model " + modelId + " cannot be applied to " + target.getType());
        }

        clearModel(target);
        ItemDisplay attachment = ensureVanillaAttachment(horse, definition);
        if (attachment == null) {
            return ApplyResult.failed("Failed to create attachment for model: " + modelId);
        }

        vanillaModels.put(target.getUniqueId(), new VanillaState(target.getUniqueId(), modelId, attachment.getUniqueId()));
        markVanillaModel(target, modelId, attachment.getUniqueId());
        return ApplyResult.applied();
    }

    public void clear(Entity target) {
        Objects.requireNonNull(target, "target");
        WrapperState state = wrappers.remove(target.getUniqueId());
        if (state != null) {
            removeWrapperEntity(state.wrapperId());
        }
        clearDisplayMarkers(target);
        appliedRemover.accept(target.getUniqueId());
    }

    public void clearModel(Entity target) {
        Objects.requireNonNull(target, "target");
        VanillaState state = vanillaModels.remove(target.getUniqueId());
        if (state != null) {
            removeWrapperEntity(state.attachmentId());
        } else {
            removeVanillaAttachment(target);
        }
        clearVanillaMarkers(target);
    }

    public void reapplyIfNeeded(Entity target) {
        Objects.requireNonNull(target, "target");
        if (!(target instanceof Horse horse)) {
            return;
        }

        PersistentDataContainer pdc = target.getPersistentDataContainer();
        String modelId = pdc.get(VANILLA_MODEL_ID_KEY, PersistentDataType.STRING);
        if (modelId == null || modelId.isBlank()) {
            return;
        }

        ModelDefinition definition = resolveDefinition(modelId).orElse(null);
        if (definition == null || !definition.matchesEntity(target.getType())) {
            clearModel(target);
            return;
        }

        ItemDisplay attachment = resolveVanillaAttachment(pdc).orElse(null);
        if (attachment == null) {
            attachment = ensureVanillaAttachment(horse, definition);
            if (attachment == null) {
                return;
            }
        } else {
            updateVanillaAttachment(horse, attachment, definition);
        }

        vanillaModels.put(target.getUniqueId(), new VanillaState(target.getUniqueId(), modelId, attachment.getUniqueId()));
        markVanillaModel(target, modelId, attachment.getUniqueId());
    }

    public Optional<ModelInstanceInfo> getModelInfo(Entity target) {
        Objects.requireNonNull(target, "target");
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        String modelId = pdc.get(VANILLA_MODEL_ID_KEY, PersistentDataType.STRING);
        if (modelId == null) {
            return Optional.empty();
        }
        UUID attachmentId = parseUuid(pdc.get(VANILLA_ATTACHMENT_ID_KEY, PersistentDataType.STRING)).orElse(null);
        Integer storedVersion = pdc.get(VANILLA_MODEL_VERSION_KEY, PersistentDataType.INTEGER);
        int version = storedVersion == null ? 0 : storedVersion;
        return Optional.of(new ModelInstanceInfo(modelId, attachmentId, version));
    }

    public void clearAll() {
        for (WrapperState state : wrappers.values()) {
            removeWrapperEntity(state.wrapperId());
            Entity target = Bukkit.getEntity(state.targetId());
            if (target != null) {
                clearDisplayMarkers(target);
            }
            appliedRemover.accept(state.targetId());
        }
        wrappers.clear();

        for (VanillaState state : vanillaModels.values()) {
            removeWrapperEntity(state.attachmentId());
            Entity target = Bukkit.getEntity(state.targetId());
            if (target != null) {
                clearVanillaMarkers(target);
            }
        }
        vanillaModels.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (wrappers.containsKey(entity.getUniqueId())) {
            clear(entity);
        }
        if (vanillaModels.containsKey(entity.getUniqueId())) {
            clearModel(entity);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityRemove(EntityRemoveFromWorldEvent event) {
        Entity entity = event.getEntity();
        UUID entityId = entity.getUniqueId();

        if (wrappers.containsKey(entityId)) {
            clear(entity);
        } else if (isDisplayWrapperEntity(entity)) {
            removeWrapperEntity(entityId);
        } else if (entity instanceof ItemDisplay) {
            handleVanillaAttachmentRemoval(entityId);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        tryAutoApply(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            reapplyIfNeeded(entity);
            tryAutoApply(entity);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            UUID entityId = entity.getUniqueId();
            if (wrappers.containsKey(entityId)) {
                clear(entity);
            } else if (isDisplayWrapperEntity(entity)) {
                removeWrapperEntity(entityId);
            } else {
                vanillaModels.remove(entityId);
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

        syncVanillaModels();
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
                    if (isDisplayWrapperEntity(entity)) {
                        entity.remove();
                        continue;
                    }
                    if (hasDisplayMarkers(entity)) {
                        clearDisplayMarkers(entity);
                    }
                }
            }
        });
    }

    private void markTarget(Entity target, String modelId, UUID wrapperId) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.set(DISPLAY_MODEL_ID_KEY, PersistentDataType.STRING, modelId);
        pdc.set(DISPLAY_WRAPPER_ID_KEY, PersistentDataType.STRING, wrapperId.toString());
    }

    private void markWrapper(Entity wrapper, String modelId, UUID targetId) {
        PersistentDataContainer pdc = wrapper.getPersistentDataContainer();
        pdc.set(DISPLAY_MODEL_ID_KEY, PersistentDataType.STRING, modelId);
        pdc.set(DISPLAY_WRAPPER_ID_KEY, PersistentDataType.STRING, wrapper.getUniqueId().toString());
        pdc.set(DISPLAY_WRAPPER_TARGET_KEY, PersistentDataType.STRING, targetId.toString());
    }

    private void clearDisplayMarkers(Entity target) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.remove(DISPLAY_MODEL_ID_KEY);
        pdc.remove(DISPLAY_WRAPPER_ID_KEY);
        pdc.remove(DISPLAY_WRAPPER_TARGET_KEY);
    }

    private boolean isDisplayWrapperEntity(Entity entity) {
        return entity.getPersistentDataContainer().has(DISPLAY_WRAPPER_ID_KEY, PersistentDataType.STRING)
                && entity instanceof ItemDisplay;
    }

    private boolean hasDisplayMarkers(Entity entity) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        return pdc.has(DISPLAY_MODEL_ID_KEY, PersistentDataType.STRING)
                || pdc.has(DISPLAY_WRAPPER_ID_KEY, PersistentDataType.STRING);
    }

    private void removeWrapperEntity(UUID wrapperId) {
        Entity wrapper = Bukkit.getEntity(wrapperId);
        if (wrapper != null) {
            wrapper.remove();
        }
    }

    private void bootstrapVanillaModels() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (var world : Bukkit.getWorlds()) {
                for (Entity entity : world.getEntities()) {
                    reapplyIfNeeded(entity);
                    tryAutoApply(entity);
                }
            }
        });
    }

    private void syncVanillaModels() {
        for (VanillaState state : vanillaModels.values()) {
            Entity target = Bukkit.getEntity(state.targetId());
            if (!(target instanceof Horse horse) || target.isDead()) {
                removeWrapperEntity(state.attachmentId());
                vanillaModels.remove(state.targetId());
                continue;
            }

            ModelDefinition definition = resolveDefinition(state.modelId()).orElse(null);
            if (definition == null || !definition.matchesEntity(target.getType())) {
                clearModel(target);
                continue;
            }

            Entity attachment = Bukkit.getEntity(state.attachmentId());
            if (!(attachment instanceof ItemDisplay display)) {
                ItemDisplay recreated = ensureVanillaAttachment(horse, definition);
                if (recreated != null) {
                    vanillaModels.put(state.targetId(), new VanillaState(
                            state.targetId(),
                            state.modelId(),
                            recreated.getUniqueId()));
                    markVanillaModel(target, state.modelId(), recreated.getUniqueId());
                }
                continue;
            }

            updateVanillaAttachment(horse, display, definition);
        }
    }

    private void tryAutoApply(Entity entity) {
        if (!(entity instanceof Horse horse)) {
            return;
        }
        if (hasVanillaModel(entity)) {
            return;
        }
        ModelsConfig config = modelsConfigSupplier.get();
        if (config == null) {
            return;
        }
        Optional<ModelDefinition> matched = config.matchingFor(entity);
        matched.ifPresent(definition -> applyModel(horse, definition.id()));
    }

    private boolean hasVanillaModel(Entity entity) {
        return entity.getPersistentDataContainer().has(VANILLA_MODEL_ID_KEY, PersistentDataType.STRING);
    }

    private Optional<ModelDefinition> resolveDefinition(String modelId) {
        ModelsConfig config = modelsConfigSupplier.get();
        if (config == null) {
            return Optional.empty();
        }
        return config.find(modelId);
    }

    private ItemDisplay ensureVanillaAttachment(Horse horse, ModelDefinition definition) {
        PersistentDataContainer pdc = horse.getPersistentDataContainer();
        ItemDisplay existing = resolveVanillaAttachment(pdc).orElse(null);
        if (existing != null) {
            updateVanillaAttachment(horse, existing, definition);
            return existing;
        }

        return horse.getWorld().spawn(horse.getLocation(), ItemDisplay.class, display -> {
            updateVanillaAttachment(horse, display, definition);
            display.setPersistent(false);
            display.setTeleportDuration(2);
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
            display.setBillboard(Display.Billboard.CENTER);
        });
    }

    private void updateVanillaAttachment(Horse horse, ItemDisplay display, ModelDefinition definition) {
        AttachmentSpec attachment = definition.attachment();
        ItemStack stack = new ItemStack(attachment.material());
        if (attachment.customModelData() != null) {
            var meta = stack.getItemMeta();
            meta.setCustomModelData(attachment.customModelData());
            stack.setItemMeta(meta);
        }
        display.setItemStack(stack);
        display.setTransformation(buildTransformation(attachment));
        display.setBillboard(Display.Billboard.CENTER);

        if (display.getVehicle() != horse) {
            horse.addPassenger(display);
        }

        HorseAppearance appearance = definition.horseAppearance();
        if (appearance != null) {
            if (appearance.color() != null) {
                horse.setColor(appearance.color());
            }
            if (appearance.style() != null) {
                horse.setStyle(appearance.style());
            }
        }
    }

    private Transformation buildTransformation(AttachmentSpec attachment) {
        Vector3f translation = attachment.offset();
        Vector3f rotation = attachment.rotation();
        Vector3f scale = attachment.scale();
        Quaternionf leftRotation = new Quaternionf().rotationXYZ(
                (float) Math.toRadians(rotation.x()),
                (float) Math.toRadians(rotation.y()),
                (float) Math.toRadians(rotation.z()));
        return new Transformation(
                new Vector3f(translation),
                leftRotation,
                new Vector3f(scale),
                new Quaternionf());
    }

    private Optional<ItemDisplay> resolveVanillaAttachment(PersistentDataContainer pdc) {
        String raw = pdc.get(VANILLA_ATTACHMENT_ID_KEY, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return parseUuid(raw)
                .map(Bukkit::getEntity)
                .filter(ItemDisplay.class::isInstance)
                .map(ItemDisplay.class::cast);
    }

    private void markVanillaModel(Entity target, String modelId, UUID attachmentId) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.set(VANILLA_MODEL_ID_KEY, PersistentDataType.STRING, modelId);
        pdc.set(VANILLA_ATTACHMENT_ID_KEY, PersistentDataType.STRING, attachmentId.toString());
        pdc.set(VANILLA_MODEL_VERSION_KEY, PersistentDataType.INTEGER, VANILLA_MODEL_VERSION);
    }

    private void clearVanillaMarkers(Entity target) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        pdc.remove(VANILLA_MODEL_ID_KEY);
        pdc.remove(VANILLA_ATTACHMENT_ID_KEY);
        pdc.remove(VANILLA_MODEL_VERSION_KEY);
    }

    private void removeVanillaAttachment(Entity target) {
        PersistentDataContainer pdc = target.getPersistentDataContainer();
        String raw = pdc.get(VANILLA_ATTACHMENT_ID_KEY, PersistentDataType.STRING);
        parseUuid(raw).ifPresent(this::removeWrapperEntity);
    }

    private void handleVanillaAttachmentRemoval(UUID attachmentId) {
        for (VanillaState state : vanillaModels.values()) {
            if (state.attachmentId().equals(attachmentId)) {
                vanillaModels.remove(state.targetId());
                Entity target = Bukkit.getEntity(state.targetId());
                if (target != null) {
                    reapplyIfNeeded(target);
                }
                return;
            }
        }
    }

    private Optional<UUID> parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
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

    private record VanillaState(UUID targetId, String modelId, UUID attachmentId) {
    }

    public record ModelInstanceInfo(String modelId, UUID attachmentId, int version) {
    }
}
