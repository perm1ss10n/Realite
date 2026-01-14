package ru.realite.quests.service;

import ru.realite.quests.model.QuestType;

public record QuestListEntry(
        String id,
        String title,
        String description,
        QuestType type,
        QuestAvailability availability
) {
}
