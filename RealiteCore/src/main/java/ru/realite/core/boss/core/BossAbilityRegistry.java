package ru.realite.core.boss.core;

import ru.realite.core.boss.api.BossAbility;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class BossAbilityRegistry {
    private final Map<String, Supplier<? extends BossAbility>> factories = new HashMap<>();

    public void register(String abilityId, Supplier<? extends BossAbility> factory) {
        if (abilityId == null || abilityId.isBlank()) {
            throw new IllegalArgumentException("abilityId is blank");
        }
        Objects.requireNonNull(factory, "factory");
        if (factories.containsKey(abilityId)) {
            throw new IllegalArgumentException("Ability already registered: " + abilityId);
        }
        factories.put(abilityId, factory);
    }

    public boolean isRegistered(String abilityId) {
        return factories.containsKey(abilityId);
    }

    public BossAbility create(String abilityId) {
        Supplier<? extends BossAbility> factory = factories.get(abilityId);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown abilityId: " + abilityId);
        }
        return factory.get();
    }
}
