package ru.realite.ui.settings;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiSlot;

public final class UiSettings {

    private final Map<UiSlot, UiProviderId> slots = new EnumMap<>(UiSlot.class);

    public Optional<UiProviderId> provider(UiSlot slot) {
        return Optional.ofNullable(slots.get(slot));
    }

    public void setProvider(UiSlot slot, UiProviderId providerId) {
        if (providerId == null) {
            slots.remove(slot);
        } else {
            slots.put(slot, providerId);
        }
    }
}
