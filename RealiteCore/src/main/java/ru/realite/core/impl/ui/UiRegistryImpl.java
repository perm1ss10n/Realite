package ru.realite.core.impl.ui;

import ru.realite.core.api.ui.UiProvider;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Базовая реализация реестра UI-провайдеров.
 */
public final class UiRegistryImpl implements UiRegistry {

    private final Map<UiProviderId, UiProvider> providers = new ConcurrentHashMap<>();

    @Override
    public void register(UiProvider provider) {
        Objects.requireNonNull(provider, "provider");
        UiProviderId id = Objects.requireNonNull(provider.id(), "provider.id");
        UiProvider prev = providers.putIfAbsent(id, provider);
        if (prev != null) {
            throw new IllegalStateException("UiProvider already registered: " + id);
        }
    }

    @Override
    public Optional<UiProvider> provider(UiProviderId id) {
        Objects.requireNonNull(id, "id");
        return Optional.ofNullable(providers.get(id));
    }

    @Override
    public Collection<UiProviderId> providerIds() {
        return Collections.unmodifiableSet(providers.keySet());
    }
}
