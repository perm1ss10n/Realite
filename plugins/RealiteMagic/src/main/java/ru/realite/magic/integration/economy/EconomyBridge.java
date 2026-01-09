package ru.realite.magic.integration.economy;

import java.util.UUID;
import net.kyori.adventure.text.Component;

public interface EconomyBridge {

    boolean isAvailable();

    double balance(UUID playerId);

    boolean withdraw(UUID playerId, double amount);

    void deposit(UUID playerId, double amount);

    default Component currencyName() {
        return Component.empty();
    }
}
