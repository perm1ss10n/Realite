package ru.realite.guilds.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
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
    private boolean guildChatEnabled;

    // Vault Chat (LuckPerms умеет отдавать префиксы/группы через Vault hook)
    private Chat vaultChat;
    private boolean vaultChatResolved;

    public GuildChatService(JavaPlugin plugin, FileConfiguration config, GuildRepository repository,
            GuildMessages messages, GuildRankService rankService) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.messages = messages;
        this.rankService = rankService;
        this.guildChatEnabled = config.getBoolean("chat.guild.enabled", true);
    }

    public boolean isGuildChatEnabled() {
        return guildChatEnabled;
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

    // --- Toggle state (server-level) ---

    public boolean isMember(Player player) {
        return repository.getMember(player.getUniqueId()) != null;
    }

    /**
     * Toggle гильд-чата на уровне сервера.
     * Возвращает true если включили, false если выключили.
     */
    public boolean toggleEnabled() {
        guildChatEnabled = !guildChatEnabled;
        config.set("chat.guild.enabled", guildChatEnabled);
        plugin.saveConfig();
        return guildChatEnabled;
    }

    // --- Recipients helpers (новое, для будущего bridge) ---

    /**
     * Онлайн-участники гильдии отправителя.
     * Не включает spy (spy отдельно).
     */
    public List<Player> getGuildRecipients(Player sender) {
        GuildMember member = repository.getMember(sender.getUniqueId());
        if (member == null)
            return List.of();

        String tag = member.tag();
        if (tag == null || tag.isBlank())
            return List.of();

        List<Player> recipients = new ArrayList<>();
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            GuildMember viewerMember = repository.getMember(viewer.getUniqueId());
            if (viewerMember != null && tag.equalsIgnoreCase(viewerMember.tag())) {
                recipients.add(viewer);
            }
        }
        return recipients;
    }

    /**
     * Онлайн-игроки со spy правом.
     */
    public List<Player> getSpyRecipients(Player sender) {
        String perm = getSpyPermission();
        if (!isSpyEnabled() || perm == null || perm.isBlank())
            return List.of();

        List<Player> spies = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(perm)) {
                spies.add(p);
            }
        }
        return spies;
    }

    // --- Sending ---

    public void sendGuildChat(Player sender, Component message) {
        if (!isGuildChatEnabled()) {
            messages.send(sender, "error.no_permission");
            return;
        }

        Component formatted = format(sender, message);
        if (formatted.equals(Component.empty())) {
            return;
        }

        // 1) Члены гильдии
        List<Player> guildRecipients = getGuildRecipients(sender);
        for (Player p : guildRecipients) {
            p.sendMessage(formatted);
        }

        // 2) Spy (тем, кто не в guildRecipients)
        List<Player> spies = getSpyRecipients(sender);
        if (!spies.isEmpty()) {
            Set<UUID> already = new HashSet<>();
            for (Player p : guildRecipients) {
                already.add(p.getUniqueId());
            }

            for (Player spy : spies) {
                if (!already.contains(spy.getUniqueId())) {
                    spy.sendMessage(formatted);
                }
            }
        }
    }

    public void sendGuildChatAsync(Player sender, Component message) {
        Bukkit.getScheduler().runTask(plugin, () -> sendGuildChat(sender, message));
    }

    // --- Public prefix / hover ---

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
            if (!hover.equals(Component.empty())) {
                prefix = prefix.hoverEvent(HoverEvent.showText(hover));
            }
        }
        return prefix;
    }

    public Component buildHover(Player player, Guild guild, GuildMember member) {
        if (!isHoverEnabled()) {
            return Component.empty();
        }
        String hoverRaw = messages.raw("chat.hover");

        boolean showRank = isShowRankEnabled();
        boolean showChat = isGuildChatEnabled();

        boolean showToggle = isToggleCommandEnabled() && canAdminToggle(player);

        hoverRaw = filterHoverLines(hoverRaw, showRank, showToggle, showChat);
        hoverRaw = applyPlaceholders(hoverRaw, player, guild, member);
        hoverRaw = hoverRaw.replace("{hintToggle}", "/g");
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

    public Component format(Player sender, Component message) {
        if (!isGuildChatEnabled()) {
            messages.send(sender, "error.no_permission");
            return Component.empty();
        }

        GuildMember member = repository.getMember(sender.getUniqueId());
        if (member == null) {
            messages.send(sender, "error.guild.no_member");
            return Component.empty();
        }

        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(sender, "guild.not_found");
            return Component.empty();
        }

        return formatGuildMessage(sender, guild, member, message);
    }

    /**
     * Формат гильдейского чата:
     * в messages/config делай так, как хочешь, например:
     * "&7[&aG&7] {lpPrefix}&r{group} &f{rank} &f{player}: &r{message}"
     */
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

        // player/guild placeholders
        replaced = replaced.replace("{player}", player.getName());
        replaced = replaced.replace("{guild}", guild.name());
        replaced = replaced.replace("{tag}", guild.tag());
        replaced = replaced.replace("{members}", String.valueOf(repository.countMembersByTag(guild.tag())));
        replaced = replaced.replace("{rank}", resolveRankName(member));

        // LuckPerms meta через Vault Chat (если доступно)
        replaced = replaced.replace("{lpPrefix}", safe(resolveVaultPrefix(player)));
        replaced = replaced.replace("{group}", safe(resolveVaultPrimaryGroup(player)));

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
        String[] lines = hoverRaw.split("\\n", -1);
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

    public boolean canAdminToggle(Player player) {
        return player.isOp() || player.hasPermission("realite.guilds.chat.toggle");
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

    private void resolveVaultChat() {
        if (vaultChatResolved)
            return;
        vaultChatResolved = true;

        try {
            RegisteredServiceProvider<Chat> reg = Bukkit.getServicesManager().getRegistration(Chat.class);
            if (reg != null) {
                vaultChat = reg.getProvider();
            }
        } catch (Throwable ignored) {
            vaultChat = null;
        }
    }

    private String resolveVaultPrefix(Player player) {
        resolveVaultChat();
        if (vaultChat == null)
            return "";
        try {
            // совместимо со старыми сигнатурами Vault
            return vaultChat.getPlayerPrefix(player.getWorld(), player.getName());
        } catch (Throwable t) {
            return "";
        }
    }

    private String resolveVaultPrimaryGroup(Player player) {
        resolveVaultChat();
        if (vaultChat == null)
            return "";
        try {
            return vaultChat.getPrimaryGroup(player.getWorld(), player.getName());
        } catch (Throwable t) {
            return "";
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }
}
