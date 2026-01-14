package ru.realite.ui.util;

import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.i18n.MiniMessageMessages;

public final class UiText {

    private UiText() {}

    public static String providerName(MiniMessageMessages messages, UiProviderId id) {
        if (messages == null || id == null) {
            return id == null ? "" : id.value();
        }
        String key = "ui.provider." + id.value();
        String raw = messages.raw(key);
        if (raw == null || raw.isBlank()) {
            return id.value();
        }
        return raw;
    }

    public static String noneName(MiniMessageMessages messages) {
        if (messages == null) {
            return "None";
        }
        String raw = messages.raw("ui.settings.none");
        if (raw == null || raw.isBlank()) {
            return "None";
        }
        return raw;
    }
}
