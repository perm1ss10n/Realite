package ru.realite.guilds.listener;

import java.util.List;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.projectiles.ProjectileSource;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.service.GuildService;
import ru.realite.guilds.service.GuildUpgradeEffectService;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildPveDamageListener implements Listener {

    private final GuildRepository repository;
    private final GuildService guildService;
    private final GuildUpgradeEffectService effectService;

    public GuildPveDamageListener(GuildRepository repository,
                                  GuildService guildService,
                                  GuildUpgradeEffectService effectService) {
        this.repository = repository;
        this.guildService = guildService;
        this.effectService = effectService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event);
        if (attacker == null) {
            return;
        }
        if (!effectService.isPveEffectEnabled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof Player) {
            return;
        }
        if (event.getEntity() instanceof ArmorStand) {
            return;
        }
        GuildMember member = repository.getMember(attacker.getUniqueId());
        if (member == null) {
            return;
        }
        String guildTag = member.tag();
        double multiplier = effectService.resolvePveDamageMultiplier(guildTag);
        if (multiplier <= 1.0d) {
            return;
        }
        if (effectService.shouldApplyPveInTerritoryOnly()) {
            Guild guild = guildService.findGuildByClaim(event.getEntity().getLocation());
            if (guild == null || !guild.tag().equalsIgnoreCase(guildTag)) {
                return;
            }
        }
        if (effectService.shouldApplyPveInWorldsOnly()) {
            List<String> allowed = effectService.getPveAllowedWorlds();
            if (!allowed.isEmpty() && !allowed.contains(event.getEntity().getWorld().getName())) {
                return;
            }
        }
        event.setDamage(event.getDamage() * multiplier);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) {
                return player;
            }
        }
        return null;
    }
}
