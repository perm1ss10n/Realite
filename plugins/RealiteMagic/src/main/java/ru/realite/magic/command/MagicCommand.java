package ru.realite.magic.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.debug.DebugService;
import ru.realite.magic.hud.MagicHudService;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.admin.MagicDiagnosticsService;
import ru.realite.magic.admin.MagicDiagnosticsService.CastLogEntry;
import ru.realite.magic.admin.MagicDiagnosticsService.CounterEntry;
import ru.realite.magic.admin.override.MagicOverrideService;
import ru.realite.magic.admin.override.MagicOverrideService.BypassType;
import ru.realite.magic.mastery.MasteryModifiers;
import ru.realite.magic.mastery.MasteryProgress;
import ru.realite.magic.mastery.MasteryService;
import ru.realite.magic.region.CastPolicy;
import ru.realite.magic.service.MagicConfigSection;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.RevokeResult;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellActionReason;
import ru.realite.magic.service.SpellUnlockSource;
import ru.realite.magic.service.UnlockResult;
import ru.realite.magic.spell.ReagentItem;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellLoadError;
import ru.realite.magic.spell.SpellLoadReport;

public final class MagicCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_ADMIN = "realite.magic.admin";
    private static final String PERMISSION_MENU = "realite.magic.menu";
    private static final String PERMISSION_USE = "realite.magic.use";

    private static final DateTimeFormatter REPORT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmm");

    private final JavaPlugin plugin;
    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final DebugService debugService;
    private final MagicHudService hudService;
    private final MagicDiagnosticsService diagnosticsService;
    private final MagicOverrideService overrideService;

    public MagicCommand(JavaPlugin plugin,
                        MagicService magicService,
                        PlayerSpellService playerSpellService,
                        MagicMessages messages,
                        DebugService debugService,
                        MagicHudService hudService) {
        this.plugin = plugin;
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
        this.debugService = debugService;
        this.hudService = hudService;
        this.diagnosticsService = magicService.diagnosticsService();
        this.overrideService = magicService.overrideService();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return openMenu(sender);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("mana".equals(sub)) {
            return showMana(sender);
        }
        if ("status".equals(sub)) {
            return showStatus(sender);
        }
        if ("menu".equals(sub)) {
            return openMenu(sender);
        }
        if ("spell".equals(sub)) {
            return handleSpell(sender, args);
        }
        if ("spells".equals(sub)) {
            return handleSpells(sender, args);
        }
        if ("slot".equals(sub)) {
            return handleSlot(sender, args);
        }
        if ("mastery".equals(sub)) {
            return showMastery(sender, args);
        }
        if ("debug".equals(sub)) {
            return handleDebug(sender, args);
        }
        if ("admin".equals(sub)) {
            return handleAdmin(sender, args);
        }
        if ("reload".equals(sub)) {
            return handleReload(sender, args);
        }
        sender.sendMessage(messages.msg("magic.cmd.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("menu", "spell", "spells", "mana", "status", "debug", "slot", "mastery", "admin", "reload");
        }
        if (args.length >= 2
                && !args[0].equalsIgnoreCase("spell")
                && !args[0].equalsIgnoreCase("spells")
                && !args[0].equalsIgnoreCase("debug")
                && !args[0].equalsIgnoreCase("slot")
                && !args[0].equalsIgnoreCase("mastery")
                && !args[0].equalsIgnoreCase("admin")
                && !args[0].equalsIgnoreCase("reload")) {
            return Collections.emptyList();
        }
        if (args[0].equalsIgnoreCase("slot")) {
            return tabCompleteSlots(args.length);
        }
        if (args[0].equalsIgnoreCase("mastery")) {
            return tabCompleteMastery(args.length);
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return Collections.emptyList();
            }
            if (args.length == 2) {
                return List.of("all", "hud", "balance", "schools", "mastery", "pve", "regions", "reagents",
                        "economy", "limits");
            }
            return Collections.emptyList();
        }
        if (args[0].equalsIgnoreCase("admin")) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return Collections.emptyList();
            }
            if (args.length == 2) {
                return List.of("stats", "log", "inspect", "bypass", "export");
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("inspect")) {
                return tabCompletePlayers();
            }
            if (args.length == 3 && args[1].equalsIgnoreCase("bypass")) {
                List<String> options = new ArrayList<>();
                options.add("list");
                options.addAll(tabCompletePlayers());
                return options;
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("bypass")) {
                return List.of("on", "off");
            }
            if (args.length == 5 && args[1].equalsIgnoreCase("bypass")) {
                return List.of("all", "requirements", "cooldown", "mana", "reagents", "economy", "staff");
            }
            return Collections.emptyList();
        }
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return List.of("on", "off", "cast", "stats");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spells")) {
                return List.of("reload", "validate");
            }
            return List.of("give", "remove", "list", "select", "clear");
        }
        if (args[0].equalsIgnoreCase("spells")) {
            return Collections.emptyList();
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (args[0].equalsIgnoreCase("debug")) {
            if ("cast".equals(action) && args.length == 3) {
                return tabCompletePlayers();
            }
            return Collections.emptyList();
        }
        if (args.length == 3) {
            return tabCompletePlayers();
        }
        if (args.length == 4 && ("give".equals(action) || "remove".equals(action) || "select".equals(action))) {
            return spellIds();
        }
        return Collections.emptyList();
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        if (!sender.hasPermission(PERMISSION_MENU)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        magicService.spellSelectMenu().open(player);
        return true;
    }

    private boolean showMana(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        double mana = magicService.getMana(player);
        double max = magicService.getMaxMana(player);
        hudService.showMana(player, (int) Math.round(mana), (int) Math.round(max));
        return true;
    }

    private boolean showStatus(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        sender.sendMessage(messages.msg("magic.cmd.status.header"));
        int slot = playerSpellService.getActiveSlot(player.getUniqueId());
        sender.sendMessage(messages.msg("magic.cmd.status.slot",
                "slot", String.valueOf(slot)));
        String spellId = playerSpellService.getActiveSlotSpell(player.getUniqueId()).orElse(null);
        if (spellId == null) {
            sender.sendMessage(messages.msg("magic.cmd.status.spell_empty"));
        } else {
            sender.sendMessage(messages.msg("magic.cmd.status.spell",
                    "spell", displaySpellName(spellId)));
        }
        double mana = magicService.getMana(player);
        double max = magicService.getMaxMana(player);
        sender.sendMessage(messages.msg("magic.cmd.status.mana",
                "current", format(mana),
                "max", format(max)));
        if (spellId != null) {
            long remainingTicks = magicService.remainingCooldownTicks(player, spellId);
            if (remainingTicks > 0) {
                String time = magicService.formatCooldownSeconds(remainingTicks / 20.0);
                sender.sendMessage(messages.msg("magic.cmd.status.cooldown", "time", time));
            }
        }
        return true;
    }

    private boolean showMastery(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.cmd.mastery.usage"));
            return true;
        }
        String target = args[1];
        String spellId;
        if ("selected".equalsIgnoreCase(target)) {
            spellId = playerSpellService.getSelected(player.getUniqueId()).orElse(null);
        } else {
            spellId = normalizeSpellId(target);
        }
        if (spellId == null || spellId.isBlank()) {
            sender.sendMessage(messages.msg("magic.no_selected_spell"));
            return true;
        }
        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null) {
            sender.sendMessage(messages.msg("magic.command.spell.unknown_spell", "spellId", spellId));
            return true;
        }
        MasteryService masteryService = magicService.masteryService();
        MasteryProgress progress = masteryService.getProgress(player.getUniqueId(), spellId);
        int xpToNext = masteryService.xpToNext(player.getUniqueId(), spellId);
        MasteryModifiers modifiers = masteryService.modifiers(player.getUniqueId(), spellId);
        sender.sendMessage(messages.msg("magic.cmd.mastery.header",
                "spell", displaySpellName(spellId)));
        sender.sendMessage(messages.msg("magic.cmd.mastery.line_level",
                "level", String.valueOf(progress.level())));
        sender.sendMessage(messages.msg("magic.cmd.mastery.line_xp",
                "xp", String.valueOf(progress.xp()),
                "next", String.valueOf(xpToNext)));
        sender.sendMessage(messages.msg("magic.cmd.mastery.line_bonus",
                "damage", formatBonus(modifiers.damageMultiplier()),
                "mana", formatBonus(modifiers.manaMultiplier()),
                "cooldown", formatBonus(modifiers.cooldownMultiplier())));
        return true;
    }

    private boolean handleSpell(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "give" -> handleSpellGive(sender, args);
            case "remove" -> handleSpellRemove(sender, args);
            case "list" -> handleSpellList(sender, args);
            case "select" -> handleSpellSelect(sender, args);
            case "clear" -> handleSpellClear(sender, args);
            default -> {
                sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
                yield true;
            }
        };
    }

    private boolean handleSpells(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.cmd.spells.usage"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "reload" -> handleSpellsReload(sender);
            case "validate" -> handleSpellsValidate(sender);
            default -> {
                sender.sendMessage(messages.msg("magic.cmd.spells.usage"));
                yield true;
            }
        };
    }

    private boolean handleDebug(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.cmd.debug.usage"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "on" -> handleDebugOn(sender);
            case "off" -> handleDebugOff(sender);
            case "cast" -> handleDebugCast(sender, args);
            case "stats" -> handleDebugStats(sender);
            default -> {
                sender.sendMessage(messages.msg("magic.cmd.debug.usage"));
                yield true;
            }
        };
    }

    private boolean handleSlot(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        if (!player.hasPermission(PERMISSION_USE)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.cmd.slot.usage"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "set" -> handleSlotSet(player, args);
            case "clear" -> handleSlotClear(player, args);
            case "use" -> handleSlotUse(player, args);
            default -> {
                sender.sendMessage(messages.msg("magic.cmd.slot.usage"));
                yield true;
            }
        };
    }

    private boolean handleSlotSet(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(messages.msg("magic.cmd.slot.usage"));
            return true;
        }
        Integer slot = parseSlot(args[2]);
        if (slot == null) {
            player.sendMessage(messages.msg("magic.slot.invalid", "slot", args[2]));
            return true;
        }
        String spellId = args[3];
        var result = playerSpellService.setSlot(player.getUniqueId(), slot, spellId);
        if (result instanceof ru.realite.magic.service.SetSlotResult.Fail fail) {
            player.sendMessage(messages.msg(fail.reasonKey()));
            return true;
        }
        player.sendMessage(messages.msg("magic.slot.set.ok"));
        return true;
    }

    private boolean handleSlotClear(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(messages.msg("magic.cmd.slot.usage"));
            return true;
        }
        Integer slot = parseSlot(args[2]);
        if (slot == null) {
            player.sendMessage(messages.msg("magic.slot.invalid", "slot", args[2]));
            return true;
        }
        var result = playerSpellService.setSlot(player.getUniqueId(), slot, null);
        if (result instanceof ru.realite.magic.service.SetSlotResult.Fail fail) {
            player.sendMessage(messages.msg(fail.reasonKey()));
            return true;
        }
        player.sendMessage(messages.msg("magic.slot.clear.ok"));
        return true;
    }

    private boolean handleSlotUse(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(messages.msg("magic.cmd.slot.usage"));
            return true;
        }
        Integer slot = parseSlot(args[2]);
        if (slot == null) {
            player.sendMessage(messages.msg("magic.slot.invalid", "slot", args[2]));
            return true;
        }
        var result = playerSpellService.setActiveSlot(player.getUniqueId(), slot);
        if (result instanceof ru.realite.magic.service.SetActiveSlotResult.Fail fail) {
            player.sendMessage(messages.msg(fail.reasonKey()));
            return true;
        }
        String spellId = playerSpellService.getActiveSlotSpell(player.getUniqueId()).orElse(null);
        hudService.showSelected(player, slot, spellId);
        return true;
    }

    private String displaySpellName(String spellId) {
        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null) {
            return spellId;
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        String raw = messages.raw(nameKey);
        return raw == null || raw.isBlank() ? spell.id() : raw;
    }

    private List<String> tabCompleteSlots(int argsLength) {
        if (argsLength == 2) {
            return List.of("set", "clear", "use");
        }
        if (argsLength == 3) {
            return List.of("1", "2", "3", "4", "5", "6", "7", "8", "9");
        }
        if (argsLength == 4) {
            return spellIds();
        }
        return Collections.emptyList();
    }

    private List<String> tabCompleteMastery(int argsLength) {
        if (argsLength == 2) {
            List<String> options = new ArrayList<>();
            options.add("selected");
            options.addAll(spellIds());
            return options;
        }
        return Collections.emptyList();
    }

    private Integer parseSlot(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value < 1 || value > 9) {
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean handleDebugOn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        debugService.enableGlobal(player);
        sender.sendMessage(messages.msg("magic.cmd.debug.on"));
        return true;
    }

    private boolean handleDebugOff(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        debugService.disableGlobal(player);
        sender.sendMessage(messages.msg("magic.cmd.debug.off"));
        return true;
    }

    private boolean handleDebugCast(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.msg("magic.cmd.debug.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found",
                    "player", args[2]));
            return true;
        }
        boolean enabled = debugService.togglePlayer(player, target.getUniqueId());
        sender.sendMessage(messages.msg(enabled ? "magic.cmd.debug.player_on" : "magic.cmd.debug.player_off",
                "player", target.getName()));
        return true;
    }

    private boolean handleDebugStats(CommandSender sender) {
        debugService.sendStats(sender);
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.cmd.admin.usage"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "stats" -> handleAdminStats(sender, args);
            case "log" -> handleAdminLog(sender, args);
            case "inspect" -> handleAdminInspect(sender, args);
            case "bypass" -> handleAdminBypass(sender, args);
            case "export" -> handleAdminExport(sender, args);
            default -> {
                sender.sendMessage(messages.msg("magic.cmd.admin.usage"));
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        String sectionRaw = args.length < 2 ? "all" : args[1];
        MagicConfigSection section = MagicConfigSection.fromString(sectionRaw);
        if (section == null) {
            sender.sendMessage(messages.msg("magic.cmd.reload.section_unknown", "section", sectionRaw));
            return true;
        }
        magicService.reloadConfigSection(section);
        sender.sendMessage(messages.msg("magic.cmd.reload.ok", "section", section.name().toLowerCase(Locale.ROOT)));
        return true;
    }

    private boolean handleAdminStats(CommandSender sender, String[] args) {
        int top = parseOptionalInt(args, 2, 10);
        sender.sendMessage(messages.msg("magic.cmd.admin.stats.header", "count", String.valueOf(top)));
        sendCounterTable(sender, messages.msg("magic.cmd.admin.stats.casts"), diagnosticsService.topCasts(top));
        sendCounterTable(sender, messages.msg("magic.cmd.admin.stats.fails"), diagnosticsService.topFails(top));
        List<CounterEntry> immunes = diagnosticsService.topPveImmunes(top);
        if (!immunes.isEmpty()) {
            sendCounterTable(sender, messages.msg("magic.cmd.admin.stats.pve_immune"), immunes);
        }
        List<CounterEntry> resists = diagnosticsService.topPveResistHits(top);
        if (!resists.isEmpty()) {
            sendCounterTable(sender, messages.msg("magic.cmd.admin.stats.pve_resist"), resists);
        }
        return true;
    }

    private boolean handleAdminLog(CommandSender sender, String[] args) {
        int limit = 20;
        String playerFilter = null;
        String spellFilter = null;
        boolean failOnly = false;
        for (int i = 2; i < args.length; i++) {
            String raw = args[i];
            if (raw == null || raw.isBlank()) {
                continue;
            }
            if (raw.matches("\\d+")) {
                limit = Integer.parseInt(raw);
                continue;
            }
            if (raw.startsWith("player:")) {
                playerFilter = raw.substring("player:".length());
            } else if (raw.startsWith("spell:")) {
                spellFilter = raw.substring("spell:".length());
            } else if (raw.startsWith("failOnly:")) {
                failOnly = Boolean.parseBoolean(raw.substring("failOnly:".length()));
            }
        }
        MagicDiagnosticsService.LogFilter filter =
                new MagicDiagnosticsService.LogFilter(playerFilter, spellFilter, failOnly);
        List<CastLogEntry> entries = diagnosticsService.recentLogs(filter, limit);
        sender.sendMessage(messages.msg("magic.cmd.admin.log.header", "count", String.valueOf(entries.size())));
        for (CastLogEntry entry : entries) {
            sender.sendMessage(formatLogEntry(entry));
        }
        return true;
    }

    private boolean handleAdminInspect(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found", "player", args[2]));
            return true;
        }
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.header", "player", target.getName()));
        int slot = playerSpellService.getActiveSlot(target.getUniqueId());
        String spellId = playerSpellService.getActiveSlotSpell(target.getUniqueId()).orElse(null);
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.slot",
                "slot", String.valueOf(slot),
                "spell", spellId == null ? "-" : displaySpellName(spellId)));
        sendSchoolSnapshot(sender, spellId);
        double mana = magicService.getMana(target);
        double max = magicService.getMaxMana(target);
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.mana",
                "current", format(mana),
                "max", format(max)));
        if (spellId != null) {
            long remainingTicks = magicService.remainingCooldownTicks(target, spellId);
            if (remainingTicks > 0) {
                sender.sendMessage(messages.msg("magic.cmd.admin.inspect.cooldown",
                        "time", magicService.formatCooldownSeconds(remainingTicks / 20.0)));
            }
        }
        sendStaffSnapshot(sender, target);
        sendReagentSnapshot(sender, target, spellId);
        sendMasterySnapshot(sender, target, spellId);
        sendTalentSnapshot(sender, target);
        sendRegionSnapshot(sender, target, spellId);
        sendGuildSnapshot(sender, target, spellId);
        return true;
    }

    private boolean handleAdminBypass(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(messages.msg("magic.admin.bypass.usage"));
            return true;
        }
        if ("list".equalsIgnoreCase(args[2])) {
            Map<java.util.UUID, Map<BypassType, Instant>> entries = overrideService.listAll();
            sender.sendMessage(messages.msg("magic.admin.bypass.list.header",
                    "count", String.valueOf(entries.size())));
            if (entries.isEmpty()) {
                return true;
            }
            for (Map.Entry<java.util.UUID, Map<BypassType, Instant>> entry : entries.entrySet()) {
                String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
                String displayName = name == null ? entry.getKey().toString() : name;
                sender.sendMessage(Component.text("- " + displayName + ": " + formatBypass(entry.getValue())));
            }
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage(messages.msg("magic.admin.bypass.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found", "player", args[2]));
            return true;
        }
        boolean enabled = "on".equalsIgnoreCase(args[3]);
        if (!enabled && !"off".equalsIgnoreCase(args[3])) {
            sender.sendMessage(messages.msg("magic.admin.bypass.usage"));
            return true;
        }
        String what = args.length >= 5 ? args[4] : "all";
        int ttlMinutes = parseOptionalInt(args, 5, 10);
        java.time.Duration ttl = java.time.Duration.ofMinutes(Math.max(1, ttlMinutes));
        if ("all".equalsIgnoreCase(what)) {
            overrideService.setBypassAll(target.getUniqueId(), enabled, ttl);
        } else {
            BypassType type = parseBypassType(what);
            if (type == null) {
                sender.sendMessage(messages.msg("magic.admin.bypass.usage"));
                return true;
            }
            overrideService.setBypass(target.getUniqueId(), type, enabled, ttl);
        }
        sender.sendMessage(messages.msg(enabled ? "magic.admin.bypass.enabled" : "magic.admin.bypass.disabled",
                "player", target.getName(),
                "what", what.toLowerCase(Locale.ROOT),
                "ttl", String.valueOf(ttlMinutes)));
        return true;
    }

    private boolean handleAdminExport(CommandSender sender, String[] args) {
        Path reportDir = plugin.getDataFolder().toPath().resolve("reports");
        String fileName = args.length >= 3 ? args[2] : null;
        if (fileName == null || fileName.isBlank()) {
            String timestamp = LocalDateTime.now().format(REPORT_TIME_FORMAT);
            fileName = "magic-report-" + timestamp + ".json";
        } else if (!fileName.endsWith(".json") && !fileName.endsWith(".txt")) {
            fileName = fileName + ".json";
        }
        Path target = reportDir.resolve(fileName);
        try {
            Files.createDirectories(reportDir);
            Files.writeString(target, buildReportJson(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            sender.sendMessage(messages.msg("magic.cmd.admin.export.fail"));
            return true;
        }
        sender.sendMessage(messages.msg("magic.cmd.admin.export.ok", "file", target.toString()));
        return true;
    }

    private boolean handleSpellsReload(CommandSender sender) {
        SpellLoadReport report = magicService.spellRegistry().reload();
        sender.sendMessage(messages.msg("magic.cmd.spells.reload.ok",
                "count", String.valueOf(report.loadedCount()),
                "errors", String.valueOf(report.errors().size())));
        sendSpellErrors(sender, report);
        logSpellReport(report);
        return true;
    }

    private boolean handleSpellsValidate(CommandSender sender) {
        SpellLoadReport report = magicService.spellRegistry().validate();
        sender.sendMessage(messages.msg("magic.cmd.spells.validate.ok",
                "count", String.valueOf(report.loadedCount()),
                "errors", String.valueOf(report.errors().size())));
        sendSpellErrors(sender, report);
        logSpellReport(report);
        return true;
    }

    private void sendSpellErrors(CommandSender sender, SpellLoadReport report) {
        if (!report.hasErrors()) {
            sender.sendMessage(messages.msg("magic.cmd.spells.no_errors"));
            return;
        }
        sender.sendMessage(messages.msg("magic.cmd.spells.errors.header"));
        for (SpellLoadError error : report.errors()) {
            if ("magic.cmd.spells.errors.schema".equals(error.messageKey())) {
                sender.sendMessage(messages.msg(error.messageKey(), error.placeholders()));
                continue;
            }
            if (error.placeholders().containsKey("path")) {
                sender.sendMessage(messages.msg("magic.cmd.spells.errors.schema",
                        Map.of("file", error.fileName(),
                                "path", error.placeholders().get("path"),
                                "error", resolveError(error))));
                continue;
            }
            sender.sendMessage(messages.msg("magic.cmd.spells.errors.entry",
                    "file", error.fileName(),
                    "error", resolveError(error)));
        }
    }

    private void logSpellReport(SpellLoadReport report) {
        if (!report.hasErrors()) {
            Bukkit.getLogger().info(messages.raw("magic.cmd.spells.no_errors"));
            return;
        }
        Bukkit.getLogger().info(messages.raw("magic.cmd.spells.errors.header"));
        for (SpellLoadError error : report.errors()) {
            if ("magic.cmd.spells.errors.schema".equals(error.messageKey())) {
                Bukkit.getLogger().warning(messages.raw(error.messageKey(), error.placeholders()));
                continue;
            }
            if (error.placeholders().containsKey("path")) {
                Bukkit.getLogger().warning(messages.raw("magic.cmd.spells.errors.schema",
                        Map.of("file", error.fileName(),
                                "path", error.placeholders().get("path"),
                                "error", resolveError(error))));
                continue;
            }
            Bukkit.getLogger().warning(messages.raw("magic.cmd.spells.errors.entry",
                    Map.of("file", error.fileName(), "error", resolveError(error))));
        }
    }

    private void sendCounterTable(CommandSender sender, Component title, List<CounterEntry> entries) {
        sender.sendMessage(title);
        if (entries.isEmpty()) {
            sender.sendMessage(messages.msg("magic.cmd.admin.stats.empty"));
            return;
        }
        int index = 1;
        for (CounterEntry entry : entries) {
            sender.sendMessage(Component.text(index + ". " + entry.key() + " — " + entry.count()));
            index++;
        }
    }

    private Component formatLogEntry(CastLogEntry entry) {
        if (entry == null) {
            return Component.text("-");
        }
        String time = DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(LocalDateTime.ofInstant(entry.time(), ZoneId.systemDefault()));
        String result = entry.success() ? "OK" : "FAIL";
        String reason = entry.reasonKey() == null ? "-" : entry.reasonKey();
        String spell = entry.spellId() == null ? "-" : entry.spellId();
        return Component.text("[" + time + "] " + entry.player()
                + " -> " + spell + " " + result + " (" + reason + ")");
    }

    private void sendStaffSnapshot(CommandSender sender, Player player) {
        var staffItem = magicService.staffChargeService().findStaff(player,
                plugin.getConfig().getBoolean("staff.allowOffhand", true)).orElse(null);
        if (staffItem == null) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.staff", "staff", "none"));
            return;
        }
        var charges = magicService.staffChargeService().readCharges(staffItem.stack());
        int max = charges.max() > 0 ? charges.max() : charges.current();
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.staff",
                "staff", charges.current() + "/" + max));
    }

    private void sendReagentSnapshot(CommandSender sender, Player player, String spellId) {
        SpellDefinition spell = spellId == null ? null : magicService.spellRegistry().get(spellId);
        if (spell == null) {
            return;
        }
        var reagents = magicService.resolveEffectiveReagentsForInspect(spell);
        if (reagents == null || reagents.isEmpty()) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.reagents_none"));
            return;
        }
        List<String> missing = new ArrayList<>();
        for (ReagentItem item : reagents.items()) {
            if (item == null) {
                continue;
            }
            if (!magicService.itemsBridge().hasItem(player, item.itemId(), item.amount())) {
                missing.add(item.itemId() + " x" + item.amount());
            }
        }
        if (missing.isEmpty()) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.reagents_ok"));
            return;
        }
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.reagents_missing",
                "list", String.join(", ", missing)));
    }

    private void sendSchoolSnapshot(CommandSender sender, String spellId) {
        if (spellId == null) {
            return;
        }
        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null || spell.school() == null) {
            return;
        }
        String key = "magic.school.name." + spell.school().name();
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.school", "school", messages.raw(key)));
    }

    private void sendMasterySnapshot(CommandSender sender, Player player, String spellId) {
        if (spellId == null) {
            return;
        }
        MasteryProgress progress = magicService.masteryService().getProgress(player.getUniqueId(), spellId);
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.mastery",
                "level", String.valueOf(progress.level()),
                "xp", String.valueOf(progress.xp())));
    }

    private void sendTalentSnapshot(CommandSender sender, Player player) {
        Set<String> talents = magicService.talentMagicService().activeTalents(player);
        if (talents.isEmpty()) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.talents_none"));
            return;
        }
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.talents",
                "list", String.join(", ", talents)));
    }

    private void sendRegionSnapshot(CommandSender sender, Player player, String spellId) {
        SpellDefinition spell = spellId == null ? null : magicService.spellRegistry().get(spellId);
        CastPolicy policy = magicService.regionRuleService().castPolicy(player, spell, player.getLocation());
        if (policy.allowed()) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.region_allow"));
            return;
        }
        String reasonKey = policy.denyReasonKey() == null ? "magic.region.denied.default" : policy.denyReasonKey();
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.region_deny",
                "reason", messages.raw(reasonKey)));
    }

    private void sendGuildSnapshot(CommandSender sender, Player player, String spellId) {
        SpellDefinition spell = spellId == null ? null : magicService.spellRegistry().get(spellId);
        var modifiers = magicService.guildBonusService().guildModifiers(player, spell);
        if (modifiers.damageMultiplier() == 1.0 && modifiers.manaMultiplier() == 1.0
                && modifiers.cooldownMultiplier() == 1.0) {
            sender.sendMessage(messages.msg("magic.cmd.admin.inspect.guild_none"));
            return;
        }
        sender.sendMessage(messages.msg("magic.cmd.admin.inspect.guild",
                "damage", formatBonus(modifiers.damageMultiplier()),
                "mana", formatBonus(modifiers.manaMultiplier()),
                "cooldown", formatBonus(modifiers.cooldownMultiplier())));
    }

    private String buildReportJson() {
        List<CounterEntry> topSpells = diagnosticsService.topCasts(10);
        List<CounterEntry> topFails = diagnosticsService.topFails(10);
        List<CastLogEntry> logs = diagnosticsService.recentLogs(MagicDiagnosticsService.LogFilter.empty(), 100);
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"summary\": {\n");
        builder.append("    \"totalCasts\": ").append(diagnosticsService.totalCasts()).append(",\n");
        builder.append("    \"totalFails\": ").append(diagnosticsService.totalFails()).append("\n");
        builder.append("  },\n");
        builder.append("  \"topSpells\": ").append(writeCounterArray(topSpells)).append(",\n");
        builder.append("  \"topFailures\": ").append(writeCounterArray(topFails)).append(",\n");
        builder.append("  \"logs\": ").append(writeLogArray(logs)).append("\n");
        builder.append("}\n");
        return builder.toString();
    }

    private String writeCounterArray(List<CounterEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < entries.size(); i++) {
            CounterEntry entry = entries.get(i);
            builder.append("{\"key\":\"").append(escape(entry.key())).append("\",");
            builder.append("\"count\":").append(entry.count()).append("}");
            if (i + 1 < entries.size()) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private String writeLogArray(List<CastLogEntry> entries) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < entries.size(); i++) {
            CastLogEntry entry = entries.get(i);
            builder.append("{\"time\":\"").append(entry.time()).append("\",");
            builder.append("\"player\":\"").append(escape(entry.player())).append("\",");
            builder.append("\"spellId\":\"").append(escape(entry.spellId())).append("\",");
            builder.append("\"success\":").append(entry.success()).append(",");
            builder.append("\"reasonKey\":\"").append(escape(entry.reasonKey())).append("\"}");
            if (i + 1 < entries.size()) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private int parseOptionalInt(String[] args, int index, int fallback) {
        if (args.length <= index) {
            return fallback;
        }
        try {
            return Integer.parseInt(args[index]);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private BypassType parseBypassType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "requirements" -> BypassType.REQUIREMENTS;
            case "cooldown" -> BypassType.COOLDOWN;
            case "mana" -> BypassType.MANA;
            case "reagents" -> BypassType.REAGENTS;
            case "economy" -> BypassType.ECONOMY;
            case "staff" -> BypassType.STAFF;
            default -> null;
        };
    }

    private String formatBypass(Map<BypassType, Instant> entries) {
        if (entries == null || entries.isEmpty()) {
            return "-";
        }
        List<Map.Entry<BypassType, Instant>> list = new ArrayList<>(entries.entrySet());
        list.sort(Comparator.comparing(entry -> entry.getKey().name()));
        List<String> parts = new ArrayList<>();
        for (var entry : list) {
            long minutes = Math.max(0, java.time.Duration.between(Instant.now(), entry.getValue()).toMinutes());
            parts.add(entry.getKey().name().toLowerCase(Locale.ROOT) + " (" + minutes + "m)");
        }
        return String.join(", ", parts);
    }
    private String resolveError(SpellLoadError error) {
        if (error.messageKey() != null) {
            return messages.raw(error.messageKey(), error.placeholders());
        }
        if (error.message() != null) {
            return error.message();
        }
        return messages.raw("magic.cmd.spells.errors.unknown");
    }


    private boolean handleSpellGive(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found",
                    "player", args[2]));
            return true;
        }
        SpellDefinition spell = magicService.spellRegistry().get(args[3]);
        if (spell == null) {
            sender.sendMessage(messages.msg("magic.command.spell.unknown_spell",
                    "spellId", args[3]));
            return true;
        }
        UnlockResult result = playerSpellService.unlock(target.getUniqueId(), spell.id(), SpellUnlockSource.COMMAND);
        if (result instanceof UnlockResult.Fail fail) {
            return handleSpellActionFailure(sender, fail.reason(), spell.id());
        }
        sender.sendMessage(messages.msg("magic.cmd.spell.give.ok",
                "player", target.getName(),
                "spell", displaySpellName(spell.id())));
        return true;
    }

    private boolean handleSpellRemove(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found",
                    "player", args[2]));
            return true;
        }
        SpellDefinition spell = magicService.spellRegistry().get(args[3]);
        if (spell == null) {
            sender.sendMessage(messages.msg("magic.command.spell.unknown_spell",
                    "spellId", args[3]));
            return true;
        }
        RevokeResult result = playerSpellService.revoke(target.getUniqueId(), spell.id(), SpellUnlockSource.COMMAND);
        if (result instanceof RevokeResult.Fail fail) {
            return handleSpellActionFailure(sender, fail.reason(), spell.id());
        }
        sender.sendMessage(messages.msg("magic.cmd.spell.remove.ok",
                "player", target.getName(),
                "spell", displaySpellName(spell.id())));
        return true;
    }

    private boolean handleSpellList(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found",
                    "player", args[2]));
            return true;
        }
        sender.sendMessage(messages.msg("magic.cmd.spell.list.header",
                "player", target.getName()));
        for (String spellId : playerSpellService.listLearned(target.getUniqueId())) {
            sender.sendMessage(messages.msg("magic.cmd.spell.list.entry",
                    "spell", displaySpellName(spellId),
                    "id", spellId));
        }
        return true;
    }

    private boolean handleSpellSelect(CommandSender sender, String[] args) {
        if (args.length != 4) {
            sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found",
                    "player", args[2]));
            return true;
        }
        SelectResult result = playerSpellService.select(target.getUniqueId(), args[3]);
        if (result instanceof SelectResult.Fail fail) {
            return handleSpellActionFailure(sender, fail.reason(), args[3]);
        }
        sender.sendMessage(messages.msg("magic.cmd.spell.select.ok",
                "player", target.getName(),
                "spell", displaySpellName(args[3])));
        return true;
    }

    private boolean handleSpellClear(CommandSender sender, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(messages.msg("magic.cmd.spell.usage"));
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(messages.msg("magic.cmd.player_not_found",
                    "player", args[2]));
            return true;
        }
        playerSpellService.clearSelected(target.getUniqueId());
        sender.sendMessage(messages.msg("magic.command.spell.cleared_other",
                "player", target.getName()));
        return true;
    }

    private boolean handleSpellActionFailure(CommandSender sender, SpellActionReason reason, String spellId) {
        if (reason == SpellActionReason.UNKNOWN_SPELL) {
            sender.sendMessage(messages.msg("magic.command.spell.unknown_spell",
                    "spellId", spellId));
            return true;
        }
        if (reason == SpellActionReason.NOT_LEARNED) {
            sender.sendMessage(messages.msg("magic.spell.select.not_learned",
                    "spell", displaySpellName(spellId)));
            return true;
        }
        sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
        return true;
    }

    private List<String> tabCompletePlayers() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private List<String> spellIds() {
        List<String> ids = new ArrayList<>();
        for (SpellDefinition spell : magicService.spellRegistry().all()) {
            ids.add(spell.id());
        }
        return ids;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private String formatBonus(double multiplier) {
        double percent = (multiplier - 1.0) * 100.0;
        return String.format(Locale.US, "%+.1f%%", percent);
    }

    private String normalizeSpellId(String spellId) {
        if (spellId == null) {
            return null;
        }
        String trimmed = spellId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
