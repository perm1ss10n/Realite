package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class TamingRulesConfigParser {

    private TamingRulesConfigParser() {
    }

    public static TamingRules parse(YamlConfiguration config) throws ConfigValidationException {
        List<String> errors = new ArrayList<>();

        int maxActive = config.getInt("limits.max-active", -1);
        int maxSummoned = config.getInt("limits.max-summoned", -1);
        int tameSeconds = config.getInt("cooldowns.tame-seconds", -1);
        int summonSeconds = config.getInt("cooldowns.summon-seconds", -1);

        if (maxActive <= 0) {
            errors.add("limits.max-active must be > 0");
        }
        if (maxSummoned <= 0) {
            errors.add("limits.max-summoned must be > 0");
        }
        if (tameSeconds < 0) {
            errors.add("cooldowns.tame-seconds must be >= 0");
        }
        if (summonSeconds < 0) {
            errors.add("cooldowns.summon-seconds must be >= 0");
        }

        if (!errors.isEmpty()) {
            throw new ConfigValidationException("Invalid taming.yml", errors);
        }

        return new TamingRules(maxActive, maxSummoned, Duration.ofSeconds(tameSeconds), Duration.ofSeconds(summonSeconds));
    }
}
