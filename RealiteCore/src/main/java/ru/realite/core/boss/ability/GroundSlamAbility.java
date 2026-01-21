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

    private static final long BASE_COOLDOWN_TICKS = 100L;

    private int cooldownLeft;
    private int telegraphLeft;

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
        // Если захочешь запуск по событию — можно выставлять telegraphLeft отсюда.
    }

    @Override
    public void tick(RealiteBoss boss) {
        if (boss == null) {
            return;
        }
        LivingEntity entity = boss.getEntity();
        if (entity == null || entity.isDead()) {
            cooldownLeft = 0;
            telegraphLeft = 0;
            return;
        }

        // Телеграфирование перед ударом
        if (telegraphLeft > 0) {
            tickTelegraph(entity);
            telegraphLeft--;
            if (telegraphLeft == 0) {
                performSlam(boss, entity);
            }
            return;
        }

        // Кулдаун
        if (cooldownLeft > 0) {
            cooldownLeft--;
            return;
        }

        // Ищем цель (ближайшего игрока), чтобы не бить в пустоту
        Player target = findNearby(entity);
        if (target == null) {
            return;
        }

        telegraphLeft = TELEGRAPH_TICKS;
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 0.7f);
    }

    private void tickTelegraph(LivingEntity entity) {
        World world = entity.getWorld();
        world.spawnParticle(Particle.LARGE_SMOKE, entity.getLocation(), 6, 0.5, 0.2, 0.5, 0.01);
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

            Vector knockback = player.getLocation().toVector()
                    .subtract(entity.getLocation().toVector())
                    .normalize()
                    .multiply(1.0);
            knockback.setY(0.45);
            player.setVelocity(knockback);

            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, true, true));
        }

        cooldownLeft = cooldownForPhase(boss.getPhase());
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

        // 8 блоков (8^2 = 64)
        if (nearest != null && nearestSq <= 64.0) {
            return nearest;
        }
        return null;
    }

    private int cooldownForPhase(BossPhase phase) {
        if (phase == null) {
            return (int) BASE_COOLDOWN_TICKS;
        }
        // Поддержка двух вариантов id фазы: "2" или "phase_2"
        return "2".equalsIgnoreCase(phase.id()) || "phase_2".equalsIgnoreCase(phase.id())
                ? 70
                : (int) BASE_COOLDOWN_TICKS;
    }

    private double radiusForPhase(BossPhase phase) {
        if (phase == null) {
            return 4.0;
        }
        return "2".equalsIgnoreCase(phase.id()) || "phase_2".equalsIgnoreCase(phase.id())
                ? 5.5
                : 4.0;
    }
}
