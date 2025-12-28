package ru.realite.core.api;

/**
 * Минимальный фасад планировщика задач.
 */
public interface Scheduler {
    void runSync(Runnable task);

    void runLater(Runnable task, long delayTicks);

    void runRepeating(Runnable task, long delayTicks, long periodTicks);
}
