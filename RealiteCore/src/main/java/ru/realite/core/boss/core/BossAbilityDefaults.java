package ru.realite.core.boss.core;

import ru.realite.core.boss.ability.BlizzardAbility;
import ru.realite.core.boss.ability.DashStrikeAbility;
import ru.realite.core.boss.ability.FrostBoltAbility;
import ru.realite.core.boss.ability.GroundSlamAbility;
import ru.realite.core.boss.ability.IceCageAbility;
import ru.realite.core.boss.ability.SummonIceboundAbility;

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
        if (!registry.isRegistered(FrostBoltAbility.ID)) {
            registry.register(FrostBoltAbility.ID, FrostBoltAbility::new);
        }
        if (!registry.isRegistered(IceCageAbility.ID)) {
            registry.register(IceCageAbility.ID, IceCageAbility::new);
        }
        if (!registry.isRegistered(BlizzardAbility.ID)) {
            registry.register(BlizzardAbility.ID, BlizzardAbility::new);
        }
        if (!registry.isRegistered(SummonIceboundAbility.ID)) {
            registry.register(SummonIceboundAbility.ID, SummonIceboundAbility::new);
        }
    }
}
