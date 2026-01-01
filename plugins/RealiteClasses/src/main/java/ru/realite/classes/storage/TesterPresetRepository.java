package ru.realite.classes.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.classes.model.ClassId;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TesterPresetRepository {

    public record TesterPreset(ClassId classId, String evolutionId, Integer level, Set<ClassId> mastered) {
    }

    private final File file;
    private final Map<String, TesterPreset> presets = new HashMap<>();

    public TesterPresetRepository(File dataFolder) {
        this.file = new File(dataFolder, "tester-presets.yml");
        reload();
    }

    public void reload() {
        presets.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("presets");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) {
                continue;
            }

            ClassId classId = ClassId.fromString(sec.getString("class"));
            String evolutionId = sec.getString("evolution");
            Integer level = sec.contains("level") ? sec.getInt("level") : null;

            Set<ClassId> mastered = new HashSet<>();
            for (String raw : sec.getStringList("mastered")) {
                ClassId mid = ClassId.fromString(raw);
                if (mid != null) {
                    mastered.add(mid);
                }
            }

            presets.put(id, new TesterPreset(classId, evolutionId, level, mastered));
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (var entry : presets.entrySet()) {
            String id = entry.getKey();
            TesterPreset preset = entry.getValue();
            String path = "presets." + id;
            if (preset.classId() != null) {
                yml.set(path + ".class", preset.classId().name().toLowerCase());
            }
            if (preset.evolutionId() != null && !preset.evolutionId().isBlank()) {
                yml.set(path + ".evolution", preset.evolutionId());
            }
            if (preset.level() != null) {
                yml.set(path + ".level", preset.level());
            }
            if (preset.mastered() != null && !preset.mastered().isEmpty()) {
                yml.set(path + ".mastered", preset.mastered().stream().map(idVal -> idVal.name().toLowerCase()).toList());
            }
        }

        try {
            yml.save(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save tester presets", e);
        }
    }

    public TesterPreset get(String id) {
        return presets.get(id);
    }

    public void set(String id, TesterPreset preset) {
        presets.put(id, preset);
    }

    public void remove(String id) {
        presets.remove(id);
    }

    public Collection<String> listIds() {
        return presets.keySet();
    }
}
