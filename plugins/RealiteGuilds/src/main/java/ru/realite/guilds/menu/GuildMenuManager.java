package ru.realite.guilds.menu;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.service.GuildService;
import ru.realite.guilds.service.GuildTreasuryService;
import ru.realite.guilds.service.GuildUpgradeService;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

public final class GuildMenuManager {

    private final GuildMessages messages;
    private final GuildService service;
    private final GuildRepository repository;
    private final GuildRankService rankService;
    private final GuildUpgradeService upgradeService;
    private final GuildUpgradeConfigRepository upgradeConfig;
    private final GuildTreasuryService treasuryService;
    private final GuildChatInputService inputService;

    public GuildMenuManager(
            GuildMessages messages,
            GuildService service,
            GuildRepository repository,
            GuildRankService rankService,
            GuildUpgradeService upgradeService,
            GuildUpgradeConfigRepository upgradeConfig,
            GuildTreasuryService treasuryService,
            GuildChatInputService inputService) {
        this.messages = messages;
        this.service = service;
        this.repository = repository;
        this.rankService = rankService;
        this.upgradeService = upgradeService;
        this.upgradeConfig = upgradeConfig;
        this.treasuryService = treasuryService;
        this.inputService = inputService;
    }

    public void openRoot(Player player) {
        new GuildMainMenu(this, messages, repository, service, rankService, upgradeConfig, treasuryService, player)
                .open(player);
    }

    public void openJoinSelection(Player player, List<String> invites) {
        new GuildJoinMenu(this, messages, invites).open(player);
    }

    public void openMembers(Player player, int page) {
        new GuildMembersMenu(this, messages, repository, rankService, page, player).open(player);
    }

    public void openMemberProfile(Player player, UUID memberId, int returnPage) {
        new GuildMemberProfileMenu(this, messages, repository, rankService, memberId, returnPage, player)
                .open(player);
    }

    public void openUpgrades(Player player, int page) {
        new GuildUpgradesMenu(this, messages, repository, upgradeService, upgradeConfig, page, player)
                .open(player);
    }

    public void openTreasury(Player player) {
        new GuildTreasuryMenu(this, messages, repository, rankService, treasuryService, player).open(player);
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

    public GuildMessages messages() {
        return messages;
    }

    public GuildService service() {
        return service;
    }

    public GuildRepository repository() {
        return repository;
    }

    public GuildRankService rankService() {
        return rankService;
    }

    public GuildUpgradeService upgradeService() {
        return upgradeService;
    }

    public GuildUpgradeConfigRepository upgradeConfig() {
        return upgradeConfig;
    }

    public GuildTreasuryService treasuryService() {
        return treasuryService;
    }
}
