package ru.realite.core.boss.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
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
                toAbilities(definition.abilityIds(), abilityRegistry));
        this.definition = definition;
        this.displayName = definition.name();
    }

    @Override
    protected LivingEntity spawnEntity(SpawnContext ctx) {
        Location location = ctx.location();
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("Spawn location has no world");
        }

        // ВАЖНО: в твоей версии API нет перегрузки с Consumer, поэтому спавним обычным
        // способом
        Entity spawned = world.spawnEntity(location, definition.entityType());

        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            throw new IllegalStateException("Configured entityType is not a LivingEntity: " + definition.entityType());
        }

        // Кастомизация после спавна
        if (displayName != null && !displayName.isBlank()) {
            living.customName(Component.text(displayName));
            living.setCustomNameVisible(true);
        }

        // Опционально: если захочешь сразу агрить на инициатора:
        // ctx.initiator().ifPresent(player -> { ... });

        return living;
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
