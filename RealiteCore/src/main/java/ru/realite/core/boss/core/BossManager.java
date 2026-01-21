package ru.realite.core.boss.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.DamageContext;
import ru.realite.core.boss.core.context.DeathContext;
import ru.realite.core.boss.core.context.SpawnContext;
import ru.realite.core.boss.data.BossConfigLoader;
import ru.realite.core.boss.ui.BossUIController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BossManager implements Listener {
    private final JavaPlugin plugin;
    private final BossConfigLoader configLoader;
    private final BossUIController uiController;
    private final Map<UUID, RealiteBoss> activeBosses = new HashMap<>();
    private final Map<UUID, UUID> entityToInstance = new HashMap<>();
    private final Map<UUID, Set<UUID>> visiblePlayers = new HashMap<>();
    private final int uiUpdateTicks;
    private BukkitTask task;
    private int tickCounter;

    public BossManager(JavaPlugin plugin, BossConfigLoader configLoader, BossUIController uiController,
            int uiUpdateTicks) {
        this.plugin = plugin;
        this.configLoader = configLoader;
        this.uiController = uiController;
        this.uiUpdateTicks = Math.max(1, uiUpdateTicks);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTicking();
    }

    public RealiteBoss spawn(String bossId, SpawnContext ctx) {
        RealiteBoss boss = configLoader.create(bossId);
        boss.spawn(ctx);
        registerBoss(boss);
        uiController.attach(boss);
        refreshVisibility(boss);
        return boss;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (RealiteBoss boss : activeBosses.values()) {
            uiController.detach(boss, DespawnReason.RESTART);
            boss.despawn(DespawnReason.RESTART);
        }
        activeBosses.clear();
        entityToInstance.clear();
        visiblePlayers.clear();
    }

    private void registerBoss(RealiteBoss boss) {
        activeBosses.put(boss.instanceId(), boss);
        LivingEntity entity = boss.getEntity();
        if (entity != null) {
            entityToInstance.put(entity.getUniqueId(), boss.instanceId());
        }
    }

    private void startTicking() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 1L, 1L);
    }

    private void tickAll() {
        tickCounter++;
        for (RealiteBoss boss : new HashSet<>(activeBosses.values())) {
            if (!boss.isAlive()) {
                cleanupBoss(boss, DespawnReason.DEATH);
                continue;
            }
            boss.tick();
            if (tickCounter % uiUpdateTicks == 0) {
                uiController.update(boss);
            }
        }
    }

    private void cleanupBoss(RealiteBoss boss, DespawnReason reason) {
        uiController.detach(boss, reason);
        boss.despawn(reason);
        activeBosses.remove(boss.instanceId());
        if (boss.getEntity() != null) {
            entityToInstance.remove(boss.getEntity().getUniqueId());
        }
        visiblePlayers.remove(boss.instanceId());
    }

    private void refreshVisibility(RealiteBoss boss) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateVisibilityForPlayer(boss, player);
        }
    }

    private void updateVisibilityForPlayer(RealiteBoss boss, Player player) {
        LivingEntity entity = boss.getEntity();
        if (entity == null || player.getWorld() != entity.getWorld()) {
            hideBossFromPlayer(boss, player);
            return;
        }
        double range = boss.bossBarRange();
        if (player.getLocation().distanceSquared(entity.getLocation()) <= range * range) {
            showBossToPlayer(boss, player);
        } else {
            hideBossFromPlayer(boss, player);
        }
    }

    private void showBossToPlayer(RealiteBoss boss, Player player) {
        visiblePlayers.computeIfAbsent(boss.instanceId(), id -> new HashSet<>()).add(player.getUniqueId());
        uiController.showTo(boss, player);
    }

    private void hideBossFromPlayer(RealiteBoss boss, Player player) {
        Set<UUID> viewers = visiblePlayers.get(boss.instanceId());
        if (viewers != null) {
            viewers.remove(player.getUniqueId());
        }
        uiController.hideFrom(boss, player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        UUID instanceId = entityToInstance.get(event.getEntity().getUniqueId());
        if (instanceId == null) {
            return;
        }
        RealiteBoss boss = activeBosses.get(instanceId);
        if (boss == null) {
            return;
        }

        Optional<LivingEntity> damager = Optional.empty();
        if (event instanceof EntityDamageByEntityEvent byEntity) {
            if (byEntity.getDamager() instanceof LivingEntity living) {
                damager = Optional.of(living);
            }
        }
        boss.onDamage(new DamageContext(event, damager));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        UUID instanceId = entityToInstance.get(event.getEntity().getUniqueId());
        if (instanceId == null) {
            return;
        }
        RealiteBoss boss = activeBosses.get(instanceId);
        if (boss == null) {
            return;
        }

        Optional<LivingEntity> killer = Optional.ofNullable(event.getEntity().getKiller());
        boss.onDeath(new DeathContext(event, killer));
        cleanupBoss(boss, DespawnReason.DEATH);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity ent : event.getChunk().getEntities()) {
            if (!(ent instanceof LivingEntity living)) {
                continue;
            }
            UUID instanceId = entityToInstance.get(living.getUniqueId());
            if (instanceId == null) {
                continue;
            }
            RealiteBoss boss = activeBosses.get(instanceId);
            if (boss != null) {
                cleanupBoss(boss, DespawnReason.CHUNK_UNLOAD);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Оптимизация: PlayerMoveEvent срабатывает даже при повороте головы.
        // Если игрок не сменил блок — видимость не пересчитываем.
        if (event.getTo() == null) {
            return;
        }

        var from = event.getFrom();
        var to = event.getTo();

        if (from.getWorld() == to.getWorld()
                && from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Доп. мелкая оптимизация: работаем только с боссами в мире игрока (и у которых
        // есть entity).
        for (RealiteBoss boss : new HashSet<>(activeBosses.values())) {
            LivingEntity e = boss.getEntity();
            if (e == null || e.getWorld() != player.getWorld()) {
                // если босс в другом мире — гарантированно скрываем (на случай телепорта/смены
                // мира)
                hideBossFromPlayer(boss, player);
                continue;
            }
            updateVisibilityForPlayer(boss, player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (RealiteBoss boss : activeBosses.values()) {
            hideBossFromPlayer(boss, player);
        }
    }
}
