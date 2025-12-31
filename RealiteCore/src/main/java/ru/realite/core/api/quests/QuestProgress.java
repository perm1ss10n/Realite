package ru.realite.core.api.quests;

import java.util.Set;

/**
 * Прогресс прохождения квеста.
 */
public interface QuestProgress {

    QuestState state();

    Set<String> completedObjectives();
}
