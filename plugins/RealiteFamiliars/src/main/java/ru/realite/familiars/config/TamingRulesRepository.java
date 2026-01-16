package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class TamingRulesRepository {

    private final TamingRules rules;

    private TamingRulesRepository(TamingRules rules) {
        this.rules = rules;
    }

    public static Optional<TamingRulesRepository> load(File file, Logger logger) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(logger, "logger");
        try {
            YamlConfiguration config = YamlConfigLoader.load(file);
            TamingRules rules = TamingRulesConfigParser.parse(config);
            return Optional.of(new TamingRulesRepository(rules));
        } catch (ConfigLoadException e) {
            logger.severe("Failed to load taming.yml: " + e.getMessage());
        } catch (ConfigValidationException e) {
            logger.severe("Failed to validate taming.yml: " + e.getMessage());
            for (String error : e.errors()) {
                logger.severe(" - " + error);
            }
        }
        return Optional.empty();
    }

    public TamingRules rules() {
        return rules;
    }
}
