package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.familiars.model.FamiliarType;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class FamiliarTypeRepository {

    private final Map<String, FamiliarType> types;

    private FamiliarTypeRepository(Map<String, FamiliarType> types) {
        this.types = Map.copyOf(types);
    }

    public static Optional<FamiliarTypeRepository> load(File file, Logger logger) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(logger, "logger");
        try {
            YamlConfiguration config = YamlConfigLoader.load(file);
            Map<String, FamiliarType> types = FamiliarTypeConfigParser.parse(config);
            return Optional.of(new FamiliarTypeRepository(types));
        } catch (ConfigLoadException e) {
            logger.severe("Failed to load familiars.yml: " + e.getMessage());
        } catch (ConfigValidationException e) {
            logger.severe("Failed to validate familiars.yml: " + e.getMessage());
            for (String error : e.errors()) {
                logger.severe(" - " + error);
            }
        }
        return Optional.empty();
    }

    public Map<String, FamiliarType> types() {
        return types;
    }

    public FamiliarType get(String id) {
        return types.get(id);
    }
}
