package ru.realite.magic.effect;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EffectExecutorRegistry {

    private final Map<String, SpellEffectExecutor> executors = new HashMap<>();

    public void register(SpellEffectExecutor executor) {
        Objects.requireNonNull(executor, "executor");
        executors.put(normalize(executor.type()), executor);
    }

    public Optional<SpellEffectExecutor> find(String type) {
        if (type == null || type.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(executors.get(normalize(type)));
    }

    public Collection<SpellEffectExecutor> all() {
        return Collections.unmodifiableCollection(executors.values());
    }

    private String normalize(String type) {
        return type.trim().toLowerCase(Locale.ROOT);
    }
}
