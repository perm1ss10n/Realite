package ru.realite.guilds.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildChatService {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final GuildRepository repository;
    private final GuildMessages messages;
    private final GuildRankService rankService;
    private final Set<UUID> toggled = new HashSet<>();

    public GuildChatService(JavaPlugin plugin, FileConfiguration config, GuildRepository repository,
                            GuildMessages messages, GuildRankService rankService) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.messages = messages;
        this.rankService = rankService;
    }

    public boolean isGuildChatEnabled() {
        return config.getBoolean("chat.guild.enabled", true);
    }

    public boolean isToggleCommandEnabled() {
        return config.getBoolean("chat.guild.toggleCommand", true);
    }

    public boolean isPrefixEnabled() {
        return config.getBoolean("chat.prefix.enabled", true);
    }

    public boolean isHoverEnabled() {
        return config.getBoolean("chat.prefix.hover.enabled", true);
    }

    public boolean isShowRankEnabled() {
        return config.getBoolean("chat.prefix.showRank", true);
    }

    public boolean isSpyEnabled() {
        return config.getBoolean("chat.guild.spy.enabled", true);
    }

    public String getSpyPermission() {
        return config.getString("chat.guild.spy.permission", "realite.guilds.chat.spy");
    }

    public boolean isToggled(Player player) {
        return toggled.contains(player.getUniqueId());
    }

    public void clearToggle(Player player) {
        toggled.remove(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (!isGuildChatEnabled() || !isToggleCommandEnabled()) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (toggled.contains(player.getUniqueId())) {
            toggled.remove(player.getUniqueId());
            messages.send(player, "chat.toggle.off");
        } else {
            toggled.add(player.getUniqueId());
            messages.send(player, "chat.toggle.on");
        }
    }

    public void sendGuildChat(Player sender, Component message) {
        if (!isGuildChatEnabled()) {
            messages.send(sender, "error.no_permission");
            return;
        }
        GuildMember member = repository.getMember(sender.getUniqueId());
        if (member == null) {
            messages.send(sender, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(sender, "guild.not_found");
            return;
        }
        Component formatted = formatGuildMessage(sender, guild, member, message);
        String spyPermission = getSpyPermission();
        boolean spyEnabled = isSpyEnabled() && spyPermission != null && !spyPermission.isBlank();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            GuildMember viewerMember = repository.getMember(viewer.getUniqueId());
            boolean sameGuild = viewerMember != null && member.tag().equalsIgnoreCase(viewerMember.tag());
            boolean spy = spyEnabled && viewer.hasPermission(spyPermission);
            if (sameGuild || spy) {
                viewer.sendMessage(formatted);
            }
        }
    }

    public void sendGuildChatAsync(Player sender, Component message) {
        Bukkit.getScheduler().runTask(plugin, () -> sendGuildChat(sender, message));
    }

    public Component buildPublicPrefix(Player player) {
        return buildPublicPrefix(player, true);
    }

    public Component buildPublicPrefix(Player player, boolean includeHover) {
        if (!isPrefixEnabled()) {
            return Component.empty();
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            return Component.empty();
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            return Component.empty();
        }
        String prefixRaw = messages.raw("chat.public.prefix");
        prefixRaw = applyPlaceholders(prefixRaw, player, guild, member);
        Component prefix = LEGACY.deserialize(prefixRaw);
        if (includeHover && isHoverEnabled()) {
            Component hover = buildHover(player, guild, member);
            prefix = prefix.hoverEvent(HoverEvent.showText(hover));
        }
        return prefix;
    }

    public Component buildHover(Player player, Guild guild, GuildMember member) {
        if (!isHoverEnabled()) {
            return Component.empty();
        }
        String hoverRaw = messages.raw("chat.hover");
        boolean showRank = isShowRankEnabled();
        boolean showToggle = isToggleCommandEnabled();
        boolean showChat = isGuildChatEnabled();
        hoverRaw = filterHoverLines(hoverRaw, showRank, showToggle, showChat);
        hoverRaw = applyPlaceholders(hoverRaw, player, guild, member);
        hoverRaw = hoverRaw.replace("{hintToggle}", "/g chat toggle");
        hoverRaw = hoverRaw.replace("{hintChat}", "/gc <msg>");
        return LEGACY.deserialize(hoverRaw);
    }

    public Component buildHover(Player player) {
        if (!isHoverEnabled()) {
            return Component.empty();
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            return Component.empty();
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            return Component.empty();
        }
        return buildHover(player, guild, member);
    }

    public Component formatGuildMessage(Player player, Guild guild, GuildMember member, Component message) {
        String raw = messages.raw("chat.guild.format");
        raw = applyPlaceholders(raw, player, guild, member);
        List<String> parts = splitMessageFormat(raw);
        Component result = LEGACY.deserialize(parts.get(0));
        if (parts.size() == 1) {
            return result.append(message);
        }
        for (int i = 1; i < parts.size(); i++) {
            result = result.append(message).append(LEGACY.deserialize(parts.get(i)));
        }
        return result;
    }

    private String applyPlaceholders(String raw, Player player, Guild guild, GuildMember member) {
        String replaced = raw;
        replaced = replaced.replace("{player}", player.getName());
        replaced = replaced.replace("{guild}", guild.name());
        replaced = replaced.replace("{tag}", guild.tag());
        replaced = replaced.replace("{members}", String.valueOf(repository.countMembersByTag(guild.tag())));
        replaced = replaced.replace("{rank}", resolveRankName(member));
        return replaced;
    }

    private String resolveRankName(GuildMember member) {
        GuildRankService.GuildRank rank = rankService.getRank(member.role());
        if (rank == null) {
            return member.role();
        }
        String raw = messages.raw(rank.displayNameKey());
        if (raw == null || raw.isBlank()) {
            return rank.id();
        }
        return raw;
    }

    private String filterHoverLines(String hoverRaw, boolean showRank, boolean showToggle, boolean showChat) {
        String[] lines = hoverRaw.split("\\\\n", -1);
        List<String> filtered = new ArrayList<>();
        for (String line : lines) {
            if (!showRank && line.contains("{rank}")) {
                continue;
            }
            if (!showToggle && line.contains("{hintToggle}")) {
                continue;
            }
            if (!showChat && line.contains("{hintChat}")) {
                continue;
            }
            filtered.add(line);
        }
        return String.join("\n", filtered);
    }

    private List<String> splitMessageFormat(String raw) {
        String token = "{message}";
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (true) {
            int next = raw.indexOf(token, index);
            if (next < 0) {
                parts.add(raw.substring(index));
                break;
            }
            parts.add(raw.substring(index, next));
            index = next + token.length();
        }
        if (parts.isEmpty()) {
            parts.add(raw);
        }
        return parts;
    }
}
