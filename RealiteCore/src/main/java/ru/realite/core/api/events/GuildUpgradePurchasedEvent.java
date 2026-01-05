package ru.realite.core.api.events;

import java.util.Objects;
import java.util.UUID;

public final class GuildUpgradePurchasedEvent implements CoreEvent {
    private final UUID actorUuid;
    private final String guildTag;
    private final String upgradeId;
    private final int level;
    private final double cost;
    private final double balanceAfter;

    public GuildUpgradePurchasedEvent(UUID actorUuid, String guildTag, String upgradeId,
                                      int level, double cost, double balanceAfter) {
        this.actorUuid = Objects.requireNonNull(actorUuid, "actorUuid");
        this.guildTag = Objects.requireNonNull(guildTag, "guildTag");
        this.upgradeId = Objects.requireNonNull(upgradeId, "upgradeId");
        this.level = level;
        this.cost = cost;
        this.balanceAfter = balanceAfter;
    }

    public UUID actorUuid() {
        return actorUuid;
    }

    public String guildTag() {
        return guildTag;
    }

    public String upgradeId() {
        return upgradeId;
    }

    public int level() {
        return level;
    }

    public double cost() {
        return cost;
    }

    public double balanceAfter() {
        return balanceAfter;
    }
}
