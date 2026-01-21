package ru.realite.core.boss.data;

import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.BossFactory;
import ru.realite.core.boss.impl.BossFirst;

import java.util.HashMap;
import java.util.Map;

public final class BossConfigLoader {
    private final Map<String, BossFactory> factories = new HashMap<>();

    public BossConfigLoader() {
        registerDefaults();
    }

    public void register(String bossId, BossFactory factory) {
        if (bossId == null || bossId.isBlank()) {
            throw new IllegalArgumentException("bossId is blank");
        }
        if (factory == null) {
            throw new IllegalArgumentException("factory is null");
        }
        factories.put(bossId, factory);
    }

    public RealiteBoss create(String bossId) {
        BossFactory factory = factories.get(bossId);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown bossId: " + bossId);
        }
        return factory.create();
    }

    private void registerDefaults() {
        register(BossFirst.ID, BossFirst::new);
    }
}
