package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class MessagesRepository {

    private final Messages messages;

    private MessagesRepository(Messages messages) {
        this.messages = messages;
    }

    public static Optional<MessagesRepository> load(File file, Logger logger) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(logger, "logger");
        try {
            YamlConfiguration config = YamlConfigLoader.load(file);
            return Optional.of(new MessagesRepository(new Messages(config)));
        } catch (ConfigLoadException e) {
            logger.severe("Failed to load messages.yml: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Messages messages() {
        return messages;
    }
}
