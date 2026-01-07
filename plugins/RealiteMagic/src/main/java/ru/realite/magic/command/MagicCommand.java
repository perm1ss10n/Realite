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
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicCommand implements CommandExecutor, TabCompleter {

    private static final String PERMISSION_SELECT = "realite.magic.spell.select";
    private static final String PERMISSION_ADMIN = "realite.magic.admin";

    private final MagicService magicService;
    private final MagicMessages messages;

    public MagicCommand(MagicService magicService, MagicMessages messages) {
        this.magicService = magicService;
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
        sender.sendMessage(messages.msg("magic.command.usage.root"));
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
        if (args.length == 2) {
            return List.of("select", "clear", "get");
        }
        if (args[1].equalsIgnoreCase("select")) {
            return tabCompleteSelect(sender, args);
        }
        if (args[1].equalsIgnoreCase("clear") || args[1].equalsIgnoreCase("get")) {
            return tabCompletePlayer(sender, args.length);
        }
        return Collections.emptyList();
    }

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
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
        if (args.length < 2) {
            sender.sendMessage(messages.msg("magic.command.usage.spell"));
            return true;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "select" -> handleSpellSelect(sender, args);
            case "clear" -> handleSpellClear(sender, args);
            case "get" -> handleSpellGet(sender, args);
            default -> {
                sender.sendMessage(messages.msg("magic.command.usage.spell"));
                yield true;
            }
        };
    }

    private boolean handleSpellSelect(CommandSender sender, String[] args) {
        if (args.length == 3) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.msg("magic.command.only-player"));
                return true;
            }
            if (!hasSelectPermission(sender)) {
                sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
                return true;
            }
            return selectSpell(sender, player, args[2], false);
        }
        if (args.length == 4) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(messages.msg("magic.command.errors.player_not_found",
                        "player", args[2]));
                return true;
            }
            return selectSpell(sender, target, args[3], true);
        }
        sender.sendMessage(messages.msg("magic.command.usage.spell"));
        return true;
    }

    private boolean selectSpell(CommandSender sender, Player target, String spellId, boolean adminMode) {
        SpellRegistry registry = magicService.spellRegistry();
        SpellDefinition spell = registry.get(spellId);
        if (spell == null) {
            sender.sendMessage(messages.msg("magic.command.spell.unknown_spell",
                    "spellId", spellId));
            return true;
        }
        if (!magicService.meetsRequirements(target, spell)) {
            String reason = magicService.spellSelectMenu().requirementReason(spell);
            if (reason == null) {
                reason = "";
            }
            sender.sendMessage(messages.msg("magic.command.spell.locked",
                    "reason", reason));
            return true;
        }
        magicService.setSelectedSpell(target, spell.id());
        String spellName = messages.raw(spell.nameKey());
        if (adminMode) {
            sender.sendMessage(messages.msg("magic.command.spell.selected_other",
                    "player", target.getName(),
                    "spell", spellName));
            if (!(sender instanceof Player senderPlayer) || !senderPlayer.getUniqueId().equals(target.getUniqueId())) {
                target.sendMessage(messages.msg("magic.command.spell.selected_self",
                        "spell", spellName));
            }
        } else {
            sender.sendMessage(messages.msg("magic.command.spell.selected_self",
                    "spell", spellName));
        }
        return true;
    }

    private boolean handleSpellClear(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.msg("magic.command.only-player"));
                return true;
            }
            if (!hasSelectPermission(sender)) {
                sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
                return true;
            }
            magicService.clearSelectedSpell(player);
            sender.sendMessage(messages.msg("magic.command.spell.cleared_self"));
            return true;
        }
        if (args.length == 3) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(messages.msg("magic.command.errors.player_not_found",
                        "player", args[2]));
                return true;
            }
            magicService.clearSelectedSpell(target);
            sender.sendMessage(messages.msg("magic.command.spell.cleared_other",
                    "player", target.getName()));
            if (!(sender instanceof Player senderPlayer) || !senderPlayer.getUniqueId().equals(target.getUniqueId())) {
                target.sendMessage(messages.msg("magic.command.spell.cleared_self"));
            }
            return true;
        }
        sender.sendMessage(messages.msg("magic.command.usage.spell"));
        return true;
    }

    private boolean handleSpellGet(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.msg("magic.command.only-player"));
                return true;
            }
            if (!hasSelectPermission(sender)) {
                sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
                return true;
            }
            return sendSelectedSpell(sender, player, false);
        }
        if (args.length == 3) {
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                sender.sendMessage(messages.msg("magic.command.errors.no_permission"));
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(messages.msg("magic.command.errors.player_not_found",
                        "player", args[2]));
                return true;
            }
            return sendSelectedSpell(sender, target, true);
        }
        sender.sendMessage(messages.msg("magic.command.usage.spell"));
        return true;
    }

    private boolean sendSelectedSpell(CommandSender sender, Player target, boolean adminMode) {
        String spellId = magicService.getSelectedSpellId(target);
        if (spellId == null || spellId.isBlank()) {
            if (adminMode) {
                sender.sendMessage(messages.msg("magic.command.spell.get_empty_other",
                        "player", target.getName()));
            } else {
                sender.sendMessage(messages.msg("magic.command.spell.get_empty_self"));
            }
            return true;
        }
        SpellDefinition spell = magicService.spellRegistry().get(spellId);
        if (spell == null) {
            magicService.clearSelectedSpell(target);
            sender.sendMessage(messages.msg("magic.command.spell.unknown_spell",
                    "spellId", spellId));
            return true;
        }
        String spellName = messages.raw(spell.nameKey());
        if (adminMode) {
            sender.sendMessage(messages.msg("magic.command.spell.get_other",
                    "player", target.getName(),
                    "spell", spellName));
        } else {
            sender.sendMessage(messages.msg("magic.command.spell.get_self",
                    "spell", spellName));
        }
        return true;
    }

    private List<String> tabCompleteSelect(CommandSender sender, String[] args) {
        if (args.length == 3) {
            List<String> suggestions = new ArrayList<>();
            if (!sender.hasPermission(PERMISSION_ADMIN)) {
                return spellIds();
            }
            for (Player player : Bukkit.getOnlinePlayers()) {
                suggestions.add(player.getName());
            }
            suggestions.addAll(spellIds());
            return suggestions;
        }
        if (args.length == 4) {
            return spellIds();
        }
        return Collections.emptyList();
    }

    private List<String> tabCompletePlayer(CommandSender sender, int argsLength) {
        if (argsLength == 3 && sender.hasPermission(PERMISSION_ADMIN)) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        return Collections.emptyList();
    }

    private List<String> spellIds() {
        List<String> ids = new ArrayList<>();
        for (SpellDefinition spell : magicService.spellRegistry().all()) {
            ids.add(spell.id());
        }
        return ids;
    }

    private boolean hasSelectPermission(CommandSender sender) {
        return sender.hasPermission(PERMISSION_SELECT) || sender.hasPermission(PERMISSION_ADMIN);
    }

    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
