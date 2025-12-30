package ru.realite.guilds.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildService {

    private final FileConfiguration config;
    private final GuildRepository repository;
    private final GuildMessages messages;
    private final GuildRankService rankService;
    private final Map<UUID, GuildInvite> invites = new HashMap<>();
    private final Map<UUID, Long> leaveCooldowns = new HashMap<>();

    public GuildService(FileConfiguration config, GuildRepository repository, GuildMessages messages,
                        GuildRankService rankService) {
        this.config = config;
        this.repository = repository;
        this.messages = messages;
        this.rankService = rankService;
    }

    public void create(Player player, String tagRaw, String nameRaw) {
        String tag = tagRaw == null ? "" : tagRaw.trim().toUpperCase(Locale.ROOT);
        if (tag.isBlank()) {
            messages.send(player, "usage.create");
            return;
        }
        int min = config.getInt("guild.tag.min", 2);
        int max = config.getInt("guild.tag.max", 4);
        if (tag.length() < min || tag.length() > max) {
            messages.send(player, "error.guild.tag-length",
                    "min", String.valueOf(min),
                    "max", String.valueOf(max));
            return;
        }
        String regex = config.getString("guild.tag.regex", "^[A-Z0-9]+$");
        if (regex != null && !Pattern.matches(regex, tag)) {
            messages.send(player, "error.guild.tag-regex");
            return;
        }
        if (repository.getGuild(tag) != null) {
            messages.send(player, "guild.exists");
            return;
        }
        String name = nameRaw == null ? "" : nameRaw.trim();
        if (name.isBlank()) {
            messages.send(player, "error.guild.name-empty");
            return;
        }
        int nameMax = config.getInt("guild.name.max", 24);
        if (name.length() > nameMax) {
            messages.send(player, "error.guild.name-length", "max", String.valueOf(nameMax));
            return;
        }
        UUID ownerId = player.getUniqueId();
        Guild guild = new Guild(tag, name, ownerId);
        repository.saveGuild(guild);
        repository.saveMember(new GuildMember(ownerId, tag, rankService.getLeaderId()));
        messages.send(player, "guild.created", "tag", tag, "name", name);
    }

    public void info(Player player, String tagRaw) {
        Guild guild;
        if (tagRaw == null || tagRaw.isBlank()) {
            GuildMember member = repository.getMember(player.getUniqueId());
            if (member == null) {
                messages.send(player, "error.guild.no_member");
                return;
            }
            guild = repository.getGuild(member.tag());
        } else {
            guild = repository.getGuild(tagRaw);
        }
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(guild.owner());
        String ownerName = owner.getName() == null ? guild.owner().toString() : owner.getName();
        messages.send(player, "info.format",
                "tag", guild.tag(),
                "name", guild.name(),
                "owner", ownerName);
    }

    public void disband(Player player) {
        if (!config.getBoolean("features.disband", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.INVITE)) {
            messages.send(player, "error.no_permission");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        if (!guild.owner().equals(player.getUniqueId())) {
            messages.send(player, "error.guild.disband-not-owner");
            return;
        }
        repository.removeGuild(guild.tag());
        repository.removeMembersByTag(guild.tag());
        messages.send(player, "success.disband", "tag", guild.tag());
    }

    public int getCreateCost() {
        return config.getInt("guild.create.cost", 0);
    }

    public void invite(Player player, String targetName) {
        if (!config.getBoolean("features.invites", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            messages.send(player, "error.player_not_found");
            return;
        }
        if (repository.getMember(target.getUniqueId()) != null) {
            messages.send(player, "join.already_in_guild");
            return;
        }
        pruneExpiredInvites();
        int maxActive = config.getInt("invites.maxActiveInvitesPerGuild", 10);
        if (maxActive > 0 && countActiveInvites(guild.tag()) >= maxActive) {
            messages.send(player, "error.no_permission");
            return;
        }
        long expiresAt = System.currentTimeMillis()
                + (long) config.getInt("invites.ttlSeconds", 600) * 1000L;
        invites.put(target.getUniqueId(), new GuildInvite(guild.tag(), player.getUniqueId(), expiresAt));
        messages.send(player, "invite.sent", "player", target.getName());
        messages.send(target, "invite.received",
                "tag", guild.tag(),
                "name", guild.name(),
                "player", player.getName());
    }

    public void join(Player player, String tagRaw) {
        if (!config.getBoolean("features.join", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        if (repository.getMember(player.getUniqueId()) != null) {
            messages.send(player, "join.already_in_guild");
            return;
        }
        Guild guild = repository.getGuild(tagRaw);
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        int maxMembers = config.getInt("guild.members.max", 30);
        if (maxMembers > 0 && repository.countMembersByTag(guild.tag()) >= maxMembers) {
            messages.send(player, "join.denied.full");
            return;
        }
        boolean requireInvite = config.getBoolean("invites.requireInviteToJoin", true);
        if (requireInvite) {
            GuildInvite invite = invites.get(player.getUniqueId());
            if (invite == null) {
                messages.send(player, "invite.none");
                return;
            }
            if (invite.expiresAt() < System.currentTimeMillis()) {
                invites.remove(player.getUniqueId());
                messages.send(player, "invite.expired");
                return;
            }
            if (!invite.tag().equalsIgnoreCase(guild.tag())) {
                messages.send(player, "join.denied.no_invite");
                return;
            }
        }
        repository.saveMember(new GuildMember(player.getUniqueId(), guild.tag(), rankService.getDefaultId()));
        invites.remove(player.getUniqueId());
        messages.send(player, "join.success", "tag", guild.tag(), "name", guild.name());
    }

    public void leave(Player player) {
        if (!config.getBoolean("features.leave", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        if (config.getBoolean("leave.denyIfLeader", true) && guild.owner().equals(player.getUniqueId())) {
            messages.send(player, "leave.denied.leader");
            return;
        }
        int cooldownSeconds = config.getInt("leave.cooldownSeconds", 300);
        long now = System.currentTimeMillis();
        Long lastLeave = leaveCooldowns.get(player.getUniqueId());
        if (cooldownSeconds > 0 && lastLeave != null) {
            long elapsed = now - lastLeave;
            long cooldownMs = cooldownSeconds * 1000L;
            if (elapsed < cooldownMs) {
                long remaining = (cooldownMs - elapsed + 999) / 1000;
                messages.send(player, "leave.denied.cooldown", "seconds", String.valueOf(remaining));
                return;
            }
        }
        repository.removeMember(player.getUniqueId());
        leaveCooldowns.put(player.getUniqueId(), now);
        messages.send(player, "leave.success", "tag", guild.tag());
    }

    public void listRanks(Player player) {
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        StringBuilder list = new StringBuilder();
        for (GuildRankService.GuildRank rank : rankService.getRanksByPriority()) {
            if (list.length() > 0) {
                list.append("\n");
            }
            String name = getRankDisplayName(rank);
            list.append("&7- &f").append(name).append(" &8(").append(rank.id()).append(")");
        }
        messages.send(player, "rank.list.header", "list", list.toString());
    }

    public void setRank(Player player, String targetName, String rankIdRaw) {
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.PROMOTE)) {
            messages.send(player, "rank.set.denied.no_permission_flag");
            return;
        }
        if (targetName == null || targetName.isBlank()) {
            messages.send(player, "error.player_not_found");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            messages.send(player, "error.player_not_found");
            return;
        }
        GuildMember targetMember = repository.getMember(target.getUniqueId());
        if (targetMember == null || !guild.tag().equalsIgnoreCase(targetMember.tag())) {
            messages.send(player, "error.player_not_found");
            return;
        }
        GuildRankService.GuildRank desiredRank = rankService.getRank(rankIdRaw);
        if (desiredRank == null) {
            messages.send(player, "rank.not_found");
            return;
        }
        GuildRankService.GuildRank actorRank = getRank(member);
        GuildRankService.GuildRank targetRank = getRank(targetMember);
        if (actorRank == null || targetRank == null) {
            messages.send(player, "rank.set.denied.no_permission_flag");
            return;
        }
        if (targetRank.priority() >= actorRank.priority()
                || desiredRank.priority() >= actorRank.priority()) {
            messages.send(player, "rank.set.denied.higher_or_equal");
            return;
        }
        if (!config.getBoolean("ranks.allowMultipleLeaders", false)
                && desiredRank.id().equals(rankService.getLeaderId())
                && !target.getUniqueId().equals(guild.owner())) {
            messages.send(player, "rank.set.denied.higher_or_equal");
            return;
        }
        repository.saveMember(new GuildMember(target.getUniqueId(), guild.tag(), desiredRank.id()));
        messages.send(player, "rank.set.success",
                "player", target.getName() == null ? target.getUniqueId().toString() : target.getName(),
                "rank", getRankDisplayName(desiredRank));
    }

    private void pruneExpiredInvites() {
        long now = System.currentTimeMillis();
        invites.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    private int countActiveInvites(String tag) {
        int count = 0;
        for (GuildInvite invite : invites.values()) {
            if (invite.tag().equalsIgnoreCase(tag) && invite.expiresAt() >= System.currentTimeMillis()) {
                count++;
            }
        }
        return count;
    }

    private record GuildInvite(String tag, UUID inviter, long expiresAt) {
    }

    private GuildRankService.GuildRank getRank(GuildMember member) {
        if (member == null) {
            return null;
        }
        String rankId = rankService.resolveRankId(member.role());
        return rankService.getRank(rankId);
    }

    private String getRankDisplayName(GuildRankService.GuildRank rank) {
        if (rank == null) {
            return "";
        }
        String name = messages.raw(rank.displayNameKey());
        if (name == null || name.isBlank()) {
            return rank.id();
        }
        return name;
    }
}
