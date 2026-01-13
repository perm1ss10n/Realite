package ru.realite.magic.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;

public final class SpellsCommand implements CommandExecutor {

    private static final String PERMISSION_MENU = "realite.magic.menu";

    private final MagicMessages messages;

    public SpellsCommand(MagicMessages messages) {
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.msg("magic.command.only-player"));
            return true;
        }
        if (!player.hasPermission(PERMISSION_MENU)) {
            player.sendMessage(messages.msg("magic.command.errors.no_permission"));
            return true;
        }
        CoreApi core = resolveCore();
        if (core == null) {
            player.sendMessage(messages.msg("magic.ui.spells.unavailable"));
            return true;
        }
        UiScreenRegistry registry = core.services().get(UiScreenRegistry.class);
        if (registry == null || !registry.open(player, "magic.spells")) {
            player.sendMessage(messages.msg("magic.ui.spells.unavailable"));
        }
        return true;
    }

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        return provider.getProvider();
    }
}
