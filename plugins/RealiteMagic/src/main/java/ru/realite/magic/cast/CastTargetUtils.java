package ru.realite.magic.cast;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.target.SpellTarget;

public final class CastTargetUtils {

    private CastTargetUtils() {
    }

    public static LivingEntity primaryTarget(SpellTarget target, Player caster) {
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            return entityTarget.entity();
        }
        if (target instanceof SpellTarget.Self) {
            return caster;
        }
        return null;
    }

    public static Location impactLocation(SpellTarget target, Player caster) {
        if (target instanceof SpellTarget.LocationTarget locationTarget) {
            return locationTarget.location();
        }
        if (target instanceof SpellTarget.BlockTarget blockTarget) {
            return blockTarget.location();
        }
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            return entityTarget.entity().getLocation();
        }
        if (target instanceof SpellTarget.Self) {
            return caster.getLocation();
        }
        return caster.getLocation();
    }
}
