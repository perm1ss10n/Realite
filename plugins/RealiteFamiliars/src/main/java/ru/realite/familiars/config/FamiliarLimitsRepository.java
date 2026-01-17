package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class FamiliarLimitsRepository {

    private final FamiliarLimits limits;

    private FamiliarLimitsRepository(FamiliarLimits limits) {
        this.limits = limits;
    }

    public static Optional<FamiliarLimitsRepository> load(File file, Logger logger) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(logger, "logger");
        try {
            YamlConfiguration config = YamlConfigLoader.load(file);
            FamiliarLimits limits = FamiliarLimitsConfigParser.parse(config);
            return Optional.of(new FamiliarLimitsRepository(limits));
        } catch (ConfigLoadException e) {
            logger.severe("Failed to load limits.yml: " + e.getMessage());
        } catch (ConfigValidationException e) {
            logger.severe("Failed to validate limits.yml: " + e.getMessage());
            for (String error : e.errors()) {
                logger.severe(" - " + error);
            }
        }
        return Optional.empty();
    }

    public FamiliarLimits limits() {
        return limits;
    }
}
