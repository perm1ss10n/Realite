package ru.realite.magic.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import ru.realite.magic.cast.CastAttemptResult;
import ru.realite.magic.cast.WarnLimiter;
import ru.realite.magic.debug.DebugService;
import ru.realite.magic.effect.EffectContext;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.effect.SpellEffectExecutor;
import ru.realite.magic.api.event.SpellCastAttemptEvent;
import ru.realite.magic.api.event.SpellCastSuccessEvent;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.events.MagicEventPublisher;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.NoopItemsBridge;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.MageState;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.requirements.DefaultSpellRequirementChecker;
import ru.realite.magic.requirements.SpellRequirementChecker;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.spell.SpellRequirements;
import ru.realite.magic.target.SpellTarget;
import ru.realite.magic.target.SpellTargetDefinition;
import ru.realite.magic.target.SpellTargetType;
import ru.realite.magic.target.TargetResolver;

public final class MagicService {

    private static final String GLOBAL_COOLDOWN_KEY = "global";
    private static final String PERMISSION_USE = "realite.magic.use";
    private static final long WARN_WINDOW_MS = 2000L;
    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final MagicMessages messages;
    private final SpellRegistry spellRegistry;
    private final ItemsBridge itemsBridge;
    private final ClassesBridge classesBridge;
    private final PlayerSpellService playerSpellService;
    private final MagicEventPublisher eventPublisher;
    private final SpellRequirementChecker requirementChecker;
    private final EffectExecutorRegistry effectRegistry;
    private final DebugService debugService;
    private final Map<UUID, MageState> states = new HashMap<>();
    private final WarnLimiter warnLimiter = new WarnLimiter();
    private final SpellSelectMenu spellSelectMenu;
    private final TargetResolver targetResolver = new TargetResolver();
    private BukkitTask regenTask;
    private boolean itemBridgeWarned;

    public MagicService(JavaPlugin plugin,
                        MagicMessages messages,
                        SpellRegistry spellRegistry,
                        PlayerSpellService playerSpellService,
                        ItemsBridge itemsBridge,
                        ClassesBridge classesBridge,
                        MagicEventPublisher eventPublisher,
                        EffectExecutorRegistry effectRegistry,
                        DebugService debugService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.itemsBridge = Objects.requireNonNull(itemsBridge, "itemsBridge");
        this.classesBridge = Objects.requireNonNull(classesBridge, "classesBridge");
        this.playerSpellService = Objects.requireNonNull(playerSpellService, "playerSpellService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.effectRegistry = Objects.requireNonNull(effectRegistry, "effectRegistry");
        this.debugService = Objects.requireNonNull(debugService, "debugService");
        boolean failWhenItemsUnavailable = plugin.getConfig()
                .getBoolean("requirements.failWhenItemsUnavailable", true);
        this.requirementChecker = new DefaultSpellRequirementChecker(
                this.itemsBridge,
                this.classesBridge,
                this.messages,
                this::warnMissingItemBridge,
                failWhenItemsUnavailable);
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
        warnLimiter.clear(player.getUniqueId());
    }

    public CastAttemptResult tryCastSelected(Player player) {
        UUID playerId = player.getUniqueId();
        int activeSlot = playerSpellService.getActiveSlot(playerId);
        String selectedSpellId = playerSpellService.getActiveSlotSpell(playerId).orElse(null);
        if (selectedSpellId == null) {
            return fail(player, null, null, SpellTargetType.NONE, "magic.cast.slot_empty", Map.of(),
                    warnLimited(player, "slot_empty"), WARN_WINDOW_MS);
        }
        if (!playerSpellService.hasSpell(playerId, selectedSpellId)) {
            playerSpellService.setSlot(playerId, activeSlot, null);
            return fail(player, null, selectedSpellId, SpellTargetType.NONE,
                    "magic.spell.select.not_learned", Map.of("spell", selectedSpellId), false, 0L);
        }
        SpellDefinition spell = spellRegistry.find(selectedSpellId).orElse(null);
        if (spell == null) {
            playerSpellService.setSlot(playerId, activeSlot, null);
            return fail(player, null, selectedSpellId, SpellTargetType.NONE,
                    "magic.spell.unknown", Map.of("spell", selectedSpellId), false, 0L);
        }
        return tryCast(player, spell);
    }

    public CastAttemptResult tryCast(Player player, SpellDefinition spell) {
        if (spell == null) {
            return fail(player, null, null, SpellTargetType.NONE, "magic.spell.unknown",
                    Map.of("spell", "null"), false, 0L);
        }
        if (!player.hasPermission(PERMISSION_USE)) {
            return fail(player, spell, null, targetType(spell), "magic.command.errors.no_permission",
                    Map.of(), false, 0L);
        }
        CastAttemptResult castItemResult = checkCastItem(player, spell);
        if (castItemResult instanceof CastAttemptResult.Fail) {
            return castItemResult;
        }
        CheckResult requirementResult = checkRequirements(player, spell);
        if (requirementResult instanceof CheckResult.Fail fail) {
            return fail(player, spell, null, targetType(spell), fail.reasonKey(), fail.placeholders(), false, 0L);
        }
        if (!hasRequiredFocus(player)) {
            return fail(player, spell, null, targetType(spell), "magic.error.need_focus", Map.of(),
                    warnLimited(player, "no_focus"), WARN_WINDOW_MS);
        }
        long remainingTicks = Math.max(remainingGlobalCooldownTicks(player),
                remainingCooldownTicks(player, spell.id()));
        if (remainingTicks > 0) {
            String time = formatCooldownSeconds(remainingTicks / 20.0);
            return fail(player, spell, null, targetType(spell), "magic.cast.cooldown", Map.of("time", time),
                    warnLimited(player, "cooldown"), WARN_WINDOW_MS);
        }
        double currentMana = getMana(player);
        if (currentMana < spell.mana()) {
            String needed = formatNumber(spell.mana() - currentMana, "casting.manaFormat", "0.0");
            return fail(player, spell, null, targetType(spell), "magic.cast.no_mana", Map.of("mana", needed),
                    warnLimited(player, "no_mana"), WARN_WINDOW_MS);
        }
        Optional<SpellTarget> resolvedTarget = targetResolver.resolve(player, spell);
        if (isTargetRequired(spell) && resolvedTarget.isEmpty()) {
            return fail(player, spell, null, targetType(spell), "magic.cast.no_target", Map.of(),
                    warnLimited(player, "no_target"), WARN_WINDOW_MS);
        }
        SpellTarget target = resolvedTarget.orElseGet(() -> new SpellTarget.Self(player));
        consumeRequiredItemOnCast(player, spell);
        addMana(player, -spell.mana());
        setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
        setCooldown(player, spell.id(), spell.cooldownTicks());
        cast(player, spell, target);
        debugService.recordSuccess(spell.id());
        debugService.logCast(player,
                spell.id(),
                targetType(target),
                getMana(player),
                remainingGlobalCooldownTicks(player),
                remainingCooldownTicks(player, spell.id()),
                null);
        publishCastAttempt(player.getUniqueId(), spell.id(), true, null, Map.of());
        eventPublisher.publish(new SpellCastSuccessEvent(player.getUniqueId(), spell.id()));
        return new CastAttemptResult.Success(spell);
    }

    public void cast(Player player, SpellDefinition spell, SpellTarget target) {
        if (spell == null || player == null || target == null) {
            return;
        }
        EffectContext context = new EffectContext(player, spell, target, ThreadLocalRandom.current(), this);
        for (SpellEffectDefinition effect : spell.effects()) {
            SpellEffectExecutor executor = effectRegistry.find(effect.type()).orElse(null);
            if (executor == null) {
                plugin.getLogger().warning("Unknown effect executor: " + effect.type());
                continue;
            }
            executor.execute(context, effect);
        }
        handleCastRewards(player, spell);
    }

    public MagicMessages messages() {
        return messages;
    }

    public SpellSelectMenu spellSelectMenu() {
        return spellSelectMenu;
    }

    public DebugService debugService() {
        return debugService;
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

    private CastAttemptResult checkCastItem(Player player, SpellDefinition spell) {
        String castItemId = spell.castItemId();
        if (castItemId == null || castItemId.isBlank()) {
            return new CastAttemptResult.Success(spell);
        }
        if (itemsBridge instanceof NoopItemsBridge) {
            return new CastAttemptResult.Success(spell);
        }
        var inHand = player.getInventory().getItemInMainHand();
        if (itemsBridge.isItem(inHand, castItemId)) {
            return new CastAttemptResult.Success(spell);
        }
        String itemName = LEGACY.serialize(itemsBridge.displayName(castItemId));
        return fail(player,
                spell,
                null,
                targetType(spell),
                "magic.cast.wrong_item",
                Map.of("item", itemName),
                warnLimited(player, "wrong_item"),
                WARN_WINDOW_MS);
    }

    private boolean warnLimited(Player player, String key) {
        return !warnLimiter.canWarn(player.getUniqueId(), key, WARN_WINDOW_MS);
    }

    private CastAttemptResult.Fail fail(Player player,
                                        @Nullable SpellDefinition spell,
                                        @Nullable String spellIdOverride,
                                        SpellTargetType targetType,
                                        String reasonKey,
                                        Map<String, String> placeholders,
                                        boolean silent,
                                        long cooldownMsForSpam) {
        String spellId = spell != null ? spell.id() : spellIdOverride;
        if (spellId != null) {
            publishCastAttempt(player.getUniqueId(), spellId, false, reasonKey, placeholders);
        }
        debugService.recordFailure(reasonKey);
        debugService.logCast(player,
                spellId == null ? "unknown" : spellId,
                targetType,
                getMana(player),
                remainingGlobalCooldownTicks(player),
                spellId == null ? 0 : remainingCooldownTicks(player, spellId),
                reasonKey);
        return new CastAttemptResult.Fail(reasonKey, Map.copyOf(placeholders), silent, cooldownMsForSpam);
    }

    private void publishCastAttempt(UUID playerId,
                                    String spellId,
                                    boolean success,
                                    @Nullable String reasonKey,
                                    Map<String, String> placeholders) {
        eventPublisher.publish(new SpellCastAttemptEvent(playerId, spellId, success, reasonKey, placeholders));
    }

    private String formatCooldownSeconds(double seconds) {
        double rounded = Math.ceil(seconds * 10.0) / 10.0;
        return formatNumber(rounded, "casting.cooldownFormat", "0.0");
    }

    private String formatNumber(double value, String configKey, String fallbackPattern) {
        String pattern = configString(configKey, fallbackPattern);
        DecimalFormat format;
        try {
            format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException ex) {
            format = new DecimalFormat(fallbackPattern, DecimalFormatSymbols.getInstance(Locale.US));
        }
        return format.format(value);
    }

    private boolean isTargetRequired(SpellDefinition spell) {
        SpellTargetDefinition target = spell.target();
        if (target == null) {
            return false;
        }
        return target.type() != SpellTargetType.NONE;
    }

    private SpellTargetType targetType(SpellDefinition spell) {
        if (spell == null || spell.target() == null) {
            return SpellTargetType.NONE;
        }
        return spell.target().type();
    }

    private SpellTargetType targetType(SpellTarget target) {
        if (target instanceof SpellTarget.Self) {
            return SpellTargetType.SELF;
        }
        if (target instanceof SpellTarget.EntityTarget) {
            return SpellTargetType.ENTITY;
        }
        if (target instanceof SpellTarget.BlockTarget) {
            return SpellTargetType.BLOCK;
        }
        if (target instanceof SpellTarget.LocationTarget) {
            return SpellTargetType.LOCATION;
        }
        return SpellTargetType.NONE;
    }

}
