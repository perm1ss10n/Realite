package ru.realite.core.boss.core;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.core.context.SpawnContext;
import ru.realite.core.boss.data.BossDefinition;
import ru.realite.core.boss.data.BossPhaseDefinition;
import ru.realite.core.boss.data.BossStatsDefinition;

import java.util.List;

public final class ConfigurableBoss extends AbstractRealiteBoss {
    private final BossDefinition definition;
    private final String displayName;

    public ConfigurableBoss(BossDefinition definition, BossAbilityRegistry abilityRegistry) {
        super(
                definition.id(),
                definition.stats().maxHp(),
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

    @Override
    protected void onSpawned(SpawnContext ctx) {
        applyStats(definition.stats());
    }

    private void applyStats(BossStatsDefinition stats) {
        LivingEntity living = getEntity();
        if (living == null || stats == null) {
            return;
        }
        AttributeInstance damage = living.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damage != null && stats.baseDamage() > 0.0) {
            damage.setBaseValue(stats.baseDamage());
        }
        AttributeInstance speed = living.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null && stats.movementSpeed() > 0.0) {
            speed.setBaseValue(stats.movementSpeed());
        }
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
