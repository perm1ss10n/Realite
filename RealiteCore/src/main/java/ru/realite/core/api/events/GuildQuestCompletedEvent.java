package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;

public final class GuildQuestCompletedEvent implements CoreEvent {
    private final UUID playerUuid;
    private final String guildTag;
    private final String questId;

    public GuildQuestCompletedEvent(UUID playerUuid, String guildTag, String questId) {
        this.playerUuid = Objects.requireNonNull(playerUuid, "playerUuid");
        this.guildTag = Objects.requireNonNull(guildTag, "guildTag");
        this.questId = Objects.requireNonNull(questId, "questId");
    }

    public UUID playerUuid() {
        return playerUuid;
    }

    public String guildTag() {
        return guildTag;
    }

    public String questId() {
        return questId;
    }
}
