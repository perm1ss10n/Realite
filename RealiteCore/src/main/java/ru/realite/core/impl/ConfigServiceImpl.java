package ru.realite.core.impl;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.core.api.Config;
import ru.realite.core.api.ConfigService;
import ru.realite.core.api.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

final class ConfigServiceImpl implements ConfigService {

    private final Platform platform;

    ConfigServiceImpl(Platform platform) {
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @Override
    public Config load(Path file) {
        Objects.requireNonNull(file, "file");
        ensureParentExists(file);
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file.toFile());
        platform.debug("Loaded config: " + file);
        return new ConfigImpl(file, platform, configuration);
    }

    @Override
    public Config loadOrCreateDefault(Path file, String resourcePath, ClassLoader cl) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(resourcePath, "resourcePath");
        Objects.requireNonNull(cl, "cl");
        if (Files.notExists(file)) {
            ensureParentExists(file);
            try (InputStream in = cl.getResourceAsStream(resourcePath)) {
                if (in == null) {
                    platform.warn("Default config resource not found: " + resourcePath + ". Creating empty file: " + file);
                    Files.createFile(file);
                } else {
                    Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
                    platform.info("Created default config: " + file);
                }
            } catch (IOException e) {
                platform.error("Failed to create default config: " + file, e);
                throw new IllegalStateException("Failed to create default config: " + file, e);
            }
        }
        return load(file);
    }

    private void ensureParentExists(Path file) {
        Path parent = file.getParent();
        if (parent == null) {
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            platform.error("Failed to create config directory: " + parent, e);
            throw new IllegalStateException("Failed to create config directory: " + parent, e);
        }
    }
}
