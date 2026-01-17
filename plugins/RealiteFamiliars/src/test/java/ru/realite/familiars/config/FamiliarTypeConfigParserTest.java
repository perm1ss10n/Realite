package ru.realite.familiars.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamiliarTypeConfigParserTest {

    @Test
    void parsesValidConfig() throws Exception {
        String yaml = """
                types:
                  wolf:
                    role: combat
                    displayKey: familiar.wolf
                    modelId: realite:wolf_01
                    allowedClasses: [warrior]
                    baseStats:
                      health: 20
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.load(new StringReader(yaml));

        var types = FamiliarTypeConfigParser.parse(config);

        assertEquals(1, types.size());
        assertTrue(types.containsKey("wolf"));
        assertEquals("combat", types.get("wolf").role());
        assertTrue(types.get("wolf").modelId().isPresent());
        assertEquals("realite:wolf_01", types.get("wolf").modelId().orElseThrow());
    }

    @Test
    void failsOnMissingRole() throws Exception {
        String yaml = """
                types:
                  wolf:
                    displayKey: familiar.wolf
                    baseStats:
                      health: 20
                """;
        YamlConfiguration config = new YamlConfiguration();
        config.load(new StringReader(yaml));

        ConfigValidationException exception = assertThrows(ConfigValidationException.class,
                () -> FamiliarTypeConfigParser.parse(config));

        assertTrue(exception.errors().stream().anyMatch(error -> error.contains("missing role")));
    }
}
