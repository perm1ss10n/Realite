package ru.realite.magic.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.RevokeResult;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellActionReason;
import ru.realite.magic.service.UnlockCause;
import ru.realite.magic.service.UnlockResult;
import ru.realite.magic.spell.SpellDefinition;

public final class MagicCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_ADMIN = "realite.magic.admin";
    private static final String PERMISSION_MENU = "realite.magic.menu";

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;

    public MagicCommand(MagicService magicService, PlayerSpellService playerSpellService, MagicMessages messages) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
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
        sender.sendMessage(messages.msg("magic.cmd.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("menu", "spell", "mana");
        }
        if (args.length >= 2 && !args[0].equalsIgnoreCase("spell")) {
            return Collections.emptyList();
        }
        if (!sender.hasPermission(PERMISSION_ADMIN)) {
            return Collections.emptyList();
        }
        if (args.length == 2) {
            return List.of("give", "remove", "list", "select", "clear");
        }
        String action = args[1].toLowerCase(Locale.ROOT);
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
        UnlockResult result = playerSpellService.unlock(target.getUniqueId(), spell.id(), UnlockCause.COMMAND);
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
        RevokeResult result = playerSpellService.revoke(target.getUniqueId(), spell.id(), UnlockCause.COMMAND);
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

    private String displaySpellName(String spellId) {
        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null) {
            return spellId;
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        return messages.raw(nameKey);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
