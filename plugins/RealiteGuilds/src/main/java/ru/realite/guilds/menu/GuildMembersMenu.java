package ru.realite.guilds.menu;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
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

public final class GuildMembersMenu extends GuildMenu {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private final GuildRepository repository;
    private final GuildRankService rankService;
    private final Player viewer;
    private final int page;

    public GuildMembersMenu(
            GuildMenuManager manager,
            GuildMessages messages,
            GuildRepository repository,
            GuildRankService rankService,
            int page,
            Player viewer) {
        super(manager, messages, SIZE, "ui.guild.members.title");
        this.repository = repository;
        this.rankService = rankService;
        this.viewer = viewer;
        this.page = Math.max(0, page);
        build();
    }

    private void build() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        GuildMember viewerMember = repository.getMember(viewer.getUniqueId());
        if (viewerMember == null) {
            messages.send(viewer, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(viewerMember.tag());
        if (guild == null) {
            messages.send(viewer, "guild.not_found");
            return;
        }

        List<GuildMember> members = repository.getMembers().stream()
                .filter(member -> member.tag().equalsIgnoreCase(guild.tag()))
                .sorted(memberComparator())
                .collect(Collectors.toList());

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, members.size());
        if (start >= members.size() && page > 0) {
            manager.openMembers(viewer, page - 1);
            return;
        }
        for (int i = start; i < end; i++) {
            GuildMember member = members.get(i);
            int slot = i - start;
            setMemberItem(member, guild, slot);
        }

        setButton(45, Material.ARROW, "ui.common.back", List.of("ui.guild.menu.back_lore"), manager::openRoot);
        if (page > 0) {
            setButton(48, Material.ARROW, "ui.common.prev", List.of("ui.guild.menu.page_lore"),
                    player -> manager.openMembers(player, page - 1));
        } else {
            setButton(48, Material.GRAY_DYE, messages.msg("ui.guild.menu.prev_disabled"), null, null);
        }
        if (end < members.size()) {
            setButton(50, Material.ARROW, "ui.common.next", List.of("ui.guild.menu.page_lore"),
                    player -> manager.openMembers(player, page + 1));
        } else {
            setButton(50, Material.GRAY_DYE, messages.msg("ui.guild.menu.next_disabled"), null, null);
        }

        boolean canInvite = rankService.hasPermission(viewerMember.role(), GuildRankPermission.INVITE);
        if (canInvite) {
            setButton(53, Material.PAPER, "ui.guild.members.invite", List.of("ui.guild.menu.input_lore"),
                    player -> manager.requestInput(player, GuildChatInputService.InputType.INVITE));
        } else {
            setButton(53, Material.GRAY_DYE, messages.msg("ui.guild.members.invite_disabled"), null, null);
        }
    }

    private void setMemberItem(GuildMember member, Guild guild, int slot) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(member.uuid());
        String name = offline.getName() == null ? member.uuid().toString() : offline.getName();
        GuildRankService.GuildRank rank = rankService.getRank(member.role());
        String rankName = rank == null ? member.role() : messages.raw(rank.displayNameKey());
        Component title = messages.msg("ui.guild.members.entry.title", "name", name);
        List<Component> lore = List.of(
                messages.msg("ui.guild.members.entry.rank", "rank", rankName),
                messages.msg(offline.isOnline()
                        ? "ui.guild.members.entry.online"
                        : "ui.guild.members.entry.offline"),
                guild.owner().equals(member.uuid())
                        ? messages.msg("ui.guild.members.entry.owner")
                        : messages.msg("ui.guild.members.entry.member")
        );
        setButton(slot, Material.PLAYER_HEAD, title, lore,
                player -> manager.openMemberProfile(player, member.uuid(), page));
    }

    private Comparator<GuildMember> memberComparator() {
        return Comparator.<GuildMember>comparingInt(member -> {
            GuildRankService.GuildRank rank = rankService.getRank(member.role());
            return rank == null ? 0 : rank.priority();
        }).reversed().thenComparing(member -> {
            OfflinePlayer offline = Bukkit.getOfflinePlayer(member.uuid());
            return offline.getName() == null ? member.uuid().toString() : offline.getName();
        }, String.CASE_INSENSITIVE_ORDER);
    }
}
