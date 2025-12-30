package ru.realite.guilds.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class EconomyService {

    private Economy economy;

    public EconomyService(JavaPlugin plugin) {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("[Economy] Vault not found. Salary will be disabled.");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null || rsp.getProvider() == null) {
            plugin.getLogger()
                    .warning("[Economy] Vault found, but no economy provider installed. Salary will be disabled.");
            return;
        }

        this.economy = rsp.getProvider();
        plugin.getLogger().info("[Economy] Vault economy hooked: " + economy.getName());
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public void deposit(Player player, double amount) {
        if (economy == null || player == null) {
            return;
        }
        economy.depositPlayer(player, amount);
    }
}
