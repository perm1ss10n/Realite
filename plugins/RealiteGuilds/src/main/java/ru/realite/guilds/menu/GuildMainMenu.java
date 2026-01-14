package ru.realite.guilds.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.service.GuildRankPermission;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.service.GuildService;
import ru.realite.guilds.service.GuildTreasuryService;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

public final class GuildMainMenu extends GuildMenu {

    private static final int SIZE = 54;

    private final java.util.UUID viewerId;
    private final GuildRepository repository;
    private final GuildService service;
    private final GuildRankService rankService;
    private final GuildUpgradeConfigRepository upgradeConfig;
    private final GuildTreasuryService treasuryService;

    public GuildMainMenu(
            GuildMenuManager manager,
            GuildMessages messages,
            GuildRepository repository,
            GuildService service,
            GuildRankService rankService,
            GuildUpgradeConfigRepository upgradeConfig,
            GuildTreasuryService treasuryService,
            Player viewer) {
        super(manager, messages, SIZE, "ui.guild.main.title");
        this.viewerId = viewer.getUniqueId();
        this.repository = repository;
        this.service = service;
        this.rankService = rankService;
        this.upgradeConfig = upgradeConfig;
        this.treasuryService = treasuryService;
        build();
    }

    private void build() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        GuildMember member = repository.getMember(viewerId);
        if (member == null) {
            renderNoGuild();
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            renderNoGuild();
            return;
        }
        renderGuildInfo(member, guild);
        renderGuildButtons(member, guild);
    }

    private void renderNoGuild() {
        setButton(13, Material.BARRIER, messages.msg("ui.guild.main.no_guild"), null, null);
        setButton(20, Material.ANVIL, "ui.guild.main.create", List.of("ui.guild.main.lore.input"),
                player -> manager.requestInput(player, GuildChatInputService.InputType.CREATE));
        setButton(24, Material.OAK_DOOR, "ui.guild.main.join", List.of("ui.guild.main.lore.open"),
                manager::handleJoin);
        setButton(49, Material.BARRIER, "ui.common.close", List.of(), Player::closeInventory);
    }

    private void renderGuildInfo(GuildMember member, Guild guild) {
        int members = repository.countMembersByTag(guild.tag());
        int maxMembers = service.getMaxMembers(guild.tag());
        double balance = treasuryService.getBalance(guild.tag());
        List<Component> lore = List.of(
                messages.msg("ui.guild.main.info.name", "name", guild.name()),
                messages.msg("ui.guild.main.info.tag", "tag", guild.tag()),
                messages.msg("ui.guild.main.info.level", "level", String.valueOf(guild.level())),
                messages.msg("ui.guild.main.info.xp", "xp", String.valueOf(guild.xp())),
                messages.msg("ui.guild.main.info.members", "count", String.valueOf(members),
                        "max", String.valueOf(maxMembers)),
                messages.msg("ui.guild.main.info.treasury", "amount", formatAmount(balance))
        );
        setButton(4, Material.BOOK, messages.msg("ui.guild.main.info.title"), lore, null);
        setButton(49, Material.BARRIER, "ui.common.close", List.of(), Player::closeInventory);
    }

    private void renderGuildButtons(GuildMember member, Guild guild) {
        setButton(20, Material.NAME_TAG, "ui.guild.main.members", List.of("ui.guild.main.lore.open"),
                player -> manager.openMembers(player, 0));

        if (rankService.getRanksByPriority().isEmpty()) {
            setButton(22, Material.GRAY_DYE, messages.msg("ui.guild.main.ranks_disabled"), null, null);
        } else {
            setButton(22, Material.WRITABLE_BOOK, "ui.guild.main.ranks", List.of("ui.guild.main.lore.run"),
                    player -> manager.runCommand(player, "g ranks"));
        }

        if (upgradeConfig.getUpgrades().isEmpty()) {
            setButton(24, Material.GRAY_DYE, messages.msg("ui.guild.main.upgrades_disabled"), null, null);
        } else {
            setButton(24, Material.NETHER_STAR, "ui.guild.main.upgrades", List.of("ui.guild.main.lore.open"),
                    player -> manager.openUpgrades(player, 0));
        }

        setButton(30, Material.SUNFLOWER, "ui.guild.main.treasury", List.of("ui.guild.main.lore.open"),
                manager::openTreasury);

        setButton(32, Material.REDSTONE, messages.msg("ui.guild.main.settings_disabled"),
                List.of(messages.msg("ui.guild.main.lore.unavailable")), null);

        boolean isOwner = guild.owner().equals(viewerId);
        if (isOwner) {
            setButton(40, Material.TNT, "ui.guild.main.disband", List.of("ui.guild.main.lore.confirm"),
                    player -> GuildConfirmMenu.openDisband(manager, player));
        } else {
            setButton(40, Material.OAK_DOOR, "ui.guild.main.leave", List.of("ui.guild.main.lore.confirm"),
                    player -> GuildConfirmMenu.openLeave(manager, player));
        }

        boolean canInvite = rankService.hasPermission(member.role(), GuildRankPermission.INVITE);
        if (canInvite) {
            setButton(31, Material.PAPER, "ui.guild.main.invite", List.of("ui.guild.main.lore.input"),
                    player -> manager.requestInput(player, GuildChatInputService.InputType.INVITE));
        }
    }

    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }

}
