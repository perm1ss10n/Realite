package ru.realite.guilds.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.realite.guilds.model.upgrade.UpgradeDefinition;

class GuildUpgradeConfigRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void invalidUpgradeIsDisabledButRepositoryLoads() throws IOException {
        Files.writeString(tempDir.resolve("upgrades.yml"), """
                settings:
                  requirePermission: false
                  allowNegative: false
                upgrades:
                  broken_upgrade:
                    enabled: true
                    name: "Broken"
                    description: "bad"
                    maxLevel: 3
                    purchase:
                      cost:
                        type: UNKNOWN
                """);

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());

        GuildUpgradeConfigRepository repository = new GuildUpgradeConfigRepository(plugin);
        UpgradeDefinition definition = repository.getUpgrades().get("broken_upgrade");

        assertNotNull(definition);
        assertFalse(definition.enabled());
    }
}
