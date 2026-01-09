package ru.realite.magic.cast;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.magic.target.SpellTargetDefinition;

public final class CastTraceUtils {

    private CastTraceUtils() {
    }

    public static LivingEntity findEntityHit(World world,
                                             Location location,
                                             double hitRadius,
                                             Player caster,
                                             SpellTargetDefinition targetDefinition) {
        if (world == null || location == null) {
            return null;
        }
        boolean allowPlayers = targetDefinition == null || targetDefinition.allowPlayers();
        boolean allowMobs = targetDefinition == null || targetDefinition.allowMobs();
        for (var entity : world.getNearbyEntities(location, hitRadius, hitRadius, hitRadius)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!CastTargetFilter.isAllowedEntity(living, caster, allowPlayers, allowMobs)) {
                continue;
            }
            return living;
        }
        return null;
    }
}
