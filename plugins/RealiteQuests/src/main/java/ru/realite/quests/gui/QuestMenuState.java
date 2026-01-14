package ru.realite.quests.gui;

import ru.realite.quests.service.QuestAvailability;
import ru.realite.quests.service.QuestSort;

public record QuestMenuState(QuestAvailability filter, QuestSort sort, int page) {

    public static QuestMenuState defaultState() {
        return new QuestMenuState(QuestAvailability.ACTIVE, QuestSort.TYPE, 0);
    }
}
