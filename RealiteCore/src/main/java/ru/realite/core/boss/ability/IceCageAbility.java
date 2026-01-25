package ru.realite.core.boss.ability;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossAbilityCleanup;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.AbilityContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class IceCageAbility implements BossAbility, BossAbilityCleanup {
    public static final String ID = "ice_cage";

    private static final long BASE_COOLDOWN_TICKS = 180L;
    private static final int CAGE_DURATION_TICKS = 70;
    private static final double RANGE = 16.0;

    private int cooldownLeft;
    private final List<IceCage> cages = new ArrayList<>();

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

        updateCages();

        if (!isActivePhase(boss.getPhase())) {
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

        spawnCage(target);
        cooldownLeft = (int) BASE_COOLDOWN_TICKS;
    }

    private void updateCages() {
        Iterator<IceCage> iterator = cages.iterator();
        while (iterator.hasNext()) {
            IceCage cage = iterator.next();
            cage.ticksLeft--;
            if (cage.ticksLeft <= 0) {
                cage.restore();
                iterator.remove();
            } else {
                cage.pulse();
            }
        }
    }

    private void spawnCage(Player target) {
        World world = target.getWorld();
        Vector center = target.getLocation().getBlock().getLocation().toVector().add(new Vector(0.5, 0.0, 0.5));
        IceCage cage = new IceCage(world, center, CAGE_DURATION_TICKS);
        cage.build();
        cages.add(cage);

        world.playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.8f);
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

    private boolean isActivePhase(BossPhase phase) {
        if (phase == null) {
            return false;
        }
        return "phase_2".equalsIgnoreCase(phase.id()) || "phase_3".equalsIgnoreCase(phase.id())
                || "2".equalsIgnoreCase(phase.id()) || "3".equalsIgnoreCase(phase.id());
    }

    @Override
    public void cleanup(RealiteBoss boss) {
        for (IceCage cage : cages) {
            cage.restore();
        }
        cages.clear();
    }

    private static final class IceCage {
        private final World world;
        private final Vector center;
        private final List<BlockSnapshot> snapshots = new ArrayList<>();
        private int ticksLeft;

        private IceCage(World world, Vector center, int ticksLeft) {
            this.world = world;
            this.center = center;
            this.ticksLeft = ticksLeft;
        }

        private void build() {
            int baseX = center.getBlockX();
            int baseY = center.getBlockY();
            int baseZ = center.getBlockZ();

            for (int y = 0; y <= 2; y++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                        boolean roof = y == 2 && Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                        if (!edge && !roof) {
                            continue;
                        }
                        Block block = world.getBlockAt(baseX + dx, baseY + y, baseZ + dz);
                        if (!block.isPassable() && block.getType() != Material.AIR) {
                            continue;
                        }
                        BlockData original = block.getBlockData();
                        block.setType(Material.PACKED_ICE, false);
                        snapshots.add(new BlockSnapshot(block, original));
                    }
                }
            }
        }

        private void pulse() {
            world.spawnParticle(
                    Particle.SNOWFLAKE,
                    center.getX(),
                    center.getY() + 1.2,
                    center.getZ(),
                    6,
                    0.4,
                    0.6,
                    0.4,
                    0.0);
        }

        private void restore() {
            for (BlockSnapshot snapshot : snapshots) {
                snapshot.restore();
            }
            snapshots.clear();
        }
    }

    private static final class BlockSnapshot {
        private final Block block;
        private final BlockData data;

        private BlockSnapshot(Block block, BlockData data) {
            this.block = block;
            this.data = data;
        }

        private void restore() {
            block.setBlockData(data, false);
        }
    }
}
