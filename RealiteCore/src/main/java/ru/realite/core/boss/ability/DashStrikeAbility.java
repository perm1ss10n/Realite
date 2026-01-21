package ru.realite.core.boss.ability;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.AbilityContext;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DashStrikeAbility implements BossAbility {
    public static final String ID = "dash_strike";

    private static final int TELEGRAPH_TICKS = 20;
    private static final int DASH_TICKS = 8;
    private static final double DASH_SPEED = 1.4;
    private static final double HIT_RADIUS = 2.6;
    private static final double DAMAGE = 6.0;

    // Базовый кд (если фаза не известна / фаза 1). Фаза 2 будет уменьшать кд в
    // логике.
    private static final long BASE_COOLDOWN_TICKS = 90L;

    private final Set<UUID> hitTargets = new HashSet<>();

    private int cooldownLeft;
    private Stage stage = Stage.IDLE;
    private int stageTicks;
    private UUID targetId;

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
        // Эта способность управляется через tick().
        // Если захочешь запуск "по требованию" — можно переводить в TELEGRAPH отсюда.
    }

    @Override
    public void tick(RealiteBoss boss) {
        if (boss == null) {
            return;
        }
        LivingEntity entity = boss.getEntity();
        if (entity == null || entity.isDead()) {
            reset();
            return;
        }

        switch (stage) {
            case IDLE -> tickIdle(boss, entity);
            case TELEGRAPH -> tickTelegraph(entity);
            case DASH -> tickDash(boss, entity);
        }
    }

    private void tickIdle(RealiteBoss boss, LivingEntity entity) {
        if (cooldownLeft > 0) {
            cooldownLeft--;
            return;
        }

        Player target = findTarget(entity);
        if (target == null) {
            return;
        }

        double distance = target.getLocation().distance(entity.getLocation());
        if (distance < 4.0 || distance > 20.0) {
            return;
        }

        this.targetId = target.getUniqueId();
        this.stage = Stage.TELEGRAPH;
        this.stageTicks = TELEGRAPH_TICKS;

        World world = entity.getWorld();
        world.playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1.1f, 1.2f);
    }

    private void tickTelegraph(LivingEntity entity) {
        World world = entity.getWorld();
        world.spawnParticle(Particle.CRIT, entity.getLocation().add(0, 1.0, 0), 12, 0.4, 0.6, 0.4, 0.0);

        stageTicks--;
        if (stageTicks <= 0) {
            startDash(entity);
        }
    }

    private void startDash(LivingEntity entity) {
        stage = Stage.DASH;
        stageTicks = DASH_TICKS;
        hitTargets.clear();

        Player target = resolveTarget(entity);
        Vector direction = (target == null)
                ? entity.getLocation().getDirection()
                : target.getLocation().toVector().subtract(entity.getLocation().toVector());

        if (direction.lengthSquared() < 0.0001) {
            direction = entity.getLocation().getDirection();
        }

        Vector velocity = direction.normalize().multiply(DASH_SPEED);
        velocity.setY(Math.min(0.6, velocity.getY() + 0.1));
        entity.setVelocity(velocity);

        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 0.9f, 0.9f);
    }

    private void tickDash(RealiteBoss boss, LivingEntity entity) {
        World world = entity.getWorld();
        world.spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation().add(0, 1.0, 0), 1, 0.2, 0.2, 0.2, 0.0);

        for (Player player : world.getPlayers()) {
            if (hitTargets.contains(player.getUniqueId())) {
                continue;
            }
            if (player.isDead() || !player.getWorld().equals(entity.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(entity.getLocation()) > HIT_RADIUS * HIT_RADIUS) {
                continue;
            }

            hitTargets.add(player.getUniqueId());
            player.damage(DAMAGE, entity);

            Vector knockback = player.getLocation().toVector()
                    .subtract(entity.getLocation().toVector())
                    .normalize()
                    .multiply(0.8);
            knockback.setY(0.35);
            player.setVelocity(knockback);
        }

        stageTicks--;
        if (stageTicks <= 0) {
            cooldownLeft = cooldownForPhase(boss.getPhase());
            stage = Stage.IDLE;
            targetId = null;
        }
    }

    private int cooldownForPhase(BossPhase phase) {
        if (phase == null) {
            return (int) BASE_COOLDOWN_TICKS;
        }
        // ВАЖНО: ты у себя фазы задаёшь как phase.id() из конфига.
        // Если у тебя id фаз = "1"/"2" — подправь условие под реальный формат.
        return "2".equalsIgnoreCase(phase.id()) || "phase_2".equalsIgnoreCase(phase.id())
                ? 60
                : (int) BASE_COOLDOWN_TICKS;
    }

    private Player findTarget(LivingEntity entity) {
        Player nearest = null;
        double nearestSq = Double.MAX_VALUE;

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
        return nearest;
    }

    private Player resolveTarget(LivingEntity entity) {
        if (targetId == null) {
            return null;
        }
        for (Player p : entity.getWorld().getPlayers()) {
            if (p.getUniqueId().equals(targetId)) {
                return p;
            }
        }
        return null;
    }

    private void reset() {
        stage = Stage.IDLE;
        stageTicks = 0;
        cooldownLeft = 0;
        hitTargets.clear();
        targetId = null;
    }

    private enum Stage {
        IDLE,
        TELEGRAPH,
        DASH
    }
}
