package ru.realite.quests.service;

import ru.realite.quests.model.QuestDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class QuestRepository {

    private final Map<String, QuestDefinition> quests;

    public QuestRepository(Map<String, QuestDefinition> quests) {
        this.quests = new HashMap<>(Objects.requireNonNull(quests, "quests"));
    }

    public QuestDefinition get(String questId) {
        if (questId == null) {
            return null;
        }
        return quests.get(normalize(questId));
    }

    public Collection<QuestDefinition> all() {
        return quests.values();
    }

    public boolean isEmpty() {
        return quests.isEmpty();
    }

    private String normalize(String questId) {
        return questId.trim().toLowerCase(Locale.ROOT);
    }
}
