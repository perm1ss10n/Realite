package ru.realite.core.api.ui;

import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * Сигнал для UI о необходимости обновить данные конкретного провайдера.
 */
public record UiInvalidateEvent(Player player, UiProviderId providerId) {

    public UiInvalidateEvent {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(providerId, "providerId");
    }
}
