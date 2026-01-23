package ru.realite.core.boss.core;

import ru.realite.core.boss.data.BossDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class BossRegistry {
    private final Map<String, BossDefinition> definitions = new HashMap<>();

    public void register(BossDefinition definition) {
        Objects.requireNonNull(definition, "definition");
        String id = definition.id();
        if (definitions.containsKey(id)) {
            throw new IllegalArgumentException("Boss id already registered: " + id);
        }
        definitions.put(id, definition);
    }

    public BossDefinition requireDefinition(String id) {
        BossDefinition definition = definitions.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown bossId: " + id);
        }
        return definition;
    }

    public Collection<BossDefinition> definitions() {
        return definitions.values();
    }

    public void clear() {
        definitions.clear();
    }
}
