package ru.realite.guilds.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuildTreasuryService {

    private static final DateTimeFormatter LOG_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaPlugin plugin;
    private final File storageFile;
    private final File logFile;
    private final Map<String, Double> balances = new HashMap<>();

    public GuildTreasuryService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "treasury.yml");
        this.logFile = new File(plugin.getDataFolder(), "treasury-transactions.log");
        load();
    }

    public double getBalance(String guildId) {
        return balances.getOrDefault(normalizeTag(guildId), 0.0d);
    }

    public double withdraw(String guildId, double amount, String reason, UUID actor) {
        String normalized = normalizeTag(guildId);
        double current = balances.getOrDefault(normalized, 0.0d);
        if (amount <= 0.0d) {
            return current;
        }
        double newBalance = current - amount;
        balances.put(normalized, newBalance);
        save();
        return newBalance;
    }

    public void logTransaction(String guildId, double amount, String reason, UUID actor, double balanceAfter) {
        String safeReason = reason == null || reason.isBlank() ? "-" : reason.trim();
        String actorText = actor == null ? "-" : actor.toString();
        String line = String.format("%s | %s | %.2f | %.2f | %s | %s",
                LocalDateTime.now().format(LOG_FORMAT),
                normalizeTag(guildId),
                amount,
                balanceAfter,
                actorText,
                safeReason);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(line);
            writer.newLine();
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write treasury log: " + ex.getMessage());
        }
    }

    private void load() {
        balances.clear();
        if (!storageFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(storageFile);
        ConfigurationSection section = config.getConfigurationSection("balances");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            balances.put(normalizeTag(key), section.getDouble(key));
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection section = config.createSection("balances");
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
        try {
            config.save(storageFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save treasury.yml: " + ex.getMessage());
        }
    }

    private String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        return tag.trim().toUpperCase(Locale.ROOT);
    }
}
