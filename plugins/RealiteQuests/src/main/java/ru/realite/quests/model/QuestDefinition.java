package ru.realite.quests.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class QuestDefinition {

    private final String id;
    private final QuestType type;
    private final String title;
    private final String description;
    private final List<ObjectiveDefinition> objectives;
    private final List<RewardDefinition> rewards;
    private final QuestConditions conditions;

    public QuestDefinition(String id,
                           QuestType type,
                           String title,
                           String description,
                           List<ObjectiveDefinition> objectives,
                           List<RewardDefinition> rewards,
                           QuestConditions conditions) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.title = Objects.requireNonNullElse(title, id);
        this.description = Objects.requireNonNullElse(description, "");
        this.objectives = List.copyOf(Objects.requireNonNull(objectives, "objectives"));
        this.rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
        this.conditions = conditions == null ? QuestConditions.EMPTY : conditions;
    }

    public String id() {
        return id;
    }

    public QuestType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public List<ObjectiveDefinition> objectives() {
        return Collections.unmodifiableList(objectives);
    }

    public List<RewardDefinition> rewards() {
        return Collections.unmodifiableList(rewards);
    }

    public QuestConditions conditions() {
        return conditions;
    }
}
