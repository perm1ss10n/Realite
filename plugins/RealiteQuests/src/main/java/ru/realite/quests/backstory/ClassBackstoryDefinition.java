package ru.realite.quests.backstory;

import java.util.List;

public record ClassBackstoryDefinition(
        String classId,
        String title,
        List<String> pages,
        String introQuestId
) {
}
