package ru.realite.classes.service;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EconomyService {

    private Economy economy; // null => экономики нет, деньги игнорируем

    public EconomyService(JavaPlugin plugin) {
        // 1) Проверяем, установлен ли Vault
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().info("[Economy] Vault not found. Money will be ignored.");
            return;
        }

        // 2) Берём провайдера экономики через Vault
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null || rsp.getProvider() == null) {
            plugin.getLogger()
                    .warning("[Economy] Vault found, but no economy provider installed. Money will be ignored.");
            return;
        }

        this.economy = rsp.getProvider();
        plugin.getLogger().info("[Economy] Vault economy hooked: " + economy.getName());
    }

    /** Экономика реально доступна (Vault + economy-плагин) */
    public boolean isAvailable() {
        return economy != null;
    }

    /** Хватает ли денег. Если экономики нет — НЕ блокируем. */
    public boolean has(Player player, double amount) {
        if (!isAvailable())
            return true;
        return economy.has(player, amount);
    }

    /**
     * Списать деньги. Если экономики нет — считаем успешным (чтобы не ломать
     * геймплей).
     */
    public boolean withdraw(Player player, double amount) {
        if (!isAvailable())
            return true;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /** Выдать деньги. Если экономики нет — просто игнорируем. */
    public void deposit(Player player, double amount) {
        if (!isAvailable())
            return;
        economy.depositPlayer(player, amount);
    }

    public double getBalance(org.bukkit.entity.Player player) {
        if (economy == null)
            return 0.0;
        return economy.getBalance(player);
    }
}
