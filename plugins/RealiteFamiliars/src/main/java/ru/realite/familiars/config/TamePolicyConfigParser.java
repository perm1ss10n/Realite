package ru.realite.familiars.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TamePolicyConfigParser {

    private TamePolicyConfigParser() {
    }

    public static TamePolicy parse(YamlConfiguration config) throws ConfigValidationException {
        List<String> errors = new ArrayList<>();
        Map<String, List<String>> allowedMobs = new HashMap<>();
        ConfigurationSection classes = config.getConfigurationSection("classes");
        if (classes != null) {
            for (String classId : classes.getKeys(false)) {
                ConfigurationSection classSection = classes.getConfigurationSection(classId);
                if (classSection == null) {
                    continue;
                }
                List<String> entries = classSection.getStringList("allowedMobs");
                if (entries.isEmpty()) {
                    errors.add("classes." + classId + ".allowedMobs must not be empty");
                    continue;
                }
                List<String> normalized = new ArrayList<>();
                for (String entry : entries) {
                    if (entry == null || entry.isBlank()) {
                        continue;
                    }
                    normalized.add(entry.toLowerCase(Locale.ROOT));
                }
                if (normalized.isEmpty()) {
                    errors.add("classes." + classId + ".allowedMobs must contain non-empty entries");
                    continue;
                }
                allowedMobs.put(classId.toLowerCase(Locale.ROOT), List.copyOf(normalized));
            }
        }
        if (!errors.isEmpty()) {
            throw new ConfigValidationException("Invalid tame-policy.yml", errors);
        }
        return new TamePolicy(Map.copyOf(allowedMobs));
    }
}
