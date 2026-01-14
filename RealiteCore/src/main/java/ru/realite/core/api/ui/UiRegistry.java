package ru.realite.core.api.ui;

import java.util.Collection;
import java.util.Optional;

/**
 * Реестр UI-провайдеров.
 */
public interface UiRegistry {
    void register(UiProvider provider);

    Optional<UiProvider> provider(UiProviderId id);

    Collection<UiProviderId> providerIds();
}
