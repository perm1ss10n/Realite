package ru.realite.familiars.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FamiliarLimitsConfigParser {

    private FamiliarLimitsConfigParser() {
    }

    public static FamiliarLimits parse(YamlConfiguration config) throws ConfigValidationException {
        List<String> errors = new ArrayList<>();
        int defaultLimit = config.getInt("default", 1);
        if (defaultLimit <= 0) {
            errors.add("default must be > 0");
        }

        Map<String, Map<Integer, Integer>> classTierLimits = new HashMap<>();
        ConfigurationSection classes = config.getConfigurationSection("classes");
        if (classes != null) {
            for (String classId : classes.getKeys(false)) {
                ConfigurationSection classSection = classes.getConfigurationSection(classId);
                if (classSection == null) {
                    continue;
                }
                ConfigurationSection tiers = classSection.getConfigurationSection("tiers");
                if (tiers == null) {
                    continue;
                }
                Map<Integer, Integer> tierLimits = new HashMap<>();
                for (String tierKey : tiers.getKeys(false)) {
                    int tier;
                    try {
                        tier = Integer.parseInt(tierKey);
                    } catch (NumberFormatException e) {
                        errors.add("classes." + classId + ".tiers." + tierKey + " must be a number");
                        continue;
                    }
                    int limit = tiers.getInt(tierKey, -1);
                    if (limit <= 0) {
                        errors.add("classes." + classId + ".tiers." + tierKey + " must be > 0");
                        continue;
                    }
                    tierLimits.put(tier, limit);
                }
                if (!tierLimits.isEmpty()) {
                    classTierLimits.put(classId.toLowerCase(Locale.ROOT), Map.copyOf(tierLimits));
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ConfigValidationException("Invalid limits.yml", errors);
        }

        return new FamiliarLimits(defaultLimit, Map.copyOf(classTierLimits));
    }
}
