package ru.realite.magic.cast;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class CastTargetFilter {

    private CastTargetFilter() {
    }

    public static boolean isAllowedEntity(LivingEntity entity,
                                          Player caster,
                                          boolean includePlayers,
                                          boolean includeMobs) {
        if (entity == null || caster == null) {
            return false;
        }
        if (entity.getUniqueId().equals(caster.getUniqueId())) {
            return false;
        }
        if (entity instanceof Player) {
            return includePlayers;
        }
        return includeMobs;
    }
}
