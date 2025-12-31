package ru.realite.core.api.quests;

import org.bukkit.entity.Player;

/**
 * Показывает и фиксирует предысторию класса.
 */
public interface ClassBackstoryService {

    /**
     * Показывает лор выбранного класса.
     *
     * @param player  игрок
     * @param classId id класса
     * @param force   если false, показывать только если лор ещё не подтверждён
     */
    void show(Player player, String classId, boolean force);

    /**
     * Подтверждает лор для класса.
     */
    void accept(Player player, String classId);

    /**
     * Помечает лор как пропущенный.
     */
    void skip(Player player, String classId);

    /**
     * Закрыть лор без отметки.
     */
    void defer(Player player, String classId);
}
