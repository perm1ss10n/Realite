package ru.realite.quests.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.core.api.quests.QuestState;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class QuestProgressRepository {

    private final File playersDir;

    public QuestProgressRepository(Path dataFolder) {
        this.playersDir = dataFolder.resolve("playerdata").toFile();
        if (!playersDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            playersDir.mkdirs();
        }
    }

    public QuestProgressData getProgress(UUID playerUuid, String questId) {
        YamlConfiguration yml = load(playerUuid);
        String basePath = path(questId);
        if (!yml.contains(basePath)) {
            return null;
        }
        String stateRaw = yml.getString(basePath + ".state", QuestState.ACTIVE.name());
        QuestState state = parseState(stateRaw);
        Set<String> completed = new HashSet<>(yml.getStringList(basePath + ".completed"));
        Map<String, Integer> counts = new HashMap<>();
        ConfigurationSection section = yml.getConfigurationSection(basePath + ".counts");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                counts.put(key, section.getInt(key, 0));
            }
        }
        return new QuestProgressData(state, completed, counts);
    }

    public void save(UUID playerUuid, String questId, QuestProgressData progress) {
        YamlConfiguration yml = load(playerUuid);
        String basePath = path(questId);
        yml.set(basePath + ".state", progress.state().name());
        yml.set(basePath + ".completed", new java.util.ArrayList<>(progress.completedObjectives()));
        Map<String, Integer> counts = progress.objectiveCounts();
        String countsPath = basePath + ".counts";
        yml.set(countsPath, null);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            yml.set(countsPath + "." + entry.getKey(), entry.getValue());
        }
        save(playerUuid, yml);
    }

    public void reset(UUID playerUuid, String questId) {
        YamlConfiguration yml = load(playerUuid);
        yml.set(path(questId), null);
        save(playerUuid, yml);
    }

    private QuestState parseState(String raw) {
        if (raw == null) {
            return QuestState.ACTIVE;
        }
        try {
            return QuestState.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return QuestState.ACTIVE;
        }
    }

    private YamlConfiguration load(UUID uuid) {
        File file = file(uuid);
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(UUID uuid, YamlConfiguration yml) {
        try {
            yml.save(file(uuid));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save quest progress for " + uuid, e);
        }
    }

    private File file(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".yml");
    }

    private String path(String questId) {
        return "quests." + questId;
    }
}
