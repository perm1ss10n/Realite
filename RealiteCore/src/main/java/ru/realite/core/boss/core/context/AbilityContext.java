package ru.realite.core.boss.core.context;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;

public record AbilityContext(Player caster, Optional<LivingEntity> target) {
    public AbilityContext {
        if (caster == null) {
            throw new IllegalArgumentException("caster is null");
        }
        target = target == null ? Optional.empty() : target;
    }

    public static AbilityContext of(Player caster, LivingEntity target) {
        return new AbilityContext(caster, Optional.ofNullable(target));
    }
}
