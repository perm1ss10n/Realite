package ru.realite.magic.integration.economy;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultEconomyBridge implements EconomyBridge {

    private final Supplier<Economy> economySupplier;
    private final Logger logger;
    private boolean warned;

    public VaultEconomyBridge(Logger logger) {
        this(logger, VaultEconomyBridge::resolveEconomy);
    }

    public VaultEconomyBridge(Logger logger, Supplier<Economy> economySupplier) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.economySupplier = Objects.requireNonNull(economySupplier, "economySupplier");
    }

    @Override
    public boolean isAvailable() {
        return economy() != null;
    }

    @Override
    public double balance(UUID playerId) {
        Economy economy = economy();
        if (economy == null || playerId == null) {
            return 0.0;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        return economy.getBalance(player);
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        Economy economy = economy();
        if (economy == null || playerId == null) {
            return false;
        }
        if (amount <= 0) {
            return true;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        return response.transactionSuccess();
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        Economy economy = economy();
        if (economy == null || playerId == null || amount <= 0) {
            return;
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        economy.depositPlayer(player, amount);
    }

    @Override
    public Component currencyName() {
        Economy economy = economy();
        if (economy == null) {
            return Component.empty();
        }
        return Component.text(economy.currencyNamePlural());
    }

    private Economy economy() {
        Economy economy = economySupplier.get();
        if (economy == null) {
            warnMissingEconomy();
        }
        return economy;
    }

    private void warnMissingEconomy() {
        if (warned) {
            return;
        }
        warned = true;
        logger.warning("[Magic] Vault economy is unavailable.");
    }

    private static Economy resolveEconomy() {
        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            return null;
        }
        return provider.getProvider();
    }
}
