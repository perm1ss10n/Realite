package ru.realite.familiars.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public final class YamlConfigLoader {

    private YamlConfigLoader() {
    }

    public static YamlConfiguration load(File file) throws ConfigLoadException {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.load(file);
            return configuration;
        } catch (IOException | InvalidConfigurationException e) {
            throw new ConfigLoadException("Failed to parse YAML: " + file.getName(), e);
        }
    }
}
