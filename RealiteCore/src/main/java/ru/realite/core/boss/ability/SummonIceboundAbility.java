package ru.realite.core.boss.ability;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.util.Vector;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossAbilityCleanup;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.AbilityContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class SummonIceboundAbility implements BossAbility, BossAbilityCleanup {
    public static final String ID = "summon_icebound";

    private static final long BASE_COOLDOWN_TICKS = 240L;
    private static final int MINION_LIFETIME_TICKS = 360;
    private static final int MINION_COUNT = 2;

    private int cooldownLeft;
    private final List<Minion> minions = new ArrayList<>();

    @Override
    public String id() {
        return ID;
    }

    @Override
    public long cooldownTicks() {
        return BASE_COOLDOWN_TICKS;
    }

    @Override
    public boolean canCast(RealiteBoss boss, AbilityContext ctx) {
        return boss != null && boss.isAlive();
    }

    @Override
    public void cast(RealiteBoss boss, AbilityContext ctx) {
        // Управляем кастом через tick().
    }

    @Override
    public void tick(RealiteBoss boss) {
        if (boss == null) {
            return;
        }
        LivingEntity entity = boss.getEntity();
        if (entity == null || entity.isDead()) {
            cleanup(boss);
            return;
        }

        updateMinions();

        if (!isActivePhase(boss.getPhase())) {
            return;
        }

        if (cooldownLeft > 0) {
            cooldownLeft--;
            return;
        }

        summon(entity);
        cooldownLeft = (int) BASE_COOLDOWN_TICKS;
    }

    private void summon(LivingEntity bossEntity) {
        World world = bossEntity.getWorld();
        Location base = bossEntity.getLocation();
        for (int i = 0; i < MINION_COUNT; i++) {
            double angle = (Math.PI * 2) * (i / (double) MINION_COUNT);
            Vector offset = new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(2.2);
            Location spawn = base.clone().add(offset);
            Entity spawned = world.spawnEntity(spawn, EntityType.STRAY);
            if (spawned instanceof Mob mob) {
                mob.setPersistent(false);
                mob.customName(bossEntity.customName());
                minions.add(new Minion(mob.getUniqueId(), MINION_LIFETIME_TICKS));
            } else {
                spawned.remove();
            }
        }
        world.playSound(base, Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.3f);
    }

    private void updateMinions() {
        Iterator<Minion> iterator = minions.iterator();
        while (iterator.hasNext()) {
            Minion minion = iterator.next();
            minion.ticksLeft--;
            Entity entity = minion.resolve();
            if (!(entity instanceof LivingEntity living) || living.isDead() || minion.ticksLeft <= 0) {
                if (entity != null) {
                    entity.remove();
                }
                iterator.remove();
            }
        }
    }

    private boolean isActivePhase(BossPhase phase) {
        if (phase == null) {
            return false;
        }
        return "phase_3".equalsIgnoreCase(phase.id()) || "3".equalsIgnoreCase(phase.id());
    }

    @Override
    public void cleanup(RealiteBoss boss) {
        for (Minion minion : minions) {
            Entity entity = minion.resolve();
            if (entity != null) {
                entity.remove();
            }
        }
        minions.clear();
    }

    private static final class Minion {
        private final UUID id;
        private int ticksLeft;

        private Minion(UUID id, int ticksLeft) {
            this.id = id;
            this.ticksLeft = ticksLeft;
        }

        private Entity resolve() {
            return org.bukkit.Bukkit.getEntity(id);
        }
    }
}
