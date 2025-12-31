package ru.realite.quests.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class QuestDefinition {

    private final String id;
    private final List<ObjectiveDefinition> objectives;
    private final List<RewardDefinition> rewards;

    public QuestDefinition(String id,
                           List<ObjectiveDefinition> objectives,
                           List<RewardDefinition> rewards) {
        this.id = Objects.requireNonNull(id, "id");
        this.objectives = List.copyOf(Objects.requireNonNull(objectives, "objectives"));
        this.rewards = List.copyOf(Objects.requireNonNull(rewards, "rewards"));
    }

    public String id() {
        return id;
    }

    public List<ObjectiveDefinition> objectives() {
        return Collections.unmodifiableList(objectives);
    }

    public List<RewardDefinition> rewards() {
        return Collections.unmodifiableList(rewards);
    }
}
