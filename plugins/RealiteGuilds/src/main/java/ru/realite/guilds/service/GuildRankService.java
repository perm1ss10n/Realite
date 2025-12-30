package ru.realite.guilds.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuildRankService {

    private final JavaPlugin plugin;
    private final Map<String, GuildRank> ranks = new HashMap<>();
    private String leaderId = "leader";
    private String defaultId = "member";

    public GuildRankService(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        ranks.clear();
        File file = new File(plugin.getDataFolder(), "ranks.yml");
        if (!file.exists()) {
            plugin.saveResource("ranks.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        leaderId = normalizeId(config.getString("leaderId", "leader"));
        defaultId = normalizeId(config.getString("defaultId", "member"));
        ConfigurationSection ranksSection = config.getConfigurationSection("ranks");
        if (ranksSection == null) {
            return;
        }
        for (String key : ranksSection.getKeys(false)) {
            ConfigurationSection rankSection = ranksSection.getConfigurationSection(key);
            if (rankSection == null) {
                continue;
            }
            String id = normalizeId(key);
            String displayNameKey = rankSection.getString("displayNameKey", "rank.name." + id);
            int priority = rankSection.getInt("priority", 0);
            int salaryDaily = rankSection.getInt("salaryDaily", 0);
            Set<GuildRankPermission> permissions = new HashSet<>();
            for (String raw : rankSection.getStringList("permissions")) {
                GuildRankPermission.fromString(raw).ifPresentOrElse(
                        permissions::add,
                        () -> plugin.getLogger().warning("Unknown rank permission: " + raw));
            }
            ranks.put(id, new GuildRank(id, displayNameKey, priority, salaryDaily,
                    Collections.unmodifiableSet(permissions)));
        }
    }

    public String getLeaderId() {
        return leaderId;
    }

    public String getDefaultId() {
        return defaultId;
    }

    public GuildRank getRank(String rankId) {
        return ranks.get(normalizeId(rankId));
    }

    public List<GuildRank> getRanksByPriority() {
        List<GuildRank> list = new ArrayList<>(ranks.values());
        list.sort((left, right) -> Integer.compare(right.priority(), left.priority()));
        return list;
    }

    public String resolveRankId(String rankId) {
        if (rankId == null || rankId.isBlank()) {
            return defaultId;
        }
        String normalized = normalizeId(rankId);
        if (ranks.containsKey(normalized)) {
            return normalized;
        }
        if ("owner".equals(normalized) && ranks.containsKey(leaderId)) {
            return leaderId;
        }
        return defaultId;
    }

    public boolean hasPermission(String rankId, GuildRankPermission permission) {
        GuildRank rank = getRank(resolveRankId(rankId));
        if (rank == null || permission == null) {
            return false;
        }
        return rank.permissions().contains(permission);
    }

    private String normalizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }

    public record GuildRank(
            String id,
            String displayNameKey,
            int priority,
            int salaryDaily,
            Set<GuildRankPermission> permissions) {
    }
}
