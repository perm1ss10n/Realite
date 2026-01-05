package ru.realite.guilds.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

/**
 * GuildChatService (после рефактора под схему):
 * - RealiteGuilds отвечает ТОЛЬКО за доменную часть: включено ли, участник ли,
 * получатели (guild + spy)
 * - НИКАКОГО форматирования и отправки сообщений здесь нет (это делает
 * RealiteChat)
 * - Дополнительно: умеет отдавать отображаемый ранг игрока в гильдии (для
 * RealiteChat.format)
 */
public final class GuildChatService {

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final GuildRepository repository;

    // теперь реально используются (ранг для отображения)
    private final GuildMessages messages;
    private final GuildRankService rankService;

    private boolean guildChatEnabled;

    public GuildChatService(
            JavaPlugin plugin,
            FileConfiguration config,
            GuildRepository repository,
            GuildMessages messages,
            GuildRankService rankService) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.messages = messages;
        this.rankService = rankService;
        this.guildChatEnabled = config.getBoolean("chat.guild.enabled", true);
    }

    // --- Flags / config ---

    public boolean isGuildChatEnabled() {
        return guildChatEnabled;
    }

    public boolean isToggleCommandEnabled() {
        return config.getBoolean("chat.guild.toggleCommand", true);
    }

    public boolean isSpyEnabled() {
        return config.getBoolean("chat.guild.spy.enabled", true);
    }

    public String getSpyPermission() {
        return config.getString("chat.guild.spy.permission", "realite.guilds.chat.spy");
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

    // --- Domain checks ---

    public boolean isMember(Player player) {
        return repository.getMember(player.getUniqueId()) != null;
    }

    public boolean canAdminToggle(Player player) {
        return player.isOp() || player.hasPermission("realite.guilds.chat.toggle");
    }

    /**
     * Отображаемое имя ранга игрока в гильдии (локализованное).
     * Возвращает пустую строку, если игрок не в гильдии.
     */
    public String getGuildRankDisplay(Player player) {
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            return "";
        }

        GuildRankService.GuildRank rank = rankService.getRank(member.role());
        if (rank == null) {
            return safe(member.role());
        }

        String raw = messages.raw(rank.displayNameKey());
        if (raw == null || raw.isBlank()) {
            return safe(rank.id());
        }

        return raw;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // --- Recipients ---

    /**
     * Онлайн-участники гильдии отправителя.
     * Не включает spy (spy отдельно).
     */
    public List<Player> getGuildRecipients(Player sender) {
        GuildMember member = repository.getMember(sender.getUniqueId());
        if (member == null) {
            return List.of();
        }

        String tag = member.tag();
        if (tag == null || tag.isBlank()) {
            return List.of();
        }

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
     *
     * ВАЖНО: возвращает уже "чистый" список — без пересечения с
     * getGuildRecipients(sender),
     * чтобы в RealiteChat можно было отправлять в guild + spy без дублей.
     */
    public List<Player> getSpyRecipients(Player sender) {
        String perm = getSpyPermission();
        if (!isSpyEnabled() || perm == null || perm.isBlank()) {
            return List.of();
        }

        // исключаем участников гильдии отправителя, чтобы не получить двойную доставку
        Set<UUID> guildIds = new HashSet<>();
        for (Player p : getGuildRecipients(sender)) {
            guildIds.add(p.getUniqueId());
        }

        List<Player> spies = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!guildIds.contains(p.getUniqueId()) && p.hasPermission(perm)) {
                spies.add(p);
            }
        }
        return spies;
    }
}
