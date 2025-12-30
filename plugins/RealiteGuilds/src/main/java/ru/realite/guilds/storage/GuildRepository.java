package ru.realite.guilds.storage;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;

public final class GuildRepository {

    private final JavaPlugin plugin;
    private final File guildsFile;
    private final File membersFile;
    private final Map<String, Guild> guilds = new HashMap<>();
    private final Map<UUID, GuildMember> members = new HashMap<>();

    public GuildRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.guildsFile = new File(plugin.getDataFolder(), "guilds.yml");
        this.membersFile = new File(plugin.getDataFolder(), "members.yml");
        load();
    }

    public void load() {
        guilds.clear();
        members.clear();
        if (guildsFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(guildsFile);
            ConfigurationSection section = config.getConfigurationSection("guilds");
            if (section != null) {
                for (String tagKey : section.getKeys(false)) {
                    ConfigurationSection guildSection = section.getConfigurationSection(tagKey);
                    if (guildSection == null) {
                        continue;
                    }
                    String name = guildSection.getString("name", tagKey);
                    String ownerRaw = guildSection.getString("owner", "");
                    if (ownerRaw == null || ownerRaw.isBlank()) {
                        continue;
                    }
                    UUID owner = UUID.fromString(ownerRaw);
                    String normalizedTag = normalizeTag(tagKey);
                    guilds.put(normalizedTag, new Guild(normalizedTag, name, owner));
                }
            }
        }
        if (membersFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(membersFile);
            ConfigurationSection section = config.getConfigurationSection("members");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection memberSection = section.getConfigurationSection(key);
                    if (memberSection == null) {
                        continue;
                    }
                    String tag = memberSection.getString("tag", "");
                    String role = memberSection.getString("role", "member");
                    if (tag == null || tag.isBlank()) {
                        continue;
                    }
                    UUID uuid = UUID.fromString(key);
                    members.put(uuid, new GuildMember(uuid, normalizeTag(tag), role));
                }
            }
        }
    }

    public Guild getGuild(String tag) {
        return guilds.get(normalizeTag(tag));
    }

    public GuildMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public Collection<Guild> getGuilds() {
        return guilds.values();
    }

    public void saveGuild(Guild guild) {
        guilds.put(normalizeTag(guild.tag()), guild);
        saveGuilds();
    }

    public void saveMember(GuildMember member) {
        members.put(member.uuid(), member);
        saveMembers();
    }

    public void removeGuild(String tag) {
        guilds.remove(normalizeTag(tag));
        saveGuilds();
    }

    public void removeMembersByTag(String tag) {
        String normalized = normalizeTag(tag);
        members.entrySet().removeIf(entry -> normalized.equals(entry.getValue().tag()));
        saveMembers();
    }

    public void removeMember(UUID uuid) {
        if (members.remove(uuid) != null) {
            saveMembers();
        }
    }

    public int countMembersByTag(String tag) {
        String normalized = normalizeTag(tag);
        int count = 0;
        for (GuildMember member : members.values()) {
            if (normalized.equals(member.tag())) {
                count++;
            }
        }
        return count;
    }

    private void saveGuilds() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("guilds");
        for (Guild guild : guilds.values()) {
            ConfigurationSection guildSection = section.createSection(guild.tag());
            guildSection.set("name", guild.name());
            guildSection.set("owner", guild.owner().toString());
        }
        save(config, guildsFile, "guilds.yml");
    }

    private void saveMembers() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("members");
        for (GuildMember member : members.values()) {
            ConfigurationSection memberSection = section.createSection(member.uuid().toString());
            memberSection.set("tag", member.tag());
            memberSection.set("role", member.role());
        }
        save(config, membersFile, "members.yml");
    }

    private void save(YamlConfiguration config, File file, String name) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + name + ": " + e.getMessage());
        }
    }

    private String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        return tag.trim().toUpperCase(Locale.ROOT);
    }
}
