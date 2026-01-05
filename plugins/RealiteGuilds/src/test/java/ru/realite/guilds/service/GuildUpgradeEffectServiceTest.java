package ru.realite.guilds.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

class GuildUpgradeEffectServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesLinearAndTableEffectValues() throws IOException {
        writeUpgradesYaml(tempDir, """
                settings:
                  requirePermission: false
                  allowNegative: false
                upgrades:
                  slots_bonus:
                    enabled: true
                    name: "Slots"
                    description: "slots"
                    maxLevel: 3
                    purchase:
                      cost:
                        type: TABLE
                        table:
                          1: 1
                          2: 1
                          3: 1
                    effects:
                      - type: MEMBER_SLOTS
                        value:
                          type: LINEAR
                          base: 5
                          perLevel: 2
                  pve_boost:
                    enabled: true
                    name: "PVE"
                    description: "pve"
                    maxLevel: 3
                    purchase:
                      cost:
                        type: TABLE
                        table:
                          1: 1
                          2: 1
                          3: 1
                    effects:
                      - type: PVE_DAMAGE_MULTIPLIER
                        value:
                          type: TABLE
                          table:
                            1: 1.1
                            2: 1.2
                            3: 1.3
                """);

        JavaPlugin plugin = mockPlugin(tempDir);
        GuildRepository repository = new GuildRepository(plugin);
        GuildUpgradeConfigRepository upgradeConfig = new GuildUpgradeConfigRepository(plugin);
        GuildUpgradeEffectService effectService = new GuildUpgradeEffectService(
                new YamlConfiguration(),
                repository,
                upgradeConfig);

        UUID playerId = UUID.randomUUID();
        repository.saveGuild(new Guild("GUILD", "Guild", playerId, null, null, 1, 0L, Map.of()));
        repository.saveMember(new GuildMember(playerId, "GUILD", "member", null));
        repository.setUpgradeLevel("GUILD", "slots_bonus", 2);
        repository.setUpgradeLevel("GUILD", "pve_boost", 3);

        assertEquals(9.0d, effectService.resolveMemberSlotsBonus("GUILD"));
        assertEquals(1.3d, effectService.resolvePveDamageMultiplier("GUILD"));
    }

    private static void writeUpgradesYaml(Path tempDir, String content) throws IOException {
        Files.writeString(tempDir.resolve("upgrades.yml"), content);
    }

    private static JavaPlugin mockPlugin(Path tempDir) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        return plugin;
    }
}
