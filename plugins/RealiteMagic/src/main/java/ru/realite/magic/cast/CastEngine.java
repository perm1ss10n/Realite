package ru.realite.magic.cast;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;

public final class CastEngine {

    private final Map<CastDeliveryType, CastStrategy> strategies = new EnumMap<>(CastDeliveryType.class);

    public CastEngine(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        CastLimits limits = CastLimits.fromConfig(plugin.getConfig());
        strategies.put(CastDeliveryType.INSTANT, new InstantCastStrategy());
        strategies.put(CastDeliveryType.PROJECTILE, new ProjectileCastStrategy(limits));
        strategies.put(CastDeliveryType.BEAM, new BeamCastStrategy(limits));
        strategies.put(CastDeliveryType.AOE, new AoeCastStrategy(limits));
        strategies.put(CastDeliveryType.CHAIN, new ChainCastStrategy(limits));
    }

    public CastExecutionPlan execute(org.bukkit.entity.Player caster,
                                     SpellDefinition spell,
                                     SpellTarget baseTarget) {
        CastDeliveryType deliveryType = spell.castDelivery();
        if (deliveryType == null) {
            deliveryType = CastDeliveryType.INSTANT;
        }
        CastStrategy strategy = strategies.getOrDefault(deliveryType, new InstantCastStrategy());
        return strategy.execute(caster, spell, baseTarget);
    }
}
