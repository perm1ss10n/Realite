package ru.realite.guilds.menu;

import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.service.GuildRankPermission;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildMemberProfileMenu extends GuildMenu {

    private static final int SIZE = 27;

    private final GuildRepository repository;
    private final GuildRankService rankService;
    private final UUID targetId;
    private final Player viewer;
    private final int returnPage;

    public GuildMemberProfileMenu(
            GuildMenuManager manager,
            GuildMessages messages,
            GuildRepository repository,
            GuildRankService rankService,
            UUID targetId,
            int returnPage,
            Player viewer) {
        super(manager, messages, SIZE, "ui.guild.member.title");
        this.repository = repository;
        this.rankService = rankService;
        this.targetId = targetId;
        this.viewer = viewer;
        this.returnPage = Math.max(0, returnPage);
        build();
    }

    private void build() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        GuildMember viewerMember = repository.getMember(viewer.getUniqueId());
        GuildMember targetMember = repository.getMember(targetId);
        if (viewerMember == null || targetMember == null) {
            messages.send(viewer, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(viewerMember.tag());
        if (guild == null || !guild.tag().equalsIgnoreCase(targetMember.tag())) {
            messages.send(viewer, "guild.not_found");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String displayName = target.getName() == null ? targetId.toString() : target.getName();
        String commandTarget = target.getName();
        GuildRankService.GuildRank targetRank = rankService.getRank(targetMember.role());
        String rankName = targetRank == null ? targetMember.role() : messages.raw(targetRank.displayNameKey());

        List<Component> lore = List.of(
                messages.msg("ui.guild.member.rank", "rank", rankName),
                messages.msg(target.isOnline() ? "ui.guild.member.online" : "ui.guild.member.offline")
        );
        setButton(13, Material.PLAYER_HEAD, messages.msg("ui.guild.member.name", "name", displayName), lore, null);

        GuildRankService.GuildRank viewerRank = rankService.getRank(viewerMember.role());
        boolean canPromote = rankService.hasPermission(viewerMember.role(), GuildRankPermission.PROMOTE);
        if (canPromote && viewerRank != null && targetRank != null && commandTarget != null) {
            int promoteIndex = nextHigherRankIndex(targetRank);
            if (promoteIndex >= 0) {
                GuildRankService.GuildRank promoteRank = rankService.getRanksByPriority().get(promoteIndex);
                List<Component> promoteLore = List.of(
                        messages.msg("ui.guild.member.promote_lore"),
                        messages.msg("ui.guild.member.rank_target", "rank", messages.raw(promoteRank.displayNameKey()))
                );
                setButton(11, Material.LIME_WOOL, messages.msg("ui.guild.member.promote"), promoteLore,
                        player -> manager.runCommand(player, "g setrank " + commandTarget + " " + promoteRank.id()));
            } else {
                setButton(11, Material.GRAY_DYE, messages.msg("ui.guild.member.promote_disabled"), null, null);
            }

            int demoteIndex = nextLowerRankIndex(targetRank);
            if (demoteIndex >= 0) {
                GuildRankService.GuildRank demoteRank = rankService.getRanksByPriority().get(demoteIndex);
                List<Component> demoteLore = List.of(
                        messages.msg("ui.guild.member.demote_lore"),
                        messages.msg("ui.guild.member.rank_target", "rank", messages.raw(demoteRank.displayNameKey()))
                );
                setButton(15, Material.ORANGE_WOOL, messages.msg("ui.guild.member.demote"), demoteLore,
                        player -> manager.runCommand(player, "g setrank " + commandTarget + " " + demoteRank.id()));
            } else {
                setButton(15, Material.GRAY_DYE, messages.msg("ui.guild.member.demote_disabled"), null, null);
            }
        } else {
            setButton(11, Material.GRAY_DYE, messages.msg("ui.guild.member.promote_disabled"), null, null);
            setButton(15, Material.GRAY_DYE, messages.msg("ui.guild.member.demote_disabled"), null, null);
        }

        setButton(21, Material.BARRIER, messages.msg("ui.guild.member.kick_disabled"),
                List.of(messages.msg("ui.guild.member.unavailable")), null);
        setButton(22, Material.ARROW, "ui.common.back", List.of("ui.guild.menu.back_lore"),
                player -> manager.openMembers(player, returnPage));
    }

    private int nextHigherRankIndex(GuildRankService.GuildRank targetRank) {
        List<GuildRankService.GuildRank> ranks = rankService.getRanksByPriority();
        int index = ranks.indexOf(targetRank);
        if (index <= 0) {
            return -1;
        }
        return index - 1;
    }

    private int nextLowerRankIndex(GuildRankService.GuildRank targetRank) {
        List<GuildRankService.GuildRank> ranks = rankService.getRanksByPriority();
        int index = ranks.indexOf(targetRank);
        if (index < 0 || index + 1 >= ranks.size()) {
            return -1;
        }
        return index + 1;
    }
}
