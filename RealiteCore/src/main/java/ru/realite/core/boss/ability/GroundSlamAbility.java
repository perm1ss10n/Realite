package ru.realite.core.boss.ability;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.AbilityContext;

public final class GroundSlamAbility implements BossAbility {
    public static final String ID = "ground_slam";

    private static final int TELEGRAPH_TICKS = 25;
    private static final double DAMAGE = 7.0;

    private int cooldownTicks;
    private int telegraphTicks;

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
            cooldownTicks = 0;
            telegraphTicks = 0;
            return;
        }

        if (telegraphTicks > 0) {
            tickTelegraph(entity);
            telegraphTicks--;
            if (telegraphTicks == 0) {
                performSlam(boss, entity);
            }
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Player target = findNearby(entity);
        if (target == null) {
            return;
        }

        telegraphTicks = TELEGRAPH_TICKS;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 0.7f);
    }

    private void tickTelegraph(LivingEntity entity) {
        World world = entity.getWorld();
        world.spawnParticle(Particle.SMOKE_LARGE, entity.getLocation(), 6, 0.5, 0.2, 0.5, 0.01);
    }

    private void performSlam(RealiteBoss boss, LivingEntity entity) {
        World world = entity.getWorld();
        double radius = radiusForPhase(boss.getPhase());
        double radiusSq = radius * radius;

        world.spawnParticle(Particle.EXPLOSION, entity.getLocation(), 2, 0.2, 0.1, 0.2, 0.0);
        world.playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.8f);

        for (Player player : world.getPlayers()) {
            if (player.isDead() || !player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(entity.getLocation()) > radiusSq) {
                continue;
            }
            player.damage(DAMAGE, entity);
            Vector knockback = player.getLocation().toVector().subtract(entity.getLocation().toVector()).normalize().multiply(1.0);
            knockback.setY(0.45);
            player.setVelocity(knockback);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 1, true, true, true));
        }

        cooldownTicks = cooldownForPhase(boss.getPhase());
    }

    private Player findNearby(LivingEntity entity) {
        double nearestSq = Double.MAX_VALUE;
        Player nearest = null;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.isDead() || !player.getWorld().equals(entity.getWorld())) {
                continue;
            }
            double distanceSq = player.getLocation().distanceSquared(entity.getLocation());
            if (distanceSq < nearestSq) {
                nearestSq = distanceSq;
                nearest = player;
            }
        }
        if (nearest != null && nearestSq <= 64.0) {
            return nearest;
        }
        return null;
    }

    private int cooldownForPhase(BossPhase phase) {
        if (phase == null) {
            return 100;
        }
        return "phase_2".equalsIgnoreCase(phase.id()) ? 70 : 100;
    }

    private double radiusForPhase(BossPhase phase) {
        if (phase == null) {
            return 4.0;
        }
        return "phase_2".equalsIgnoreCase(phase.id()) ? 5.5 : 4.0;
    }
}
