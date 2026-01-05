package ru.realite.guilds.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

class GuildUpgradePurchaseTest {

    @TempDir
    Path tempDir;

    @Test
    void purchasesUpgradeAndLogsTreasury() throws IOException {
        writeUpgradesYaml(tempDir, """
                settings:
                  requirePermission: false
                  allowNegative: false
                upgrades:
                  upgrade_one:
                    enabled: true
                    name: "Upgrade One"
                    description: "upgrade"
                    maxLevel: 3
                    purchase:
                      cost:
                        type: TABLE
                        table:
                          1: 25
                          2: 50
                          3: 75
                """);
        writeTreasuryYaml(tempDir, """
                balances:
                  GUILD: 100
                """);

        JavaPlugin plugin = mockPlugin(tempDir);
        GuildRepository repository = new GuildRepository(plugin);
        GuildUpgradeConfigRepository upgradeConfig = new GuildUpgradeConfigRepository(plugin);
        GuildRankService rankService = mock(GuildRankService.class);
        GuildTreasuryService treasuryService = new GuildTreasuryService(plugin);
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

        GuildUpgradeService.PurchaseResult result = upgradeService.purchase(player, "upgrade_one");

        assertEquals(GuildUpgradeService.PurchaseStatus.SUCCESS, result.status());
        assertEquals(75.0d, treasuryService.getBalance("GUILD"));
        assertEquals(1, repository.getUpgradeLevel("GUILD", "upgrade_one"));

        Path logFile = tempDir.resolve("treasury-transactions.log");
        assertTrue(Files.exists(logFile));
        String logContent = Files.readString(logFile);
        assertTrue(logContent.contains("upgrade:upgrade_one:level:1"));
    }

    private static void writeUpgradesYaml(Path tempDir, String content) throws IOException {
        Files.writeString(tempDir.resolve("upgrades.yml"), content);
    }

    private static void writeTreasuryYaml(Path tempDir, String content) throws IOException {
        Files.writeString(tempDir.resolve("treasury.yml"), content);
    }

    private static JavaPlugin mockPlugin(Path tempDir) {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        return plugin;
    }
}
