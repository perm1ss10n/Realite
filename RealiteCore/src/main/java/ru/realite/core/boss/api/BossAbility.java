package ru.realite.core.boss.api;

import ru.realite.core.boss.core.context.AbilityContext;

public interface BossAbility {
    String id();

    long cooldownTicks();

    boolean canCast(RealiteBoss boss, AbilityContext ctx);

    void cast(RealiteBoss boss, AbilityContext ctx);

    default void tick(RealiteBoss boss) {
    }
}
