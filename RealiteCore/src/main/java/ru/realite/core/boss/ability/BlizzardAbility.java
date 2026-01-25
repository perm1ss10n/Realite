package ru.realite.core.boss.ability;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.AbilityContext;

public final class BlizzardAbility implements BossAbility {
    public static final String ID = "blizzard";

    private static final long BASE_COOLDOWN_TICKS = 200L;
    private static final int DURATION_TICKS = 120;
    private static final int DAMAGE_INTERVAL = 20;
    private static final double DAMAGE = 2.0;
    private static final double RADIUS = 6.0;

    private int cooldownLeft;
    private int activeTicks;
    private int damageTick;

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
            activeTicks = 0;
            damageTick = 0;
            return;
        }

        if (activeTicks > 0) {
            tickBlizzard(entity);
            activeTicks--;
            if (activeTicks <= 0) {
                cooldownLeft = (int) BASE_COOLDOWN_TICKS;
            }
            return;
        }

        if (!isActivePhase(boss.getPhase())) {
            return;
        }

        if (cooldownLeft > 0) {
            cooldownLeft--;
            return;
        }

        if (!hasNearbyPlayers(entity)) {
            return;
        }

        activeTicks = DURATION_TICKS;
        damageTick = 0;
        entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 0.6f, 1.6f);
    }

    private void tickBlizzard(LivingEntity entity) {
        World world = entity.getWorld();
        world.spawnParticle(Particle.SNOWFLAKE, entity.getLocation(), 18, 2.4, 1.4, 2.4, 0.02);
        world.spawnParticle(Particle.CLOUD, entity.getLocation(), 6, 1.6, 0.6, 1.6, 0.01);

        damageTick++;
        if (damageTick % DAMAGE_INTERVAL == 0) {
            double radiusSq = RADIUS * RADIUS;
            for (Player player : world.getPlayers()) {
                if (player.isDead() || !player.getWorld().equals(world)) {
                    continue;
                }
                if (player.getLocation().distanceSquared(entity.getLocation()) > radiusSq) {
                    continue;
                }
                player.damage(DAMAGE, entity);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DAMAGE_INTERVAL + 10, 0, true, true, true));
            }
        }
    }

    private boolean hasNearbyPlayers(LivingEntity entity) {
        double radiusSq = RADIUS * RADIUS;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.isDead() || !player.getWorld().equals(entity.getWorld())) {
                continue;
            }
            if (player.getLocation().distanceSquared(entity.getLocation()) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private boolean isActivePhase(BossPhase phase) {
        if (phase == null) {
            return false;
        }
        return "phase_3".equalsIgnoreCase(phase.id()) || "3".equalsIgnoreCase(phase.id());
    }
}
