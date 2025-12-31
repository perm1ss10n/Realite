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

    private static final int CURRENT_DATA_VERSION = 2;
    private static final String DATA_VERSION_KEY = "dataVersion";
    private static final String QUESTS_KEY = "quests";

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
        boolean rewardGranted = yml.getBoolean(basePath + ".rewarded", false);
        Set<String> completed = new HashSet<>(yml.getStringList(basePath + ".completed"));
        Map<String, Integer> counts = new HashMap<>();
        ConfigurationSection section = yml.getConfigurationSection(basePath + ".counts");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                counts.put(key, section.getInt(key, 0));
            }
        }
        return new QuestProgressData(state, rewardGranted, completed, counts);
    }

    public void save(UUID playerUuid, String questId, QuestProgressData progress) {
        YamlConfiguration yml = load(playerUuid);
        String basePath = path(questId);
        yml.set(basePath + ".state", progress.state().name());
        yml.set(basePath + ".rewarded", progress.rewardGranted());
        yml.set(basePath + ".completed", new java.util.ArrayList<>(progress.completedObjectives()));
        Map<String, Integer> counts = progress.objectiveCounts();
        String countsPath = basePath + ".counts";
        yml.set(countsPath, null);
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            yml.set(countsPath + "." + entry.getKey(), entry.getValue());
        }
        save(playerUuid, yml);
    }

    public Set<String> getActiveQuestIds(UUID playerUuid) {
        YamlConfiguration yml = load(playerUuid);
        ConfigurationSection section = yml.getConfigurationSection(QUESTS_KEY);
        if (section == null) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (String questId : section.getKeys(false)) {
            String stateRaw = yml.getString(path(questId) + ".state", QuestState.ACTIVE.name());
            QuestState state = parseState(stateRaw);
            if (state == QuestState.ACTIVE) {
                result.add(questId);
            }
        }
        return result;
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
            YamlConfiguration yml = new YamlConfiguration();
            yml.set(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
            return yml;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        boolean changed = migrate(yml);
        if (changed) {
            save(uuid, yml);
        }
        return yml;
    }

    private void save(UUID uuid, YamlConfiguration yml) {
        try {
            yml.set(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
            yml.save(file(uuid));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save quest progress for " + uuid, e);
        }
    }

    private File file(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".yml");
    }

    private String path(String questId) {
        return QUESTS_KEY + "." + questId;
    }

    private boolean migrate(YamlConfiguration yml) {
        int version = yml.getInt(DATA_VERSION_KEY, 0);
        boolean changed = false;
        if (version < 1) {
            changed |= migrateQuestRoot(yml);
        }
        if (version < 2) {
            changed |= normalizeQuestKeys(yml);
        }
        if (version != CURRENT_DATA_VERSION) {
            yml.set(DATA_VERSION_KEY, CURRENT_DATA_VERSION);
            changed = true;
        }
        return changed;
    }

    private boolean migrateQuestRoot(YamlConfiguration yml) {
        if (!yml.contains("quest")) {
            return false;
        }
        if (yml.contains(QUESTS_KEY)) {
            return false;
        }
        ConfigurationSection legacy = yml.getConfigurationSection("quest");
        if (legacy == null) {
            return false;
        }
        yml.set("quest", null);
        yml.createSection(QUESTS_KEY, legacy.getValues(false));
        return true;
    }

    private boolean normalizeQuestKeys(YamlConfiguration yml) {
        ConfigurationSection quests = yml.getConfigurationSection(QUESTS_KEY);
        if (quests == null) {
            return false;
        }
        boolean changed = false;
        for (String questId : new HashSet<>(quests.getKeys(false))) {
            String normalized = normalize(questId);
            if (normalized.equals(questId)) {
                continue;
            }
            String targetPath = path(normalized);
            if (!yml.contains(targetPath)) {
                yml.set(targetPath, yml.get(path(questId)));
                changed = true;
            }
            yml.set(path(questId), null);
            changed = true;
        }
        return changed;
    }

    private String normalize(String questId) {
        if (questId == null) {
            return "";
        }
        return questId.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
