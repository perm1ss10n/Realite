package ru.realite.core.boss.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
import ru.realite.core.boss.data.BossDefinition;
import ru.realite.core.boss.impl.BossFirst;
import ru.realite.core.boss.impl.BossSecond;
import ru.realite.core.boss.ui.BossUIController;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class BossManager implements Listener {
    private final JavaPlugin plugin;
    private final BossRegistry registry;
    private final BossAbilityRegistry abilityRegistry;
    private final BossUIController uiController;

    private final Map<UUID, RealiteBoss> activeBosses = new HashMap<>();
    private final Map<UUID, UUID> entityToInstance = new HashMap<>();
    private final Map<UUID, Set<UUID>> visiblePlayers = new HashMap<>();

    private final int uiUpdateTicks;
    private BukkitTask task;
    private int tickCounter;

    public BossManager(JavaPlugin plugin,
            BossRegistry registry,
            BossAbilityRegistry abilityRegistry,
            BossUIController uiController,
            int uiUpdateTicks) {
        this.plugin = plugin;
        this.registry = registry;
        this.abilityRegistry = abilityRegistry;
        this.uiController = uiController;
        this.uiUpdateTicks = Math.max(1, uiUpdateTicks);

        BossAbilityDefaults.registerDefaults(abilityRegistry);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        startTicking();
    }

    public RealiteBoss spawn(String bossId, SpawnContext ctx) {
        BossDefinition definition = registry.requireDefinition(bossId);
        int maxInstances = definition.maxActiveInstances();
        if (maxInstances > 0 && countActiveInstances(bossId) >= maxInstances) {
            throw new IllegalStateException("Boss " + bossId + " reached maxActiveInstances: " + maxInstances);
        }

        RealiteBoss boss;
        if (BossFirst.ID.equals(bossId)) {
            boss = new BossFirst(definition, abilityRegistry);
        } else if (BossSecond.ID.equals(bossId)) {
            boss = new BossSecond(definition, abilityRegistry);
        } else {
            boss = new ConfigurableBoss(definition, abilityRegistry);
        }

        boss.spawn(ctx);
        registerBoss(boss);

        uiController.attach(boss);
        refreshVisibility(boss);

        return boss;
    }

    private int countActiveInstances(String bossId) {
        int count = 0;
        for (RealiteBoss boss : activeBosses.values()) {
            if (boss.bossId().equalsIgnoreCase(bossId)) {
                count++;
            }
        }
        return count;
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        for (RealiteBoss boss : new HashSet<>(activeBosses.values())) {
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

        LivingEntity entity = boss.getEntity();
        if (entity != null) {
            entityToInstance.remove(entity.getUniqueId());
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
        if (entity.getLocation().distanceSquared(player.getLocation()) <= range * range) {
            showBossToPlayer(boss, player);
        } else {
            hideBossFromPlayer(boss, player);
        }
    }

    private void showBossToPlayer(RealiteBoss boss, Player player) {
        visiblePlayers.computeIfAbsent(boss.instanceId(), id -> new HashSet<>())
                .add(player.getUniqueId());
        uiController.showTo(boss, player);
    }

    private void hideBossFromPlayer(RealiteBoss boss, Player player) {
        Set<UUID> viewers = visiblePlayers.get(boss.instanceId());
        if (viewers == null || !viewers.remove(player.getUniqueId())) {
            return;
        }
        uiController.hideFrom(boss, player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) {
            return;
        }

        UUID instanceId = entityToInstance.get(entity.getUniqueId());
        if (instanceId == null) {
            return;
        }

        RealiteBoss boss = activeBosses.get(instanceId);
        if (boss == null) {
            return;
        }

        Optional<LivingEntity> damager = Optional.empty();

        if (event instanceof EntityDamageByEntityEvent e) {
            Entity d = e.getDamager();

            // Прямой урон (игрок/моб)
            if (d instanceof LivingEntity le) {
                damager = Optional.of(le);
            }
            // Урон снарядом (стрелы/трезубец/и т.п.)
            else if (d instanceof Projectile p && p.getShooter() instanceof LivingEntity le) {
                damager = Optional.of(le);
            }
        }

        boss.onDamage(new DamageContext(event, damager));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        UUID instanceId = entityToInstance.get(entity.getUniqueId());
        if (instanceId == null) {
            return;
        }

        RealiteBoss boss = activeBosses.get(instanceId);
        if (boss == null) {
            return;
        }

        boss.onDeath(new DeathContext(event, Optional.ofNullable(entity.getKiller())));
        cleanupBoss(boss, DespawnReason.DEATH);
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            UUID instanceId = entityToInstance.get(entity.getUniqueId());
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
        if (event.getTo() == null) {
            return;
        }

        Player player = event.getPlayer();

        if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            for (RealiteBoss boss : new HashSet<>(activeBosses.values())) {
                hideBossFromPlayer(boss, player);
            }
            return;
        }

        for (RealiteBoss boss : activeBosses.values()) {
            updateVisibilityForPlayer(boss, player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        for (RealiteBoss boss : activeBosses.values()) {
            hideBossFromPlayer(boss, player);
        }
    }
}
