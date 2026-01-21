package ru.realite.core.boss.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.core.context.SpawnContext;
import ru.realite.core.boss.data.BossDefinition;
import ru.realite.core.boss.data.BossPhaseDefinition;

import java.util.List;

public final class ConfigurableBoss extends AbstractRealiteBoss {
    private final BossDefinition definition;
    private final String displayName;

    public ConfigurableBoss(BossDefinition definition, BossAbilityRegistry abilityRegistry) {
        super(
                definition.id(),
                definition.maxHp(),
                toPhases(definition.phases()),
                toAbilities(definition.abilityIds(), abilityRegistry)
        );
        this.definition = definition;
        this.displayName = definition.name();
    }

    @Override
    protected LivingEntity spawnEntity(SpawnContext ctx) {
        Location location = ctx.location();
        if (location.getWorld() == null) {
            throw new IllegalStateException("Spawn location has no world");
        }
        return (LivingEntity) location.getWorld().spawnEntity(location, definition.entityType(), spawned -> {
            if (displayName != null) {
                spawned.customName(Component.text(displayName));
                spawned.setCustomNameVisible(true);
            }
        });
    }

    private static List<BossPhase> toPhases(List<BossPhaseDefinition> phases) {
        return phases.stream()
                .map(phase -> new BossPhase(phase.id(), phase.enterAt()))
                .toList();
    }

    private static List<BossAbility> toAbilities(List<String> abilityIds, BossAbilityRegistry registry) {
        return abilityIds.stream()
                .map(registry::create)
                .toList();
    }
}
