package ru.realite.classes.listener;

import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.ProgressionService;
import ru.realite.classes.storage.XpConfigRepository;

public class ClassActionXpListener implements Listener {

    private final ClassService classService;
    private final ProgressionService progression;
    private final XpConfigRepository xp;

    public ClassActionXpListener(ClassService classService,
                                 ProgressionService progression,
                                 XpConfigRepository xp) {
        this.classService = classService;
        this.progression = progression;
        this.xp = xp;
    }

    // -------------------------
    // MINER: руда
    // -------------------------
    @EventHandler(ignoreCancelled = true)
    public void onOreBreak(BlockBreakEvent e) {
        if (!xp.isMinerEnabled()) return;

        Player p = e.getPlayer();
        var prof = classService.getProfile(p);
        if (prof == null || !prof.hasClass()) return;
        if (prof.getClassId() != ClassId.MINER) return;

        Material type = e.getBlock().getType();
        int add = xp.getMinerOreXp(type);
        if (add <= 0) return;

        progression.addXp(p, add);
    }

    // -------------------------
    // WARRIOR: убийства
    // ARCHER: убийства стрелой
    // -------------------------
    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent e) {
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        var prof = classService.getProfile(killer);
        if (prof == null || !prof.hasClass()) return;

        boolean isPlayerVictim = (e.getEntity() instanceof Player);

        // ARCHER: убийство стрелой
        if (prof.getClassId() == ClassId.ARCHER && xp.isArcherEnabled()) {
            if (e.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent dmg) {
                if (dmg.getDamager() instanceof Arrow arrow) {
                    if (arrow.getShooter() instanceof Player shooter
                            && shooter.getUniqueId().equals(killer.getUniqueId())) {

                        int add = isPlayerVictim ? xp.getArcherArrowKillPlayers() : xp.getArcherArrowKillMobs();
                        if (add > 0) progression.addXp(killer, add);
                        return;
                    }
                }
            }
        }

        // WARRIOR: обычные убийства
        if (prof.getClassId() == ClassId.WARRIOR && xp.isWarriorEnabled()) {
            int add = isPlayerVictim ? xp.getWarriorKillPlayers() : xp.getWarriorKillMobs();
            if (add > 0) progression.addXp(killer, add);
        }
    }

    // -------------------------
    // ALCHEMIST: забрал готовое зелье из варочной стойки
    // -------------------------
    @EventHandler(ignoreCancelled = true)
    public void onBrewTake(InventoryClickEvent e) {
        if (!xp.isAlchemistEnabled()) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;

        var prof = classService.getProfile(p);
        if (prof == null || !prof.hasClass()) return;
        if (prof.getClassId() != ClassId.ALCHEMIST) return;

        if (e.getView().getTopInventory().getType() != InventoryType.BREWING) return;

        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().equals(e.getView().getTopInventory())) return;

        ItemStack current = e.getCurrentItem();
        if (current == null) return;

        Material t = current.getType();
        boolean okType =
                (t == Material.POTION && xp.isAlchemistAllowNormal())
                        || (t == Material.SPLASH_POTION && xp.isAlchemistAllowSplash())
                        || (t == Material.LINGERING_POTION && xp.isAlchemistAllowLingering());

        if (!okType) return;
        if (!(current.getItemMeta() instanceof PotionMeta)) return;

        // простая защита от странных переносов
        if (e.getCursor() != null && e.getCursor().getType() != Material.AIR) return;

        int add = xp.getAlchemistPotionTakeXp();
        if (add <= 0) return;

        progression.addXp(p, add);
    }
}
