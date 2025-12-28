package ru.realite.core.impl;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.core.api.Config;
import ru.realite.core.api.Platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class ConfigImpl implements Config {

    private final Path file;
    private final Platform platform;
    private final YamlConfiguration config;

    ConfigImpl(Path file, Platform platform, YamlConfiguration config) {
        this.file = Objects.requireNonNull(file, "file");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.config = Objects.requireNonNull(config, "config");
    }

    @Override
    public String getString(String path, String def) {
        return config.getString(path, def);
    }

    @Override
    public int getInt(String path, int def) {
        return config.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        return config.getBoolean(path, def);
    }

    @Override
    public double getDouble(String path, double def) {
        return config.getDouble(path, def);
    }

    @Override
    public List<String> getStringList(String path) {
        return config.getStringList(path);
    }

    @Override
    public boolean contains(String path) {
        return config.contains(path);
    }

    @Override
    public void set(String path, Object value) {
        config.set(path, value);
    }

    @Override
    public void save() {
        try {
            config.save(file.toFile());
        } catch (IOException e) {
            platform.error("Failed to save config: " + file, e);
            throw new IllegalStateException("Failed to save config: " + file, e);
        }
    }

    @Override
    public void reload() {
        try {
            config.load(file.toFile());
        } catch (Exception e) {
            platform.error("Failed to reload config: " + file, e);
            throw new IllegalStateException("Failed to reload config: " + file, e);
        }
    }
}
