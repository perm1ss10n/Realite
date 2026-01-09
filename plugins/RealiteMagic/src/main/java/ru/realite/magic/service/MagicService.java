package ru.realite.magic.service;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import ru.realite.magic.cast.CastAttemptResult;
import ru.realite.magic.cast.CastDeliveryType;
import ru.realite.magic.cast.CastEngine;
import ru.realite.magic.cast.CastExecutionPlan;
import ru.realite.magic.cast.WarnLimiter;
import ru.realite.magic.admin.MagicDiagnosticsService;
import ru.realite.magic.admin.MagicDiagnosticsService.CastLogEntry;
import ru.realite.magic.admin.override.MagicOverrideService;
import ru.realite.magic.debug.DebugService;
import ru.realite.magic.balance.ItemModifiers;
import ru.realite.magic.balance.ItemModifiersService;
import ru.realite.magic.effect.BalanceModifiers;
import ru.realite.magic.effect.EffectContext;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.effect.SpellEffectExecutor;
import ru.realite.magic.api.event.SpellCastAttemptEvent;
import ru.realite.magic.api.event.SpellCastSuccessEvent;
import ru.realite.magic.hud.MagicHudService;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.economy.EconomyBridge;
import ru.realite.magic.integration.events.MagicEventPublisher;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.NoopItemsBridge;
import ru.realite.magic.integration.talents.TalentsBridge;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.MageState;
import ru.realite.magic.mastery.MasteryModifiers;
import ru.realite.magic.mastery.MasteryService;
import ru.realite.magic.school.SchoolModifiers;
import ru.realite.magic.school.SchoolService;
import ru.realite.magic.service.StaffChargeService.StaffCharges;
import ru.realite.magic.service.StaffChargeService.StaffItem;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.pve.PveService;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.requirements.DefaultSpellRequirementChecker;
import ru.realite.magic.requirements.SpellRequirementChecker;
import ru.realite.magic.region.CastPolicy;
import ru.realite.magic.region.RegionRuleService;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.ReagentCost;
import ru.realite.magic.spell.ReagentItem;
import ru.realite.magic.spell.SpellCastTrigger;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.spell.SpellRequirements;
import ru.realite.magic.spell.SpellType;
import ru.realite.magic.talent.TalentMagicService;
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
    private final EconomyBridge economyBridge;
    private final PlayerSpellService playerSpellService;
    private final MagicEventPublisher eventPublisher;
    private final SpellRequirementChecker requirementChecker;
    private final EffectExecutorRegistry effectRegistry;
    private final DebugService debugService;
    private final MagicHudService hudService;
    private final SchoolService schoolService;
    private final MasteryService masteryService;
    private final ItemModifiersService itemModifiersService;
    private final StaffChargeService staffChargeService;
    private final RegionRuleService regionRuleService;
    private final GuildBonusService guildBonusService;
    private final TalentMagicService talentMagicService;
    private final PveService pveService;
    private final MagicDiagnosticsService diagnosticsService;
    private final MagicOverrideService overrideService;
    private final Map<UUID, MageState> states = new HashMap<>();
    private final WarnLimiter warnLimiter = new WarnLimiter();
    private final SpellSelectMenu spellSelectMenu;
    private final TargetResolver targetResolver = new TargetResolver();
    private CastEngine castEngine;
    private BukkitTask regenTask;
    private boolean itemBridgeWarned;
    private final boolean failWhenItemsUnavailable;
    private boolean failWhenReagentsUnavailable;
    private boolean economyEnabled;
    private boolean failWhenEconomyUnavailable;
    private Map<MagicSchool, List<ReagentItem>> defaultReagentsBySchool;

    public MagicService(JavaPlugin plugin,
                        MagicMessages messages,
                        SpellRegistry spellRegistry,
                        PlayerSpellService playerSpellService,
                        ItemsBridge itemsBridge,
                        ClassesBridge classesBridge,
                        EconomyBridge economyBridge,
                        TalentsBridge talentsBridge,
                        MagicEventPublisher eventPublisher,
                        EffectExecutorRegistry effectRegistry,
                        DebugService debugService,
                        MagicHudService hudService,
                        MasteryService masteryService,
                        RegionRuleService regionRuleService,
                        GuildBonusService guildBonusService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.itemsBridge = Objects.requireNonNull(itemsBridge, "itemsBridge");
        this.classesBridge = Objects.requireNonNull(classesBridge, "classesBridge");
        this.economyBridge = Objects.requireNonNull(economyBridge, "economyBridge");
        this.playerSpellService = Objects.requireNonNull(playerSpellService, "playerSpellService");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.effectRegistry = Objects.requireNonNull(effectRegistry, "effectRegistry");
        this.debugService = Objects.requireNonNull(debugService, "debugService");
        this.hudService = Objects.requireNonNull(hudService, "hudService");
        this.schoolService = new SchoolService(plugin, classesBridge, messages, this::state);
        this.masteryService = Objects.requireNonNull(masteryService, "masteryService");
        this.regionRuleService = Objects.requireNonNull(regionRuleService, "regionRuleService");
        this.guildBonusService = Objects.requireNonNull(guildBonusService, "guildBonusService");
        this.talentMagicService = new TalentMagicService(talentsBridge, plugin.getLogger());
        this.pveService = new PveService(plugin);
        this.castEngine = new CastEngine(plugin);
        this.diagnosticsService = new MagicDiagnosticsService(100);
        this.overrideService = new MagicOverrideService();
        this.failWhenItemsUnavailable = plugin.getConfig()
                .getBoolean("requirements.failWhenItemsUnavailable", true);
        this.failWhenReagentsUnavailable = plugin.getConfig()
                .getBoolean("reagents.failWhenItemsUnavailable", true);
        this.economyEnabled = plugin.getConfig().getBoolean("economy.enabled", true);
        this.failWhenEconomyUnavailable = plugin.getConfig().getBoolean("economy.failWhenUnavailable", false);
        this.defaultReagentsBySchool = loadDefaultReagents();
        this.requirementChecker = new DefaultSpellRequirementChecker(
                this.itemsBridge,
                this.classesBridge,
                this.messages,
                this::warnMissingItemBridge,
                this.failWhenItemsUnavailable);
        this.spellSelectMenu = new SpellSelectMenu(plugin, spellRegistry, playerSpellService, requirementChecker, messages);
        this.itemModifiersService = new ItemModifiersService(plugin, itemsBridge);
        this.staffChargeService = new StaffChargeService(itemsBridge);
    }

    public void start() {
        if (regenTask != null) {
            return;
        }
        warnStaffConfiguration();
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRegen, 20L, 20L);
        runSmokeTestsIfEnabled();
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

    public PveService pveService() {
        return pveService;
    }

    public MagicDiagnosticsService diagnosticsService() {
        return diagnosticsService;
    }

    public MagicOverrideService overrideService() {
        return overrideService;
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
        hudService.cleanup(player);
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
        UUID playerId = player.getUniqueId();
        boolean bypassRequirements = overrideService.bypassRequirements(playerId);
        boolean bypassCooldown = overrideService.bypassCooldown(playerId);
        boolean bypassMana = overrideService.bypassMana(playerId);
        boolean bypassReagents = overrideService.bypassReagents(playerId);
        boolean bypassEconomy = overrideService.bypassEconomy(playerId);
        boolean bypassStaff = overrideService.bypassStaffCharges(playerId);
        if (!bypassRequirements) {
            CastAttemptResult castItemResult = checkCastItem(player, spell);
            if (castItemResult instanceof CastAttemptResult.Fail) {
                return castItemResult;
            }
            CheckResult requirementResult = checkRequirements(player, spell);
            if (requirementResult instanceof CheckResult.Fail fail) {
                return fail(player, spell, null, targetType(spell), fail.reasonKey(), fail.placeholders(), false, 0L);
            }
            CheckResult conflictResult = schoolService.conflictReason(player, spell);
            if (conflictResult instanceof CheckResult.Fail fail) {
                return fail(player, spell, null, targetType(spell), fail.reasonKey(), fail.placeholders(), false, 0L);
            }
        }
        CastPolicy regionPolicy = regionRuleService.castPolicy(player, spell, player.getLocation());
        if (!regionPolicy.allowed()) {
            String reasonKey = regionPolicy.denyReasonKey() == null
                    ? "magic.region.denied.default"
                    : regionPolicy.denyReasonKey();
            return fail(player, spell, null, targetType(spell), reasonKey, regionPolicy.placeholders(), false, 0L);
        }
        if (!bypassRequirements) {
            if (!hasRequiredFocus(player)) {
                return fail(player, spell, null, targetType(spell), "magic.error.need_focus", Map.of(),
                        warnLimited(player, "no_focus"), WARN_WINDOW_MS);
            }
        }
        long remainingTicks = Math.max(remainingGlobalCooldownTicks(player),
                remainingCooldownTicks(player, spell.id()));
        if (!bypassCooldown && remainingTicks > 0) {
            String time = formatCooldownSeconds(remainingTicks / 20.0);
            return fail(player, spell, null, targetType(spell), "magic.cast.cooldown", Map.of("time", time),
                    warnLimited(player, "cooldown"), WARN_WINDOW_MS);
        }
        if (!bypassRequirements) {
            CheckResult runeCheck = itemModifiersService.checkRuneSchool(player, spell);
            if (runeCheck instanceof CheckResult.Fail fail) {
                return fail(player, spell, null, targetType(spell), fail.reasonKey(), fail.placeholders(), false, 0L);
            }
        }
        ReagentCost effectiveReagents = resolveEffectiveReagents(spell);
        if (!bypassReagents && effectiveReagents != null && !effectiveReagents.isEmpty()) {
            if (itemsBridge instanceof NoopItemsBridge) {
                warnMissingItemBridge();
                if (failWhenReagentsUnavailable) {
                    return fail(player, spell, null, targetType(spell), "magic.cast.items_bridge_missing",
                            Map.of(), false, 0L);
                }
                effectiveReagents = null;
            } else {
                CastAttemptResult reagentsResult = checkReagents(player, spell, effectiveReagents);
                if (reagentsResult != null) {
                    return reagentsResult;
                }
            }
        }
        boolean shouldPay = !bypassEconomy && economyEnabled && spell.moneyCost() > 0;
        if (shouldPay) {
            if (!economyBridge.isAvailable()) {
                if (failWhenEconomyUnavailable) {
                    return fail(player, spell, null, targetType(spell), "magic.economy.unavailable", Map.of(),
                            false, 0L);
                }
                shouldPay = false;
            } else if (economyBridge.balance(player.getUniqueId()) < spell.moneyCost()) {
                return fail(player, spell, null, targetType(spell), "magic.economy.not_enough", Map.of(), false, 0L);
            }
        }
        BalanceModifiers modifiers = balanceModifiers(player, spell, null);
        double effectiveMana = effectiveManaCost(spell, modifiers);
        double currentMana = getMana(player);
        if (!bypassMana && currentMana < effectiveMana) {
            String needed = formatNumber(effectiveMana - currentMana, "casting.manaFormat", "0.0");
            return fail(player, spell, null, targetType(spell), "magic.cast.no_mana", Map.of("mana", needed),
                    warnLimited(player, "no_mana"), WARN_WINDOW_MS);
        }
        StaffCastResult staffResult = bypassStaff ? StaffCastResult.ok() : checkStaffForCast(player, spell);
        if (staffResult.reasonKey() != null) {
            return fail(player, spell, null, targetType(spell), staffResult.reasonKey(),
                    staffResult.placeholders(), warnLimited(player, "staff_fail"), WARN_WINDOW_MS);
        }
        Optional<SpellTarget> resolvedTarget = targetResolver.resolve(player, spell);
        if (isTargetRequired(spell) && resolvedTarget.isEmpty()) {
            return fail(player, spell, null, targetType(spell), "magic.cast.no_target", Map.of(),
                    warnLimited(player, "no_target"), WARN_WINDOW_MS);
        }
        SpellTarget target = resolvedTarget.orElseGet(() -> new SpellTarget.Self(player));
        if (shouldPay && !economyBridge.withdraw(player.getUniqueId(), spell.moneyCost())) {
            return fail(player, spell, null, targetType(spell), "magic.economy.not_enough", Map.of(), false, 0L);
        }
        if (!bypassReagents && effectiveReagents != null && effectiveReagents.consumeOnCast()) {
            CastAttemptResult consumeResult = consumeReagents(player, spell, effectiveReagents);
            if (consumeResult != null) {
                if (shouldPay) {
                    economyBridge.deposit(player.getUniqueId(), spell.moneyCost());
                }
                return consumeResult;
            }
        }
        if (!bypassStaff) {
            consumeStaffChargesOnCast(player, staffResult);
        }
        if (!bypassRequirements) {
            consumeRequiredItemOnCast(player, spell);
        }
        if (!bypassMana) {
            addMana(player, -effectiveMana);
        }
        long cooldownTicks = effectiveCooldownTicks(spell, modifiers);
        if (!bypassCooldown) {
            setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
            setCooldown(player, spell.id(), cooldownTicks);
        }
        cast(player, spell, target, modifiers);
        MageState state = state(player);
        state.lastSchool(spell.school());
        state.lastSchoolTime(System.currentTimeMillis());
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
        diagnosticsService.recordSuccess(new CastLogEntry(
                Instant.now(),
                player.getName(),
                spell.id(),
                true,
                null,
                targetType(target).name(),
                spell.school() == null ? null : spell.school().name(),
                bypassMana ? 0.0 : effectiveMana,
                bypassCooldown ? 0L : cooldownTicks,
                regionRuleService.regionId(player.getLocation()),
                bypassStaff ? 0 : staffResult.cost(),
                !bypassReagents && effectiveReagents != null && effectiveReagents.consumeOnCast()
                        ? effectiveReagents.items()
                        : List.of(),
                shouldPay ? spell.moneyCost() : 0.0));
        return new CastAttemptResult.Success(spell);
    }

    public void cast(Player player, SpellDefinition spell, SpellTarget target) {
        BalanceModifiers modifiers = balanceModifiers(player, spell, null);
        cast(player, spell, target, modifiers);
    }

    private void cast(Player player, SpellDefinition spell, SpellTarget target, BalanceModifiers modifiers) {
        if (spell == null || player == null || target == null) {
            return;
        }
        CastExecutionPlan plan = castEngine.execute(player, spell, target);
        if (plan == null) {
            return;
        }
        if (requiresTargets(spell) && plan.targets().isEmpty()) {
            hudService.showCastFail(player, "magic.cast.no_targets", Map.of());
            return;
        }
        EffectContext context = new EffectContext(player, spell, plan, modifiers, ThreadLocalRandom.current(), this);
        executeEffects(context, spell.effects());
        executeEffects(context, talentMagicService.extraEffects(player, spell, plan));
        handleCastRewards(player, spell);
    }

    private boolean requiresTargets(SpellDefinition spell) {
        CastDeliveryType deliveryType = spell.castDelivery();
        if (deliveryType == CastDeliveryType.AOE || deliveryType == CastDeliveryType.CHAIN) {
            return spell.target() != null && spell.target().type() == SpellTargetType.ENTITY;
        }
        return false;
    }

    public MagicMessages messages() {
        return messages;
    }

    public MasteryService masteryService() {
        return masteryService;
    }

    public void handleCastResult(Player player, CastAttemptResult result) {
        if (result instanceof CastAttemptResult.Fail fail) {
            hudService.showCastFail(player, fail.reasonKey(), fail.placeholders());
            return;
        }
        if (result instanceof CastAttemptResult.Success success) {
            hudService.showCastSuccess(player, success.spell());
        }
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

    public EconomyBridge economyBridge() {
        return economyBridge;
    }

    public StaffChargeService staffChargeService() {
        return staffChargeService;
    }

    public TalentMagicService talentMagicService() {
        return talentMagicService;
    }

    public RegionRuleService regionRuleService() {
        return regionRuleService;
    }

    public GuildBonusService guildBonusService() {
        return guildBonusService;
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
        BalanceModifiers modifiers = balanceModifiers(player, spell, null);
        consumeRequiredItemOnCast(player, spell);
        addMana(player, -effectiveManaCost(spell, modifiers));
        setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
        setCooldown(player, spell.id(), effectiveCooldownTicks(spell, modifiers));
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

    private CastAttemptResult checkReagents(Player player, SpellDefinition spell, ReagentCost reagents) {
        for (ReagentItem item : reagents.items()) {
            if (item == null) {
                continue;
            }
            if (!itemsBridge.hasItem(player, item.itemId(), item.amount())) {
                String display = item.itemId();
                if (item.itemId() != null && !item.itemId().isBlank()) {
                    display = LEGACY.serialize(itemsBridge.displayName(item.itemId()));
                }
                return fail(player, spell, null, targetType(spell), "magic.reagents.missing",
                        Map.of("item", display, "need", String.valueOf(item.amount())), false, 0L);
            }
        }
        return null;
    }

    private CastAttemptResult consumeReagents(Player player, SpellDefinition spell, ReagentCost reagents) {
        for (ReagentItem item : reagents.items()) {
            if (item == null) {
                continue;
            }
            if (!itemsBridge.hasItem(player, item.itemId(), item.amount())) {
                String display = item.itemId();
                if (item.itemId() != null && !item.itemId().isBlank()) {
                    display = LEGACY.serialize(itemsBridge.displayName(item.itemId()));
                }
                return fail(player, spell, null, targetType(spell), "magic.reagents.missing",
                        Map.of("item", display, "need", String.valueOf(item.amount())), false, 0L);
            }
        }
        for (ReagentItem item : reagents.items()) {
            if (item == null) {
                continue;
            }
            if (!itemsBridge.takeItem(player, item.itemId(), item.amount())) {
                String display = item.itemId();
                if (item.itemId() != null && !item.itemId().isBlank()) {
                    display = LEGACY.serialize(itemsBridge.displayName(item.itemId()));
                }
                return fail(player, spell, null, targetType(spell), "magic.reagents.missing",
                        Map.of("item", display, "need", String.valueOf(item.amount())), false, 0L);
            }
        }
        return null;
    }

    private StaffCastResult checkStaffForCast(Player player, SpellDefinition spell) {
        if (!isStaffEnabled()) {
            return StaffCastResult.ok();
        }
        if (itemsBridge instanceof NoopItemsBridge) {
            warnMissingItemBridge();
            if (isStaffRequired() && failWhenItemsUnavailable) {
                return StaffCastResult.fail("magic.cast.items_bridge_missing", Map.of());
            }
            return StaffCastResult.ok();
        }
        boolean allowOffhand = plugin.getConfig().getBoolean("staff.allowOffhand", true);
        Optional<StaffItem> staffItem = staffChargeService.findStaff(player, allowOffhand);
        if (staffItem.isEmpty()) {
            if (isStaffRequired()) {
                return StaffCastResult.fail("magic.error.need_focus", Map.of());
            }
            return StaffCastResult.ok();
        }
        StaffCharges charges = staffChargeService.readCharges(staffItem.get().stack());
        showStaffCharges(player, charges);
        int cost = staffChargesCost(spell);
        if (cost > 0 && charges.current() < cost) {
            return StaffCastResult.fail("magic.staff.no_charges", Map.of());
        }
        return new StaffCastResult(null, Map.of(), staffItem.get(), charges, cost);
    }

    private void consumeStaffChargesOnCast(Player player, StaffCastResult staffResult) {
        if (player == null || staffResult == null) {
            return;
        }
        if (staffResult.staffItem() == null || staffResult.cost() <= 0) {
            return;
        }
        if (!plugin.getConfig().getBoolean("staff.consumeChargesOnCast", true)) {
            return;
        }
        int newCharges = Math.max(0, staffResult.charges().current() - staffResult.cost());
        staffChargeService.writeCharges(player, staffResult.staffItem(), newCharges);
    }

    private void showStaffCharges(Player player, StaffCharges charges) {
        if (charges == null) {
            return;
        }
        int max = charges.max() > 0 ? charges.max() : charges.current();
        hudService.showGeneric(player, messages.msg("magic.staff.charges",
                "current", String.valueOf(charges.current()),
                "max", String.valueOf(max)));
    }

    private boolean isStaffEnabled() {
        return plugin.getConfig().getBoolean("staff.enabled", true);
    }

    private boolean isStaffRequired() {
        return plugin.getConfig().getBoolean("staff.requireStaff", true);
    }

    private int staffChargesCost(SpellDefinition spell) {
        if (spell != null && spell.staffChargesCost() != null) {
            return Math.max(0, spell.staffChargesCost());
        }
        int configured = plugin.getConfig().getInt("staff.chargesCostDefault", 1);
        return Math.max(0, configured);
    }

    private void warnStaffConfiguration() {
        if (isStaffEnabled()) {
            return;
        }
        boolean hasStaffCost = spellRegistry.all().stream()
                .anyMatch(spell -> spell != null && spell.staffChargesCost() != null && spell.staffChargesCost() > 0);
        if (hasStaffCost) {
            plugin.getLogger().warning("Staff charges are configured in spells, but staff.enabled=false.");
        }
    }

    private record StaffCastResult(@Nullable String reasonKey,
                                   Map<String, String> placeholders,
                                   @Nullable StaffItem staffItem,
                                   @Nullable StaffCharges charges,
                                   int cost) {
        private static StaffCastResult ok() {
            return new StaffCastResult(null, Map.of(), null, null, 0);
        }

        private static StaffCastResult fail(String reasonKey, Map<String, String> placeholders) {
            return new StaffCastResult(reasonKey, placeholders, null, null, 0);
        }
    }

    private void warnMissingItemBridge() {
        if (itemBridgeWarned) {
            return;
        }
        itemBridgeWarned = true;
        plugin.getLogger().warning(messages.raw("magic.cast.items_bridge_missing"));
    }

    private ReagentCost resolveEffectiveReagents(SpellDefinition spell) {
        if (!plugin.getConfig().getBoolean("reagents.enabled", true)) {
            return null;
        }
        List<ReagentItem> defaults = defaultReagentsBySchool.getOrDefault(spell.school(), List.of());
        List<ReagentItem> items = spell.reagents() != null && spell.reagents().items() != null
                ? spell.reagents().items()
                : List.of();
        if (defaults.isEmpty() && items.isEmpty()) {
            return null;
        }
        Map<String, Integer> merged = new HashMap<>();
        mergeReagents(merged, defaults);
        mergeReagents(merged, items);
        List<ReagentItem> mergedItems = merged.entrySet().stream()
                .map(entry -> new ReagentItem(entry.getKey(), entry.getValue()))
                .toList();
        boolean consumeOnCast = spell.reagents() == null || spell.reagents().consumeOnCast();
        return new ReagentCost(consumeOnCast, mergedItems);
    }

    public ReagentCost resolveEffectiveReagentsForInspect(SpellDefinition spell) {
        return resolveEffectiveReagents(spell);
    }

    private void mergeReagents(Map<String, Integer> target, List<ReagentItem> items) {
        for (ReagentItem item : items) {
            if (item == null || item.itemId() == null || item.itemId().isBlank()) {
                continue;
            }
            int amount = Math.max(0, item.amount());
            if (amount == 0) {
                continue;
            }
            target.merge(item.itemId(), amount, Integer::sum);
        }
    }

    private Map<MagicSchool, List<ReagentItem>> loadDefaultReagents() {
        Map<MagicSchool, List<ReagentItem>> result = new HashMap<>();
        var section = plugin.getConfig().getConfigurationSection("reagents.defaultsBySchool");
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            MagicSchool school = MagicSchool.fromString(key);
            if (school == null) {
                continue;
            }
            List<ReagentItem> items = parseReagentItems(section.getMapList(key));
            result.put(school, items);
        }
        return result;
    }

    private List<ReagentItem> parseReagentItems(List<Map<?, ?>> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            return List.of();
        }
        List<ReagentItem> items = new java.util.ArrayList<>();
        for (Object raw : rawItems) {
            if (!(raw instanceof Map<?, ?> itemMap)) {
                continue;
            }
            Object itemIdRaw = itemMap.get("itemId");
            String itemId = itemIdRaw == null ? null : String.valueOf(itemIdRaw);
            int amount = 0;
            Object amountRaw = itemMap.get("amount");
            if (amountRaw instanceof Number number) {
                amount = number.intValue();
            } else if (amountRaw != null) {
                try {
                    amount = Integer.parseInt(String.valueOf(amountRaw));
                } catch (NumberFormatException ex) {
                    amount = 0;
                }
            }
            items.add(new ReagentItem(itemId, amount));
        }
        return List.copyOf(items);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double effectiveManaCost(SpellDefinition spell, BalanceModifiers modifiers) {
        if (spell == null) {
            return 0;
        }
        double multiplier = modifiers == null ? 1.0 : modifiers.manaMultiplier();
        return Math.max(0, spell.mana() * multiplier);
    }

    private long effectiveCooldownTicks(SpellDefinition spell, BalanceModifiers modifiers) {
        if (spell == null) {
            return 0;
        }
        double multiplier = modifiers == null ? 1.0 : modifiers.cooldownMultiplier();
        return Math.max(0, Math.round(spell.cooldownTicks() * multiplier));
    }

    private BalanceModifiers balanceModifiers(Player player, SpellDefinition spell) {
        return balanceModifiers(player, spell, null);
    }

    private BalanceModifiers balanceModifiers(Player player, SpellDefinition spell, @Nullable CastExecutionPlan plan) {
        if (spell == null) {
            return BalanceModifiers.identity();
        }
        SchoolModifiers schoolModifiers = schoolService.modifiersFor(player, spell);
        MasteryModifiers masteryModifiers = masteryService.modifiers(player.getUniqueId(), spell.id());
        ItemModifiers itemModifiers = itemModifiersService.modifiers(player, spell);
        BalanceModifiers regionModifiers = regionRuleService.regionModifiers(player, spell, player.getLocation());
        BalanceModifiers guildModifiers = guildBonusService.guildModifiers(player, spell);
        BalanceModifiers talentModifiers = talentMagicService.modifiers(player, spell, plan);
        return new BalanceModifiers(
                schoolModifiers.damageMultiplier()
                        * masteryModifiers.damageMultiplier()
                        * itemModifiers.damageMultiplier()
                        * regionModifiers.damageMultiplier()
                        * guildModifiers.damageMultiplier()
                        * talentModifiers.damageMultiplier(),
                schoolModifiers.manaMultiplier()
                        * masteryModifiers.manaMultiplier()
                        * itemModifiers.manaMultiplier()
                        * regionModifiers.manaMultiplier()
                        * guildModifiers.manaMultiplier()
                        * talentModifiers.manaMultiplier(),
                schoolModifiers.cooldownMultiplier()
                        * masteryModifiers.cooldownMultiplier()
                        * itemModifiers.cooldownMultiplier()
                        * regionModifiers.cooldownMultiplier()
                        * guildModifiers.cooldownMultiplier()
                        * talentModifiers.cooldownMultiplier());
    }

    private void executeEffects(EffectContext context, Iterable<SpellEffectDefinition> effects) {
        if (effects == null) {
            return;
        }
        int index = 0;
        for (SpellEffectDefinition effect : effects) {
            if (effect == null) {
                index++;
                continue;
            }
            SpellEffectExecutor executor = effectRegistry.find(effect.type()).orElse(null);
            if (executor == null) {
                plugin.getLogger().warning("Unknown effect executor: " + effect.type());
                index++;
                continue;
            }
            try {
                executor.execute(context, effect);
            } catch (Exception ex) {
                String spellId = context.spell() == null ? "unknown" : context.spell().id();
                plugin.getLogger().log(Level.SEVERE,
                        "Failed to execute effect for spell '" + spellId + "' at index " + index, ex);
            }
            index++;
        }
    }

    private void runSmokeTestsIfEnabled() {
        if (!plugin.getConfig().getBoolean("magic.debug.smokeTests", false)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, this::runSmokeTests);
    }

    private void runSmokeTests() {
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) {
            plugin.getLogger().warning("[Magic] Smoke tests enabled but no online players found.");
            return;
        }
        if (!player.hasPermission(PERMISSION_USE)) {
            plugin.getLogger().warning("[Magic] Smoke tests skipped: player lacks permission realite.magic.use.");
            return;
        }
        UUID playerId = player.getUniqueId();
        overrideService.setBypass(playerId, MagicOverrideService.BypassType.REQUIREMENTS, true, null);
        overrideService.setBypass(playerId, MagicOverrideService.BypassType.MANA, true, null);
        overrideService.setBypass(playerId, MagicOverrideService.BypassType.ECONOMY, true, null);
        overrideService.setBypass(playerId, MagicOverrideService.BypassType.STAFF, true, null);
        try {
            SpellDefinition successSpell = SmokeTestSpells.successSpell();
            CastAttemptResult success = tryCast(player, successSpell);
            if (!(success instanceof CastAttemptResult.Success)) {
                plugin.getLogger().warning("[Magic] Smoke test failed: success cast did not succeed.");
                return;
            }
            SpellDefinition cooldownSpell = SmokeTestSpells.cooldownSpell();
            CastAttemptResult firstCooldown = tryCast(player, cooldownSpell);
            CastAttemptResult secondCooldown = tryCast(player, cooldownSpell);
            if (firstCooldown instanceof CastAttemptResult.Success
                    && secondCooldown instanceof CastAttemptResult.Fail fail
                    && !"magic.cast.cooldown".equals(fail.reasonKey())) {
                plugin.getLogger().warning("[Magic] Smoke test warning: cooldown failure returned " + fail.reasonKey());
            }
            SpellDefinition reagentsSpell = SmokeTestSpells.reagentsSpell();
            CastAttemptResult reagentsResult = tryCast(player, reagentsSpell);
            if (reagentsResult instanceof CastAttemptResult.Success) {
                plugin.getLogger().warning("[Magic] Smoke test warning: reagents failure did not trigger.");
            }
            runPveSmokeTest(player);
        } finally {
            overrideService.clearBypass(playerId, MagicOverrideService.BypassType.REQUIREMENTS);
            overrideService.clearBypass(playerId, MagicOverrideService.BypassType.MANA);
            overrideService.clearBypass(playerId, MagicOverrideService.BypassType.ECONOMY);
            overrideService.clearBypass(playerId, MagicOverrideService.BypassType.STAFF);
        }
    }

    private void runPveSmokeTest(Player player) {
        if (!pveService.enabled()) {
            plugin.getLogger().warning("[Magic] Smoke test skipped: PVE is disabled.");
            return;
        }
        var world = player.getWorld();
        var location = player.getLocation().add(1, 0, 1);
        var entity = world.spawn(location, org.bukkit.entity.Zombie.class, spawned -> spawned.addScoreboardTag("boss"));
        try {
            SpellDefinition pveSpell = SmokeTestSpells.pveResistSpell();
            CastExecutionPlan plan = new CastExecutionPlan(
                    pveSpell,
                    player,
                    List.of(entity),
                    player.getLocation(),
                    entity.getLocation(),
                    entity,
                    Map.of());
            EffectContext context = new EffectContext(player, pveSpell, plan, BalanceModifiers.identity(),
                    ThreadLocalRandom.current(), this);
            executeEffects(context, pveSpell.effects());
        } finally {
            entity.remove();
        }
    }

    private static final class SmokeTestSpells {
        private static SpellDefinition successSpell() {
            return baseSpell("smoke_success", 0, 0);
        }

        private static SpellDefinition cooldownSpell() {
            return baseSpell("smoke_cooldown", 0, 40);
        }

        private static SpellDefinition reagentsSpell() {
            ReagentCost reagents = new ReagentCost(true, List.of(new ReagentItem("missing_item", 99)));
            return baseSpell("smoke_reagents", reagents, 0, 0);
        }

        private static SpellDefinition pveResistSpell() {
            return baseSpell("smoke_pve", 0, 0,
                    List.of(new SpellEffectDefinition("knockback", Map.of("strength", 1, "mode", "PRIMARY"))));
        }

        private static SpellDefinition baseSpell(String id, long cooldownTicks, double mana) {
            return baseSpell(id, null, cooldownTicks, mana);
        }

        private static SpellDefinition baseSpell(String id, ReagentCost reagents, long cooldownTicks, double mana) {
            return baseSpell(id, reagents, cooldownTicks, mana,
                    List.of(new SpellEffectDefinition("particles", Map.of(
                            "particle", "SPELL_INSTANT",
                            "count", 1,
                            "spread", 0.1,
                            "target", "ORIGIN"))));
        }

        private static SpellDefinition baseSpell(String id,
                                                 ReagentCost reagents,
                                                 long cooldownTicks,
                                                 double mana,
                                                 List<SpellEffectDefinition> effects) {
            return new SpellDefinition(
                    id,
                    SpellType.UTILITY,
                    id,
                    id,
                    MagicSchool.NONE,
                    mana,
                    cooldownTicks,
                    0,
                    0,
                    new SpellRequirements(null, null, null, false),
                    SpellTargetDefinition.none(),
                    CastDeliveryType.INSTANT,
                    null,
                    null,
                    null,
                    null,
                    effects,
                    SpellCastTrigger.INSTANT,
                    null,
                    null,
                    reagents,
                    0,
                    null,
                    0,
                    null,
                    null,
                    null);
        }
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
        diagnosticsService.recordFail(new CastLogEntry(
                Instant.now(),
                player.getName(),
                spellId,
                false,
                reasonKey,
                targetType.name(),
                spell == null || spell.school() == null ? null : spell.school().name(),
                0.0,
                0L,
                regionRuleService.regionId(player.getLocation()),
                0,
                List.of(),
                0.0));
        return new CastAttemptResult.Fail(reasonKey, Map.copyOf(placeholders), silent, cooldownMsForSpam);
    }

    public boolean reloadConfigSection(MagicConfigSection section) {
        if (section == null) {
            return false;
        }
        plugin.reloadConfig();
        if (section == MagicConfigSection.ALL || section == MagicConfigSection.PVE) {
            pveService.reload(plugin.getConfig());
        }
        if (section == MagicConfigSection.ALL || section == MagicConfigSection.REAGENTS) {
            failWhenReagentsUnavailable = plugin.getConfig()
                    .getBoolean("reagents.failWhenItemsUnavailable", true);
            defaultReagentsBySchool = loadDefaultReagents();
        }
        if (section == MagicConfigSection.ALL || section == MagicConfigSection.ECONOMY) {
            economyEnabled = plugin.getConfig().getBoolean("economy.enabled", true);
            failWhenEconomyUnavailable = plugin.getConfig().getBoolean("economy.failWhenUnavailable", false);
        }
        if (section == MagicConfigSection.ALL || section == MagicConfigSection.MASTERY) {
            masteryService.reloadConfig();
        }
        if (section == MagicConfigSection.ALL || section == MagicConfigSection.LIMITS) {
            castEngine = new CastEngine(plugin);
        }
        if (section == MagicConfigSection.ALL || section == MagicConfigSection.HUD) {
            messages.reload();
        }
        return true;
    }

    private void publishCastAttempt(UUID playerId,
                                    String spellId,
                                    boolean success,
                                    @Nullable String reasonKey,
                                    Map<String, String> placeholders) {
        eventPublisher.publish(new SpellCastAttemptEvent(playerId, spellId, success, reasonKey, placeholders));
    }

    public String formatCooldownSeconds(double seconds) {
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
