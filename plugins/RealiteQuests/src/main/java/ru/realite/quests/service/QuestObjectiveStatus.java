package ru.realite.quests.service;

public record QuestObjectiveStatus(
        String id,
        String description,
        int current,
        int target,
        boolean completed
) {
}
