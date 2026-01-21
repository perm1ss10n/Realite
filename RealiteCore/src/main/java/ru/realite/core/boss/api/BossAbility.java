package ru.realite.core.boss.api;

import ru.realite.core.boss.core.context.AbilityContext;

import java.util.Objects;

public interface BossAbility {
    String id();

    long cooldownTicks();

    boolean canCast(RealiteBoss boss, AbilityContext ctx);

    void cast(RealiteBoss boss, AbilityContext ctx);

    default void tick(RealiteBoss boss) {
    }

    // --- Helpers (чтобы реализации не страдали от Optional-рутины) ---

    default boolean hasTarget(AbilityContext ctx) {
        return ctx != null && ctx.target().isPresent();
    }

    default org.bukkit.entity.LivingEntity requiredTarget(AbilityContext ctx) {
        Objects.requireNonNull(ctx, "ctx");
        return ctx.target().orElseThrow(() -> new IllegalStateException("Ability '" + id() + "' requires a target"));
    }
}
