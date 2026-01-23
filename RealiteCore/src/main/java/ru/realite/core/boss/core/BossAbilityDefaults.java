package ru.realite.core.boss.core;

import ru.realite.core.boss.ability.DashStrikeAbility;
import ru.realite.core.boss.ability.GroundSlamAbility;

public final class BossAbilityDefaults {
    private BossAbilityDefaults() {
    }

    public static void registerDefaults(BossAbilityRegistry registry) {
        if (!registry.isRegistered(DashStrikeAbility.ID)) {
            registry.register(DashStrikeAbility.ID, DashStrikeAbility::new);
        }
        if (!registry.isRegistered(GroundSlamAbility.ID)) {
            registry.register(GroundSlamAbility.ID, GroundSlamAbility::new);
        }
    }
}
