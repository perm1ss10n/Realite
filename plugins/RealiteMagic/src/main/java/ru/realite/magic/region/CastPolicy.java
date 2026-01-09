package ru.realite.magic.region;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

public record CastPolicy(boolean allowed,
                         @Nullable String denyReasonKey,
                         Map<String, String> placeholders) {
    public static CastPolicy allow() {
        return new CastPolicy(true, null, Map.of());
    }

    public static CastPolicy deny(String reasonKey, Map<String, String> placeholders) {
        return new CastPolicy(false, reasonKey, placeholders == null ? Map.of() : Map.copyOf(placeholders));
    }
}
