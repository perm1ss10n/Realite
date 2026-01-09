package ru.realite.magic.integration.economy;

import java.util.UUID;

public final class NoopEconomyBridge implements EconomyBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public double balance(UUID playerId) {
        return 0.0;
    }

    @Override
    public boolean withdraw(UUID playerId, double amount) {
        return false;
    }

    @Override
    public void deposit(UUID playerId, double amount) {
        // no-op
    }
}
