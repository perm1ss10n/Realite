package ru.realite.guilds.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildClaim;
import ru.realite.guilds.model.GuildHome;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildService {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final GuildRepository repository;
    private final GuildMessages messages;
    private final GuildRankService rankService;
    private final Map<UUID, GuildInvite> invites = new HashMap<>();
    private final Map<UUID, Long> leaveCooldowns = new HashMap<>();
    private final Map<UUID, ClaimSelection> claimSelections = new HashMap<>();
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<>();

    public GuildService(JavaPlugin plugin, FileConfiguration config, GuildRepository repository, GuildMessages messages,
                        GuildRankService rankService) {
        this.plugin = plugin;
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
        Guild guild = new Guild(tag, name, ownerId, null, null);
        repository.saveGuild(guild);
        repository.saveMember(new GuildMember(ownerId, tag, rankService.getLeaderId()));
        messages.send(player, "guild.created", "tag", tag, "name", name);
    }

    public void setHome(Player player) {
        if (!config.getBoolean("home.enabled", true)) {
            messages.send(player, "home.denied");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.SETHOME)) {
            messages.send(player, "home.denied");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        GuildHome home = GuildHome.fromLocation(player.getLocation());
        repository.saveGuild(new Guild(guild.tag(), guild.name(), guild.owner(), home, guild.claim()));
        messages.send(player, "home.set");
    }

    public void teleportHome(Player player) {
        if (!config.getBoolean("home.enabled", true)) {
            messages.send(player, "home.denied");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!hasHomePermission(member)) {
            messages.send(player, "home.denied");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        GuildHome home = guild.home();
        if (home == null) {
            messages.send(player, "home.not_set");
            return;
        }
        World world = Bukkit.getWorld(home.world());
        if (world == null) {
            messages.send(player, "home.not_set");
            return;
        }
        Location target = new Location(world, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
        startTeleport(player, target);
    }

    public void setClaimPos(Player player, boolean first) {
        if (!config.getBoolean("claim.enabled", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.CLAIM)) {
            messages.send(player, "error.no_permission");
            return;
        }
        ClaimSelection selection = claimSelections.computeIfAbsent(player.getUniqueId(), key -> new ClaimSelection());
        if (first) {
            selection.pos1 = player.getLocation();
            messages.send(player, "claim.pos1", "loc", formatLocation(selection.pos1));
        } else {
            selection.pos2 = player.getLocation();
            messages.send(player, "claim.pos2", "loc", formatLocation(selection.pos2));
        }
    }

    public void applyClaim(Player player) {
        if (!config.getBoolean("claim.enabled", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.CLAIM)) {
            messages.send(player, "error.no_permission");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        ClaimSelection selection = claimSelections.get(player.getUniqueId());
        if (selection == null || selection.pos1 == null || selection.pos2 == null) {
            messages.send(player, "claim.apply.denied.size");
            return;
        }
        Location pos1 = selection.pos1;
        Location pos2 = selection.pos2;
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            messages.send(player, "claim.apply.denied.size");
            return;
        }
        if (!pos1.getWorld().getName().equalsIgnoreCase(pos2.getWorld().getName())) {
            messages.send(player, "claim.apply.denied.size");
            return;
        }
        if (!isWorldAllowed(pos1.getWorld().getName())) {
            messages.send(player, "claim.apply.denied.size");
            return;
        }
        int sizeX = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
        int sizeY = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
        int sizeZ = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
        int maxX = config.getInt("claim.maxSize.x", 120);
        int maxY = config.getInt("claim.maxSize.y", 80);
        int maxZ = config.getInt("claim.maxSize.z", 120);
        if (sizeX > maxX || sizeY > maxY || sizeZ > maxZ) {
            messages.send(player, "claim.apply.denied.size",
                    "x", String.valueOf(sizeX),
                    "y", String.valueOf(sizeY),
                    "z", String.valueOf(sizeZ));
            return;
        }
        GuildClaim claim = new GuildClaim(
                pos1.getWorld().getName(),
                pos1.getBlockX(),
                pos1.getBlockY(),
                pos1.getBlockZ(),
                pos2.getBlockX(),
                pos2.getBlockY(),
                pos2.getBlockZ());
        repository.saveGuild(new Guild(guild.tag(), guild.name(), guild.owner(), guild.home(), claim));
        messages.send(player, "claim.apply.success");
    }

    public void clearClaim(Player player) {
        if (!config.getBoolean("claim.enabled", true)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.CLAIM)) {
            messages.send(player, "error.no_permission");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "guild.not_found");
            return;
        }
        repository.saveGuild(new Guild(guild.tag(), guild.name(), guild.owner(), guild.home(), null));
        messages.send(player, "claim.clear");
    }

    public boolean handleTeleportMove(Player player, Location from, Location to) {
        if (!config.getBoolean("home.cancelOnMove", true)) {
            return false;
        }
        if (to == null || from == null) {
            return false;
        }
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()) {
            cancelTeleport(player, pending);
            return true;
        }
        return false;
    }

    public boolean handleTeleportDamage(Player player) {
        if (!config.getBoolean("home.cancelOnDamage", true)) {
            return false;
        }
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        cancelTeleport(player, pending);
        return true;
    }

    public GuildClaim findClaim(Location location) {
        if (location == null) {
            return null;
        }
        for (Guild guild : repository.getGuilds()) {
            GuildClaim claim = guild.claim();
            if (claim != null && claim.contains(location)) {
                return claim;
            }
        }
        return null;
    }

    public Guild findGuildByClaim(Location location) {
        if (location == null) {
            return null;
        }
        for (Guild guild : repository.getGuilds()) {
            GuildClaim claim = guild.claim();
            if (claim != null && claim.contains(location)) {
                return guild;
            }
        }
        return null;
    }

    public boolean canAccessClaim(Player player, Guild guild) {
        if (player == null || guild == null) {
            return false;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null || !guild.tag().equalsIgnoreCase(member.tag())) {
            return false;
        }
        if (!config.getBoolean("access.protect.requirePermissionFlag", true)) {
            return true;
        }
        return rankService.hasPermission(member.role(), GuildRankPermission.ACCESS);
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

    private record PendingTeleport(Location origin, Location target, int taskId) {
    }

    private static final class ClaimSelection {
        private Location pos1;
        private Location pos2;
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

    private boolean hasHomePermission(GuildMember member) {
        String flag = config.getString("home.requiresPermissionFlag", "");
        if (flag == null || flag.isBlank()) {
            return true;
        }
        return GuildRankPermission.fromString(flag)
                .map(permission -> rankService.hasPermission(member.role(), permission))
                .orElse(true);
    }

    private boolean isWorldAllowed(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return false;
        }
        if (config.getStringList("claim.blockedWorlds").stream()
                .anyMatch(name -> name.equalsIgnoreCase(worldName))) {
            return false;
        }
        var allowed = config.getStringList("claim.allowInWorlds");
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        return allowed.stream().anyMatch(name -> name.equalsIgnoreCase(worldName));
    }

    private void startTeleport(Player player, Location target) {
        int warmupSeconds = config.getInt("home.teleportWarmupSeconds", 3);
        if (warmupSeconds <= 0) {
            player.teleport(target);
            messages.send(player, "home.tp.success");
            return;
        }
        PendingTeleport existing = pendingTeleports.remove(player.getUniqueId());
        if (existing != null) {
            plugin.getServer().getScheduler().cancelTask(existing.taskId());
        }
        messages.send(player, "home.tp.start", "seconds", String.valueOf(warmupSeconds));
        Location origin = player.getLocation().clone();
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            PendingTeleport current = pendingTeleports.remove(player.getUniqueId());
            if (current == null) {
                return;
            }
            player.teleport(target);
            messages.send(player, "home.tp.success");
        }, warmupSeconds * 20L).getTaskId();
        pendingTeleports.put(player.getUniqueId(), new PendingTeleport(origin, target, taskId));
    }

    private void cancelTeleport(Player player, PendingTeleport pending) {
        plugin.getServer().getScheduler().cancelTask(pending.taskId());
        pendingTeleports.remove(player.getUniqueId());
        messages.send(player, "home.tp.cancelled");
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getName()
                + " [" + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ() + "]";
    }
}
