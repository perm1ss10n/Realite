package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class TamePolicyRepository {

    private final TamePolicy policy;

    private TamePolicyRepository(TamePolicy policy) {
        this.policy = policy;
    }

    public static Optional<TamePolicyRepository> load(File file, Logger logger) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(logger, "logger");
        try {
            YamlConfiguration config = YamlConfigLoader.load(file);
            TamePolicy policy = TamePolicyConfigParser.parse(config);
            return Optional.of(new TamePolicyRepository(policy));
        } catch (ConfigLoadException e) {
            logger.severe("Failed to load tame-policy.yml: " + e.getMessage());
        } catch (ConfigValidationException e) {
            logger.severe("Failed to validate tame-policy.yml: " + e.getMessage());
            for (String error : e.errors()) {
                logger.severe(" - " + error);
            }
        }
        return Optional.empty();
    }

    public TamePolicy policy() {
        return policy;
    }
}
