package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;
import ru.realite.core.api.quests.QuestStartTrigger;

public final class QuestStartedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final String questId;
    private final QuestStartTrigger trigger;

    public QuestStartedEvent(UUID playerUuid, String questId, QuestStartTrigger trigger) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.questId = Objects.requireNonNull(questId, "questId");
        this.trigger = Objects.requireNonNull(trigger, "trigger");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String questId() {
        return questId;
    }

    public QuestStartTrigger trigger() {
        return trigger;
    }
}
