package ru.realite.guilds.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

class GuildUpgradeCostTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesFormulaAndTableCosts() throws IOException {
        writeUpgradesYaml(tempDir, """
                settings:
                  requirePermission: false
                  allowNegative: false
                upgrades:
                  formula_upgrade:
                    enabled: true
                    name: "Formula"
                    description: "formula"
                    maxLevel: 3
                    purchase:
                      cost:
                        type: FORMULA
                        formula: "100 + level * 10"
                  table_upgrade:
                    enabled: true
                    name: "Table"
                    description: "table"
                    maxLevel: 3
                    purchase:
                      cost:
                        type: TABLE
                        table:
                          1: 50
                          2: 75
                          3: 100
                """);

        JavaPlugin plugin = mockPlugin(tempDir);
        GuildRepository repository = new GuildRepository(plugin);
        GuildUpgradeConfigRepository upgradeConfig = new GuildUpgradeConfigRepository(plugin);
        GuildRankService rankService = mock(GuildRankService.class);
        GuildTreasuryService treasuryService = mock(GuildTreasuryService.class);
        GuildUpgradeService upgradeService = new GuildUpgradeService(
                plugin,
                repository,
                rankService,
                upgradeConfig,
                treasuryService,
                () -> null);

        UUID playerId = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(playerId);

        repository.saveGuild(new Guild("GUILD", "Guild", playerId, null, null, 1, 0L, Map.of()));
        repository.saveMember(new GuildMember(playerId, "GUILD", "member", null));

        GuildUpgradeService.UpgradeListResult result = upgradeService.list(player);
        assertEquals(GuildUpgradeService.UpgradeListStatus.SUCCESS, result.status());

        GuildUpgradeService.UpgradeEntry formula = result.entries().stream()
                .filter(entry -> entry.id().equals("formula_upgrade"))
                .findFirst()
                .orElse(null);
        GuildUpgradeService.UpgradeEntry table = result.entries().stream()
                .filter(entry -> entry.id().equals("table_upgrade"))
                .findFirst()
                .orElse(null);

        assertNotNull(formula);
        assertNotNull(table);
        assertEquals(110.0d, formula.nextCost());
        assertEquals(50.0d, table.nextCost());
        assertTrue(formula.nextCost() > table.nextCost());
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
