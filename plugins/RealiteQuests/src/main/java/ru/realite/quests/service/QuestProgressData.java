package ru.realite.quests.service;

import ru.realite.core.api.quests.QuestProgress;
import ru.realite.core.api.quests.QuestState;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuestProgressData implements QuestProgress {

    private QuestState state;
    private boolean rewardGranted;
    private final Set<String> completedObjectives;
    private final Map<String, Integer> objectiveCounts;

    public QuestProgressData(QuestState state,
                             boolean rewardGranted,
                             Set<String> completedObjectives,
                             Map<String, Integer> objectiveCounts) {
        this.state = Objects.requireNonNull(state, "state");
        this.rewardGranted = rewardGranted;
        this.completedObjectives = new HashSet<>(Objects.requireNonNull(completedObjectives, "completedObjectives"));
        this.objectiveCounts = new HashMap<>(Objects.requireNonNull(objectiveCounts, "objectiveCounts"));
    }

    @Override
    public QuestState state() {
        return state;
    }

    public void state(QuestState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public boolean rewardGranted() {
        return rewardGranted;
    }

    public void rewardGranted(boolean rewardGranted) {
        this.rewardGranted = rewardGranted;
    }

    @Override
    public Set<String> completedObjectives() {
        return Collections.unmodifiableSet(completedObjectives);
    }

    public Set<String> completedObjectivesMutable() {
        return completedObjectives;
    }

    public Map<String, Integer> objectiveCounts() {
        return Collections.unmodifiableMap(objectiveCounts);
    }

    public Map<String, Integer> objectiveCountsMutable() {
        return objectiveCounts;
    }
}
