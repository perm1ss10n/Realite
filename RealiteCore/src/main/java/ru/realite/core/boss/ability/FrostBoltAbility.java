package ru.realite.core.boss.ability;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.AbilityContext;

public final class FrostBoltAbility implements BossAbility {
    public static final String ID = "frost_bolt";

    private static final long BASE_COOLDOWN_TICKS = 70L;
    private static final double RANGE = 18.0;
    private static final double DAMAGE = 5.0;
    private static final int SLOW_TICKS = 60;

    private int cooldownLeft;

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
            cooldownLeft = 0;
            return;
        }

        if (cooldownLeft > 0) {
            cooldownLeft--;
            return;
        }

        Player target = findTarget(entity);
        if (target == null) {
            return;
        }

        fireBolt(entity, target);
        cooldownLeft = (int) BASE_COOLDOWN_TICKS;
    }

    private void fireBolt(LivingEntity caster, Player target) {
        World world = caster.getWorld();
        Vector direction = target.getEyeLocation().toVector()
                .subtract(caster.getEyeLocation().toVector())
                .normalize();
        RayTraceResult result = world.rayTraceEntities(
                caster.getEyeLocation(),
                direction,
                RANGE,
                0.6,
                entity -> entity instanceof Player player && !player.isDead() && player.getWorld().equals(world));

        double maxDistance = RANGE;
        Player hit = null;
        if (result != null && result.getHitEntity() instanceof Player player) {
            hit = player;
            if (result.getHitPosition() != null) {
                maxDistance = result.getHitPosition().distance(caster.getEyeLocation().toVector());
            }
        }

        spawnBoltParticles(world, caster.getEyeLocation().toVector(), direction, maxDistance);
        world.playSound(caster.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0f, 0.8f);

        if (hit != null) {
            hit.damage(DAMAGE, caster);
            hit.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, SLOW_TICKS, 1, true, true, true));
            world.playSound(hit.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.9f, 1.1f);
        }
    }

    private void spawnBoltParticles(World world, Vector start, Vector direction, double distance) {
        double step = 0.5;
        int steps = (int) Math.ceil(distance / step);
        for (int i = 0; i <= steps; i++) {
            Vector point = start.clone().add(direction.clone().multiply(i * step));
            world.spawnParticle(Particle.SNOWFLAKE, point.getX(), point.getY(), point.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    private Player findTarget(LivingEntity entity) {
        Player nearest = null;
        double nearestSq = Double.MAX_VALUE;

        for (Player player : entity.getWorld().getPlayers()) {
            if (player.isDead() || !player.getWorld().equals(entity.getWorld())) {
                continue;
            }
            double distanceSq = player.getLocation().distanceSquared(entity.getLocation());
            if (distanceSq > RANGE * RANGE) {
                continue;
            }
            if (distanceSq < nearestSq) {
                nearestSq = distanceSq;
                nearest = player;
            }
        }
        return nearest;
    }
}
