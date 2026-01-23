package ru.realite.core.boss.api;

import java.util.Objects;
import java.util.function.Consumer;

public final class BossPhase {
    private static final Consumer<RealiteBoss> NOOP = boss -> {};

    private final String id;
    private final double enterWhenHpPctAtMost;
    private final Consumer<RealiteBoss> onEnter;
    private final Consumer<RealiteBoss> onExit;

    public BossPhase(String id, double enterWhenHpPctAtMost) {
        this(id, enterWhenHpPctAtMost, NOOP, NOOP);
    }

    public BossPhase(String id,
                     double enterWhenHpPctAtMost,
                     Consumer<RealiteBoss> onEnter,
                     Consumer<RealiteBoss> onExit) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("BossPhase id is blank");
        }
        this.id = id;
        this.enterWhenHpPctAtMost = enterWhenHpPctAtMost;
        this.onEnter = Objects.requireNonNull(onEnter, "onEnter");
        this.onExit = Objects.requireNonNull(onExit, "onExit");
    }

    public String id() {
        return id;
    }

    public double enterWhenHpPctAtMost() {
        return enterWhenHpPctAtMost;
    }

    public boolean shouldEnter(double hpPct) {
        return hpPct <= enterWhenHpPctAtMost;
    }

    public void onEnter(RealiteBoss boss) {
        onEnter.accept(boss);
    }

    public void onExit(RealiteBoss boss) {
        onExit.accept(boss);
    }
}
