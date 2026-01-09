package ru.realite.magic.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.realite.magic.debug.DebugService;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.RevokeResult;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellActionReason;
import ru.realite.magic.service.SpellUnlockSource;
import ru.realite.magic.service.UnlockResult;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellLoadError;
import ru.realite.magic.spell.SpellLoadReport;

public final class MagicCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_ADMIN = "realite.magic.admin";
    private static final String PERMISSION_MENU = "realite.magic.menu";
    private static final String PERMISSION_USE = "realite.magic.use";

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;
    private final DebugService debugService;

    public MagicCommand(MagicService magicService,
                        PlayerSpellService playerSpellService,
                        MagicMessages messages,
                        DebugService debugService) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
        this.debugService = debugService;
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
        if ("debug".equals(sub)) {
            return handleDebug(sender, args);
        }
        sender.sendMessage(messages.msg("magic.cmd.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("menu", "spell", "spells", "mana", "debug", "slot");
        }
        if (args.length >= 2
                && !args[0].equalsIgnoreCase("spell")
                && !args[0].equalsIgnoreCase("spells")
                && !args[0].equalsIgnoreCase("debug")
                && !args[0].equalsIgnoreCase("slot")) {
            return Collections.emptyList();
        }
        if (args[0].equalsIgnoreCase("slot")) {
            return tabCompleteSlots(args.length);
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
        Component msg = messages.msg("magic.mana.actionbar",
                "mana", format(mana),
                "max", format(max));
        player.sendActionBar(msg);
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
        sendSlotActionbar(player, slot);
        return true;
    }

    private void sendSlotActionbar(Player player, int slot) {
        String spellId = playerSpellService.getActiveSlotSpell(player.getUniqueId()).orElse(null);
        if (spellId == null) {
            player.sendActionBar(messages.msg("magic.bar.slot.empty", "slot", String.valueOf(slot)));
            return;
        }
        String spellName = displaySpellName(spellId);
        player.sendActionBar(messages.msg("magic.bar.slot.changed",
                "slot", String.valueOf(slot),
                "spell", spellName));
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
}
