package ru.realite.magic.effect;

import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.cast.CastExecutionPlan;

public final class EffectTargetResolver {

    private EffectTargetResolver() {
    }

    public static List<LivingEntity> resolveTargets(CastExecutionPlan plan, EffectApplyMode mode) {
        if (plan == null) {
            return List.of();
        }
        if (mode == EffectApplyMode.ALL) {
            return plan.targets().stream()
                    .filter(Objects::nonNull)
                    .toList();
        }
        LivingEntity primary = plan.primaryTarget();
        return primary == null ? List.of() : List.of(primary);
    }

    public static Location resolveLocation(CastExecutionPlan plan, EffectTargetType targetType, Player caster) {
        if (plan == null) {
            return caster == null ? null : caster.getLocation();
        }
        EffectTargetType resolved = targetType == null ? EffectTargetType.LOCATION : targetType;
        return switch (resolved) {
            case ORIGIN -> plan.origin();
            case IMPACT -> plan.impactLocation() != null ? plan.impactLocation() : plan.origin();
            case PRIMARY -> plan.primaryTarget() != null ? plan.primaryTarget().getLocation() : plan.origin();
            case LOCATION -> {
                if (plan.impactLocation() != null) {
                    yield plan.impactLocation();
                }
                if (plan.primaryTarget() != null) {
                    yield plan.primaryTarget().getLocation();
                }
                yield plan.origin();
            }
            case ENTITY -> plan.primaryTarget() != null ? plan.primaryTarget().getLocation() : plan.origin();
        };
    }
}
