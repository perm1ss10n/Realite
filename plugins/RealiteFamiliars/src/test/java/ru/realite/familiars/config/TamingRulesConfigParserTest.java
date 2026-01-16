package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TamingRulesConfigParserTest {

    @Test
    void parsesValidConfig() throws Exception {
        String yaml = """
                limits:
                  max-active: 2
                  max-summoned: 1
                cooldowns:
                  tame-seconds: 10
                  summon-seconds: 5
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.load(new StringReader(yaml));

        TamingRules rules = TamingRulesConfigParser.parse(config);

        assertEquals(2, rules.maxActive());
        assertEquals(1, rules.maxSummoned());
        assertEquals(10, rules.tameCooldown().toSeconds());
    }

    @Test
    void failsOnNegativeValues() throws Exception {
        String yaml = """
                limits:
                  max-active: 0
                  max-summoned: -1
                cooldowns:
                  tame-seconds: -5
                  summon-seconds: 0
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.load(new StringReader(yaml));

        assertThrows(ConfigValidationException.class, () -> TamingRulesConfigParser.parse(config));
    }
}
