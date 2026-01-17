package ru.realite.familiars.integration.quests;

public interface QuestsBridge {

    boolean isAvailable();

    void publish(FamiliarQuestEvent event);
}
