package ru.realite.magic.effect;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.target.SpellTarget;

public final class EffectTargetResolver {

    private EffectTargetResolver() {
    }

    public static LivingEntity resolveEntity(SpellTarget target) {
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            return entityTarget.entity();
        }
        if (target instanceof SpellTarget.Self self) {
            return self.player();
        }
        return null;
    }

    public static Location resolveLocation(SpellTarget target, Player caster) {
        if (target instanceof SpellTarget.LocationTarget locationTarget) {
            return locationTarget.location();
        }
        if (target instanceof SpellTarget.BlockTarget blockTarget) {
            return blockTarget.location();
        }
        if (target instanceof SpellTarget.EntityTarget entityTarget) {
            return entityTarget.entity().getLocation();
        }
        if (target instanceof SpellTarget.Self self) {
            return self.player().getLocation();
        }
        if (caster != null) {
            return caster.getLocation();
        }
        return null;
    }
}
