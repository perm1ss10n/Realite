package ru.realite.quests.service;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QuestUnlockRepository {

    private final File file;

    public QuestUnlockRepository(Path dataFolder) {
        this.file = dataFolder.resolve("quest_unlocks.yml").toFile();
    }

    public Set<String> getUnlocks(UUID uuid) {
        YamlConfiguration yml = load();
        List<String> raw = yml.getStringList(uuid.toString());
        Set<String> unlocks = new HashSet<>();
        for (String entry : raw) {
            if (entry != null && !entry.isBlank()) {
                unlocks.add(entry.trim().toLowerCase());
            }
        }
        return unlocks;
    }

    public boolean hasUnlock(UUID uuid, String unlockId) {
        if (unlockId == null || unlockId.isBlank()) {
            return false;
        }
        return getUnlocks(uuid).contains(unlockId.trim().toLowerCase());
    }

    public void grantUnlock(UUID uuid, String unlockId) {
        if (unlockId == null || unlockId.isBlank()) {
            return;
        }
        String normalized = unlockId.trim().toLowerCase();
        YamlConfiguration yml = load();
        List<String> raw = yml.getStringList(uuid.toString());
        Set<String> unlocks = new HashSet<>();
        for (String entry : raw) {
            if (entry != null && !entry.isBlank()) {
                unlocks.add(entry.trim().toLowerCase());
            }
        }
        if (unlocks.add(normalized)) {
            yml.set(uuid.toString(), List.copyOf(unlocks));
            save(yml);
        }
    }

    private YamlConfiguration load() {
        if (!file.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private void save(YamlConfiguration yml) {
        try {
            yml.save(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save quest unlocks to " + file.getName(), e);
        }
    }
}
