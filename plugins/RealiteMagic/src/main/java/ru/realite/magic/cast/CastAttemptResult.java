package ru.realite.magic.cast;

import java.util.Map;
import ru.realite.magic.spell.SpellDefinition;

public sealed interface CastAttemptResult permits CastAttemptResult.Success, CastAttemptResult.Fail {

    record Success(SpellDefinition spell) implements CastAttemptResult {
    }

    record Fail(String reasonKey,
                Map<String, String> placeholders,
                boolean silent,
                long cooldownMsForSpam) implements CastAttemptResult {
    }
}
