package ru.realite.familiars.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.familiars.model.FamiliarType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FamiliarTypeConfigParser {

    private FamiliarTypeConfigParser() {
    }

    public static Map<String, FamiliarType> parse(YamlConfiguration config) throws ConfigValidationException {
        List<String> errors = new ArrayList<>();
        ConfigurationSection typesSection = config.getConfigurationSection("types");
        if (typesSection == null) {
            errors.add("Missing 'types' section");
            throw new ConfigValidationException("Invalid familiars.yml", errors);
        }

        Map<String, FamiliarType> types = new HashMap<>();
        for (String id : typesSection.getKeys(false)) {
            ConfigurationSection section = typesSection.getConfigurationSection(id);
            if (section == null) {
                errors.add("Type '" + id + "' must be a section");
                continue;
            }
            if (id.isBlank()) {
                errors.add("Type id must not be blank");
                continue;
            }
            String role = section.getString("role");
            String displayKey = section.getString("displayKey");
            if (role == null || role.isBlank()) {
                errors.add("Type '" + id + "' missing role");
            }
            if (displayKey == null || displayKey.isBlank()) {
                errors.add("Type '" + id + "' missing displayKey");
            }
            List<String> allowedClasses = section.getStringList("allowedClasses");

            Map<String, Integer> baseStats = new HashMap<>();
            ConfigurationSection statsSection = section.getConfigurationSection("baseStats");
            if (statsSection == null) {
                errors.add("Type '" + id + "' missing baseStats section");
            } else {
                for (String statKey : statsSection.getKeys(false)) {
                    Object value = statsSection.get(statKey);
                    if (value instanceof Number number) {
                        baseStats.put(statKey, number.intValue());
                    } else {
                        errors.add("Type '" + id + "' baseStats." + statKey + " must be a number");
                    }
                }
                if (baseStats.isEmpty()) {
                    errors.add("Type '" + id + "' baseStats must have at least one entry");
                }
            }

            if (role != null && !role.isBlank() && displayKey != null && !displayKey.isBlank() && !baseStats.isEmpty()) {
                types.put(id, new FamiliarType(id, role, displayKey, List.copyOf(allowedClasses), Map.copyOf(baseStats)));
            }
        }

        if (!errors.isEmpty()) {
            throw new ConfigValidationException("Invalid familiars.yml", errors);
        }

        return Map.copyOf(types);
    }
}
