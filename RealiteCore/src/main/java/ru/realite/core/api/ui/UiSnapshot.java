package ru.realite.core.api.ui;

/**
 * Снимок состояния UI-провайдера для игрока.
 */
public record UiSnapshot(int current, int max) {

    /**
     * Отношение current/max без деления на 0.
     */
    public double ratio() {
        if (max <= 0) {
            return 0.0;
        }
        return (double) current / (double) max;
    }
}
