package ru.realite.quests.backstory;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

public final class BackstoryProgressRepository {

    private final File playersDir;

    public BackstoryProgressRepository(Path dataFolder) {
        this.playersDir = dataFolder.resolve("playerdata").toFile();
        if (!playersDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            playersDir.mkdirs();
        }
    }

    public boolean isConfirmed(UUID playerUuid, String classId) {
        String normalized = normalize(classId);
        if (normalized == null) {
            return false;
        }
        YamlConfiguration yml = load(playerUuid);
        return yml.contains(path(normalized));
    }

    public void setAccepted(UUID playerUuid, String classId, boolean accepted) {
        String normalized = normalize(classId);
        if (normalized == null) {
            return;
        }
        YamlConfiguration yml = load(playerUuid);
        yml.set(path(normalized), accepted);
        save(playerUuid, yml);
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
            throw new IllegalStateException("Failed to save backstory for " + uuid, e);
        }
    }

    private File file(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".yml");
    }

    private String path(String classId) {
        return "backstory." + classId + ".accepted";
    }

    private String normalize(String classId) {
        if (classId == null) {
            return null;
        }
        String trimmed = classId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
