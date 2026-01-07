package ru.realite.magic.command;

import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;

public final class MagicCommand implements CommandExecutor {

    private final MagicService magicService;
    private final MagicMessages messages;

    public MagicCommand(MagicService magicService, MagicMessages messages) {
        this.magicService = magicService;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        if (args.length == 0) {
            magicService.spellSelectMenu().open(player);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if ("mana".equals(sub)) {
            double mana = magicService.getMana(player);
            double max = magicService.getMaxMana(player);
            Component msg = messages.msg("magic.mana.actionbar",
                    "mana", format(mana),
                    "max", format(max));
            player.sendActionBar(msg);
            return true;
        }
        if ("menu".equals(sub)) {
            magicService.spellSelectMenu().open(player);
            return true;
        }
        player.sendMessage(messages.msg("magic.command.usage.root"));
        return true;
    }

    private String format(double value) {
        return String.format(Locale.US, "%.1f", value);
    }
}
