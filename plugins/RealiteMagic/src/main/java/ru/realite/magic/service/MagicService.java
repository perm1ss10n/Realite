package ru.realite.magic.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.NoopItemsBridge;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.MageState;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.requirements.DefaultSpellRequirementChecker;
import ru.realite.magic.requirements.SpellRequirementChecker;
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
    private final ItemsBridge itemsBridge;
    private final ClassesBridge classesBridge;
    private final SpellRequirementChecker requirementChecker;
    private final Map<UUID, MageState> states = new HashMap<>();
    private final SpellSelectMenu spellSelectMenu;
    private BukkitTask regenTask;
    private boolean itemBridgeWarned;

    public MagicService(JavaPlugin plugin,
                        MagicMessages messages,
                        SpellRegistry spellRegistry,
                        PlayerSpellService playerSpellService,
                        ItemsBridge itemsBridge,
                        ClassesBridge classesBridge) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.itemsBridge = Objects.requireNonNull(itemsBridge, "itemsBridge");
        this.classesBridge = Objects.requireNonNull(classesBridge, "classesBridge");
        this.requirementChecker = new DefaultSpellRequirementChecker(
                this.itemsBridge,
                this.classesBridge,
                this.messages,
                this::warnMissingItemBridge);
        this.caster = new SpellCaster(this, messages);
        this.spellSelectMenu = new SpellSelectMenu(plugin, spellRegistry, playerSpellService, requirementChecker, messages);
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

    public ItemsBridge itemsBridge() {
        return itemsBridge;
    }

    public ClassesBridge classesBridge() {
        return classesBridge;
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
        return requirementChecker.check(player, spell) instanceof CheckResult.Ok;
    }

    public CheckResult checkRequirements(Player player, SpellDefinition spell) {
        if (spell == null) {
            return new CheckResult.Ok();
        }
        return requirementChecker.check(player, spell);
    }

    public boolean hasRequiredFocus(Player player) {
        if (!plugin.getConfig().getBoolean("casting.requireFocus", false)) {
            return true;
        }
        if (itemsBridge instanceof NoopItemsBridge) {
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
            if (itemsBridge.isItem(mainHand, allowedId) || itemsBridge.isItem(offHand, allowedId)) {
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

    public void handleCastRewards(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        String giveItemId = spell.giveItemId();
        if (giveItemId == null || giveItemId.isBlank()) {
            return;
        }
        int amount = Math.max(1, spell.giveItemAmount());
        if (itemsBridge instanceof NoopItemsBridge) {
            warnMissingItemBridge();
            return;
        }
        itemsBridge.give(player, giveItemId, amount);
        if (plugin.getConfig().getBoolean("casting.giveItemMessage", true)) {
            String itemName = LEGACY.serialize(itemsBridge.displayName(giveItemId));
            player.sendMessage(messages.msg("magic.cast.give_item",
                    "item", itemName,
                    "amount", String.valueOf(amount)));
        }
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

    private void sendRequirementMessage(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        CheckResult result = requirementChecker.check(player, spell);
        if (result instanceof CheckResult.Fail fail) {
            player.sendMessage(messages.msg(fail.reasonKey(), fail.placeholders()));
        }
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
        if (itemsBridge instanceof NoopItemsBridge) {
            warnMissingItemBridge();
            return;
        }
        itemsBridge.removeItem(player, requiredItemId, 1);
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
