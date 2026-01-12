package ru.realite.guilds.menu;

import java.util.List;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildService;

public final class GuildMenuManager {

    private final GuildMessages messages;
    private final GuildService service;
    private final GuildChatInputService inputService;

    public GuildMenuManager(GuildMessages messages, GuildService service, GuildChatInputService inputService) {
        this.messages = messages;
        this.service = service;
        this.inputService = inputService;
    }

    public void openRoot(Player player) {
        new GuildCommandsMenu(this, messages).open(player);
    }

    public void openBasics(Player player) {
        new GuildCommandsBasicsMenu(this, messages).open(player);
    }

    public void openMembers(Player player) {
        new GuildCommandsMembersMenu(this, messages).open(player);
    }

    public void openTerritory(Player player) {
        new GuildCommandsTerritoryMenu(this, messages).open(player);
    }

    public void openClaim(Player player) {
        new GuildCommandsClaimMenu(this, messages).open(player);
    }

    public void openEconomy(Player player) {
        new GuildCommandsEconomyMenu(this, messages).open(player);
    }

    public void openJoinSelection(Player player, List<String> invites) {
        new GuildCommandsJoinMenu(this, messages, invites).open(player);
    }

    public void handleJoin(Player player) {
        List<String> invites = service.getActiveInvites(player);
        if (invites.size() == 1) {
            player.performCommand("g join " + invites.getFirst());
            return;
        }
        if (invites.size() > 1) {
            openJoinSelection(player, invites);
            return;
        }
        messages.send(player, "invite.none");
    }

    public void requestInput(Player player, GuildChatInputService.InputType type) {
        inputService.requestInput(player, type);
    }

    public void runCommand(Player player, String command) {
        player.performCommand(command);
    }

}
