package ru.realite.magic.requirements;

import java.util.Map;
import java.util.Objects;

public sealed interface CheckResult permits CheckResult.Ok, CheckResult.Fail {

    record Ok() implements CheckResult {}

    record Fail(String reasonKey, Map<String, String> placeholders) implements CheckResult {

        public Fail {
            Objects.requireNonNull(reasonKey, "reasonKey");
            if (placeholders == null) {
                placeholders = Map.of();
            } else {
                placeholders = Map.copyOf(placeholders);
            }
        }
    }
}
