package ru.realite.core.api.ui;

import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Источник UI-данных.
 */
public interface UiProvider {
    UiProviderId id();

    Optional<UiSnapshot> snapshot(Player player);

    boolean isAvailable(Player player);
}
