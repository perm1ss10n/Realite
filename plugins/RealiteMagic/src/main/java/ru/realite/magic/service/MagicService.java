package ru.realite.magic.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassProfile;
import ru.realite.core.api.classes.ClassProfileProvider;
import ru.realite.items.service.ItemService;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.MageState;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.spell.SpellCaster;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.spell.SpellRequirements;

public final class MagicService {

    private static final String GLOBAL_COOLDOWN_KEY = "global";
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final MagicMessages messages;
    private final SpellRegistry spellRegistry;
    private final SpellCaster caster;
    private final Map<UUID, MageState> states = new HashMap<>();
    private final SpellSelectMenu spellSelectMenu;
    private BukkitTask regenTask;
    private boolean itemBridgeWarned;

    public MagicService(JavaPlugin plugin,
                        MagicMessages messages,
                        SpellRegistry spellRegistry,
                        PlayerSpellService playerSpellService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.caster = new SpellCaster(this, messages);
        this.spellSelectMenu = new SpellSelectMenu(plugin, spellRegistry, playerSpellService, messages);
    }

    public void start() {
        if (regenTask != null) {
            return;
        }
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRegen, 20L, 20L);
    }

    public void stop() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
    }

    public SpellRegistry spellRegistry() {
        return spellRegistry;
    }

    public double getMana(Player player) {
        return state(player).mana();
    }

    public double getMaxMana(Player player) {
        return state(player).maxMana();
    }

    public void setMana(Player player, double mana) {
        MageState state = state(player);
        state.mana(clamp(mana, 0, state.maxMana()));
    }

    public void addMana(Player player, double amount) {
        setMana(player, getMana(player) + amount);
    }

    public boolean canCast(Player player, SpellDefinition spell) {
        if (spell == null) {
            return false;
        }
        if (!meetsRequirements(player, spell)) {
            return false;
        }
        if (isOnCooldown(player, GLOBAL_COOLDOWN_KEY)) {
            return false;
        }
        if (isOnCooldown(player, spell.id())) {
            return false;
        }
        return getMana(player) >= spell.mana();
    }

    public boolean spendMana(Player player, SpellDefinition spell) {
        if (!canCast(player, spell)) {
            return false;
        }
        addMana(player, -spell.mana());
        setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
        setCooldown(player, spell.id(), spell.cooldownTicks());
        return true;
    }

    public boolean isOnCooldown(Player player, String key) {
        MageState state = state(player);
        Long until = state.cooldowns().get(key);
        return until != null && until > System.currentTimeMillis();
    }

    public boolean isOnGlobalCooldown(Player player) {
        return isOnCooldown(player, GLOBAL_COOLDOWN_KEY);
    }

    public void setCooldown(Player player, String key, long ticks) {
        if (ticks <= 0) {
            return;
        }
        MageState state = state(player);
        long until = System.currentTimeMillis() + ticks * 50L;
        state.cooldowns().put(key, until);
    }

    public long remainingCooldownTicks(Player player, String key) {
        MageState state = state(player);
        Long until = state.cooldowns().get(key);
        if (until == null) {
            return 0;
        }
        long remainingMs = until - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0;
        }
        return (long) Math.ceil(remainingMs / 50.0);
    }

    public long remainingGlobalCooldownTicks(Player player) {
        return remainingCooldownTicks(player, GLOBAL_COOLDOWN_KEY);
    }

    public void markCombat(Player player) {
        state(player).lastCombatTime(System.currentTimeMillis());
    }

    public void cleanup(Player player) {
        states.remove(player.getUniqueId());
    }

    public void cast(Player player, SpellDefinition spell) {
        if (!meetsRequirements(player, spell)) {
            sendRequirementMessage(player, spell);
            return;
        }
        caster.cast(player, spell);
    }

    public MagicMessages messages() {
        return messages;
    }

    public SpellSelectMenu spellSelectMenu() {
        return spellSelectMenu;
    }

    public void setSelectedSpell(Player player, String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return;
        }
        state(player).selectedSpellId(spellId);
    }

    public void clearSelectedSpell(Player player) {
        state(player).selectedSpellId(null);
    }

    public String getSelectedSpellId(Player player) {
        return state(player).selectedSpellId();
    }

    public SpellDefinition getSelectedSpell(Player player) {
        String spellId = getSelectedSpellId(player);
        if (spellId == null || spellId.isBlank()) {
            return null;
        }
        return spellRegistry.get(spellId);
    }

    public boolean meetsRequirements(Player player, SpellDefinition spell) {
        if (spell == null) {
            return false;
        }
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        String requiredItemId = requirements.requiredItemId();
        if (requiredItemId != null && !requiredItemId.isBlank()) {
            if (!hasRequiredItem(player, requiredItemId)) {
                return false;
            }
        }
        ClassProfileProvider provider = resolveClassProfileProvider();
        if (provider == null) {
            return true;
        }
        Optional<ClassProfile> profile = provider.getProfile(player);
        if (profile.isEmpty()) {
            return false;
        }
        ClassProfile info = profile.get();
        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            if (info.classId() == null || !info.classId().equalsIgnoreCase(classId)) {
                return false;
            }
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            if (info.evolutionId() == null || !info.evolutionId().equalsIgnoreCase(evolutionId)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRequiredFocus(Player player) {
        if (!plugin.getConfig().getBoolean("casting.requireFocus", false)) {
            return true;
        }
        ItemService itemService = resolveItemService();
        if (itemService == null) {
            return false;
        }
        var allowed = plugin.getConfig().getStringList("casting.allowedFocusItemIds");
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        var inventory = player.getInventory();
        var mainHand = inventory.getItemInMainHand();
        var offHand = inventory.getItemInOffHand();
        for (String allowedId : allowed) {
            if (itemService.isItem(mainHand, allowedId) || itemService.isItem(offHand, allowedId)) {
                return true;
            }
        }
        return false;
    }

    private void tickRegen() {
        double regenPerSecond = plugin.getConfig().getDouble("mana.regenPerSecond", 0);
        if (regenPerSecond <= 0) {
            return;
        }
        double inCombatMultiplier = plugin.getConfig().getDouble("mana.regenInCombatMultiplier", 1.0);
        double combatTimeoutSeconds = plugin.getConfig().getDouble("mana.combatTimeoutSeconds", 10.0);
        long combatTimeoutMs = (long) (combatTimeoutSeconds * 1000L);
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            MageState state = state(player);
            double regen = regenPerSecond;
            if (now - state.lastCombatTime() <= combatTimeoutMs) {
                regen *= inCombatMultiplier;
            }
            if (regen != 0) {
                addMana(player, regen);
            }
        }
    }

    private MageState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), id -> {
            double max = maxManaDefault();
            return new MageState(max, max);
        });
    }

    private double maxManaDefault() {
        return plugin.getConfig().getDouble("mana.maxDefault", 100);
    }

    private long globalCastTicks() {
        return plugin.getConfig().getLong("cooldowns.globalCastTicks", 10);
    }

    public void consumeManaAndCooldowns(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        consumeRequiredItemOnCast(player, spell);
        addMana(player, -spell.mana());
        setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
        setCooldown(player, spell.id(), spell.cooldownTicks());
    }

    public String configString(String path, String fallback) {
        FileConfiguration config = plugin.getConfig();
        if (config == null) {
            return fallback;
        }
        String value = config.getString(path);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private ItemService resolveItemService() {
        RegisteredServiceProvider<ItemService> provider =
                Bukkit.getServicesManager().getRegistration(ItemService.class);
        return provider != null ? provider.getProvider() : null;
    }

    private ClassProfileProvider resolveClassProfileProvider() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        CoreApi core = provider.getProvider();
        return core.services().get(ClassProfileProvider.class);
    }

    private void sendRequirementMessage(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        String requiredItemId = requirements.requiredItemId();
        if (requiredItemId != null && !requiredItemId.isBlank()) {
            if (!hasRequiredItem(player, requiredItemId)) {
                ItemService itemService = resolveItemService();
                String itemName = resolveRequiredItemName(itemService, requiredItemId);
                player.sendMessage(messages.msg("magic.cast.missing_item", "item", itemName));
            }
        }
        ClassProfileProvider provider = resolveClassProfileProvider();
        if (provider == null) {
            return;
        }
        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            player.sendMessage(messages.msg("magic.spell.requirements.class",
                    "class", classId));
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            player.sendMessage(messages.msg("magic.spell.requirements.evolution",
                    "evolution", evolutionId));
        }
    }

    private boolean hasRequiredItem(Player player, String requiredItemId) {
        ItemService itemService = resolveItemService();
        if (itemService == null) {
            warnMissingItemBridge();
            return true;
        }
        var inventory = player.getInventory();
        for (ItemStack stack : inventory.getStorageContents()) {
            if (itemService.isItem(stack, requiredItemId)) {
                return true;
            }
        }
        ItemStack offHand = inventory.getItemInOffHand();
        return itemService.isItem(offHand, requiredItemId);
    }

    private void consumeRequiredItemOnCast(Player player, SpellDefinition spell) {
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        String requiredItemId = requirements.requiredItemId();
        if (requiredItemId == null || requiredItemId.isBlank() || !requirements.consumeOnCast()) {
            return;
        }
        ItemService itemService = resolveItemService();
        if (itemService == null) {
            warnMissingItemBridge();
            return;
        }
        removeRequiredItem(player, itemService, requiredItemId);
    }

    private boolean removeRequiredItem(Player player, ItemService itemService, String requiredItemId) {
        var inventory = player.getInventory();
        ItemStack[] contents = inventory.getStorageContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (!itemService.isItem(stack, requiredItemId)) {
                continue;
            }
            int amount = stack.getAmount();
            if (amount > 1) {
                stack.setAmount(amount - 1);
                contents[i] = stack;
            } else {
                contents[i] = null;
            }
            inventory.setStorageContents(contents);
            return true;
        }
        ItemStack offHand = inventory.getItemInOffHand();
        if (!itemService.isItem(offHand, requiredItemId)) {
            return false;
        }
        int amount = offHand.getAmount();
        if (amount > 1) {
            offHand.setAmount(amount - 1);
            inventory.setItemInOffHand(offHand);
        } else {
            inventory.setItemInOffHand(null);
        }
        return true;
    }

    private String resolveRequiredItemName(ItemService itemService, String requiredItemId) {
        if (itemService == null) {
            warnMissingItemBridge();
            return requiredItemId;
        }
        try {
            ItemStack stack = itemService.create(requiredItemId, 1);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Component displayName = meta.displayName();
                if (displayName != null && !displayName.equals(Component.empty())) {
                    return LEGACY.serialize(displayName);
                }
            }
        } catch (IllegalArgumentException ex) {
            return requiredItemId;
        }
        return requiredItemId;
    }

    private void warnMissingItemBridge() {
        if (itemBridgeWarned) {
            return;
        }
        itemBridgeWarned = true;
        plugin.getLogger().warning(messages.raw("magic.cast.items_bridge_missing"));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
