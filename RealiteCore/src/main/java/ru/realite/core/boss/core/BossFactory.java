package ru.realite.core.boss.core;

import ru.realite.core.boss.api.RealiteBoss;

@FunctionalInterface
public interface BossFactory {
    RealiteBoss create();
}
