package ru.realite.city.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyService {

    private Economy economy;

    public EconomyService(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("[Economy] Vault not found. Economy features disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null || rsp.getProvider() == null) {
            plugin.getLogger()
                    .warning("[Economy] Vault found, but no economy provider installed. Economy features disabled.");
            return;
        }

        this.economy = rsp.getProvider();
        plugin.getLogger().info("[Economy] Vault economy hooked: " + economy.getName());
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return false;
        }
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (!isAvailable()) {
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
