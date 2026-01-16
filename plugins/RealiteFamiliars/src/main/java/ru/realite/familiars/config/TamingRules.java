package ru.realite.familiars.config;

import java.time.Duration;

public record TamingRules(
        int maxActive,
        int maxSummoned,
        Duration tameCooldown,
        Duration summonCooldown
) {
}
