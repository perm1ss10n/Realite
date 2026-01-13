package ru.realite.quests.service;

import java.util.List;
import ru.realite.quests.model.QuestType;

public record QuestDetails(
        String id,
        String title,
        String description,
        QuestType type,
        QuestAvailability availability,
        List<QuestObjectiveStatus> objectives,
        List<QuestRewardView> rewards
) {
}
