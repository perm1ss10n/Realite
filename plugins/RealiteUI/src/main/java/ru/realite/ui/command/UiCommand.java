package ru.realite.ui.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.realite.core.api.ui.UiRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.hud.UiHudService;
import ru.realite.ui.menu.UiSettingsMenu;
import ru.realite.ui.settings.UiSettingsStore;

public final class UiCommand implements CommandExecutor {

    private final MiniMessageMessages messages;
    private final UiSettingsStore settingsStore;
    private final UiHudService hudService;
    private final UiRegistry registry;

    public UiCommand(MiniMessageMessages messages,
                     UiSettingsStore settingsStore,
                     UiHudService hudService,
                     UiRegistry registry) {
        this.messages = messages;
        this.settingsStore = settingsStore;
        this.hudService = hudService;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("ui.command.player_only"));
            return true;
        }
        new UiSettingsMenu(messages, settingsStore, hudService, registry).open(player);
        return true;
    }
}
