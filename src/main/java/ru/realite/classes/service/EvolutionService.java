package ru.realite.classes.service;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.EvolutionDef;
import ru.realite.classes.model.ItemAmount;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;

import java.util.List;

public class EvolutionService {

    private final ClassConfigRepository classConfig;
    private final String changePermission;

    public EvolutionService(ClassConfigRepository classConfig, String changePermission) {
        this.classConfig = classConfig;
        this.changePermission = changePermission;
    }

    public String evolve(Player player, PlayerProfile profile, EconomyService economy) {
        if (profile == null || !profile.hasClass())
            return "no-class";

        var next = getNextEvolution(profile);
        if (next == null)
            return "already-max";

        if (profile.getClassLevel() < next.requiredLevel)
            return "not-enough-level";

        // стоимость (деньги + предметы)
        if (next.costMoney > 0) {
            boolean ok = economy.withdraw(player, next.costMoney);
            if (!ok)
                return "no-money";
        }
        if (next.costItems != null && !next.costItems.isEmpty()) {
            if (!hasItems(player, next.costItems))
                return "no-items";
            takeItems(player, next.costItems);
        }

        // ап эволюции
        profile.setEvolution(next.id); // должен сбросить evolutionNotified внутри setEvolution, если ты так сделал
        profile.setEvolutionRewardTaken(false);

        // если это последняя эволюция — помечаем класс как пройденный до конца
        // (mastered)
        if (getNextEvolution(profile) == null) {
            profile.addMastered(profile.getClassId());
        }

        // награда
        if (next.rewardMoney > 0)
            economy.deposit(player, next.rewardMoney);
        if (next.rewardItems != null && !next.rewardItems.isEmpty())
            giveItems(player, next.rewardItems);

        return "ok";
    }

    private boolean hasItems(Player player, List<ItemAmount> items) {
        for (ItemAmount ia : items) {
            if (!player.getInventory().containsAtLeast(new ItemStack(ia.material()), ia.amount())) {
                return false;
            }
        }
        return true;
    }

    private void takeItems(Player player, List<ItemAmount> items) {
        for (ItemAmount ia : items) {
            int left = ia.amount();
            var inv = player.getInventory();
            for (int slot = 0; slot < inv.getSize(); slot++) {
                var stack = inv.getItem(slot);
                if (stack == null)
                    continue;
                if (stack.getType() != ia.material())
                    continue;

                int take = Math.min(left, stack.getAmount());
                stack.setAmount(stack.getAmount() - take);
                left -= take;

                if (stack.getAmount() <= 0)
                    inv.setItem(slot, null);
                if (left <= 0)
                    break;
            }
        }
    }

    private void giveItems(Player player, List<ItemAmount> items) {
        for (ItemAmount ia : items) {
            player.getInventory().addItem(new ItemStack(ia.material(), ia.amount()));
        }
    }

    public boolean hasChangePermission(Player player) {
        return player.hasPermission(changePermission);
    }

    public EvolutionDef getCurrentEvolution(PlayerProfile profile) {
        if (profile == null || !profile.hasClass())
            return null;

        var def = classConfig.get(profile.getClassId());
        if (def == null || def.evolutions == null || def.evolutions.isEmpty())
            return null;

        String curId = profile.getEvolution();
        if (curId == null || curId.isBlank())
            return def.evolutions.get(0);

        for (var e : def.evolutions) {
            if (e.id.equalsIgnoreCase(curId))
                return e;
        }
        return def.evolutions.get(0);
    }

    public int getEvolutionNumber(PlayerProfile profile) {
        if (profile == null || !profile.hasClass())
            return 1;

        var def = classConfig.get(profile.getClassId());
        if (def == null || def.evolutions == null || def.evolutions.isEmpty())
            return 1;

        String curId = profile.getEvolution();
        if (curId == null || curId.isBlank())
            return 1;

        for (int i = 0; i < def.evolutions.size(); i++) {
            if (def.evolutions.get(i).id.equalsIgnoreCase(curId)) {
                return i + 1; // I = 1
            }
        }
        return 1;
    }

    public EvolutionDef getNextEvolution(PlayerProfile profile) {
        if (profile == null || !profile.hasClass())
            return null;

        var def = classConfig.get(profile.getClassId());
        if (def == null || def.evolutions == null || def.evolutions.isEmpty())
            return null;

        String curId = profile.getEvolution();
        int idx = -1;

        if (curId != null && !curId.isBlank()) {
            for (int i = 0; i < def.evolutions.size(); i++) {
                if (def.evolutions.get(i).id.equalsIgnoreCase(curId)) {
                    idx = i;
                    break;
                }
            }
        }

        int nextIndex = idx + 1;
        if (nextIndex < 0)
            nextIndex = 0;
        if (nextIndex >= def.evolutions.size())
            return null;

        return def.evolutions.get(nextIndex);
    }

    public boolean canEvolve(PlayerProfile profile) {
        var next = getNextEvolution(profile);
        if (next == null)
            return false;
        return profile.getClassLevel() >= next.requiredLevel;
    }

    public String getFirstEvolutionId(ClassId classId) {
        var def = classConfig.get(classId);
        if (def == null)
            return null;
        var first = def.firstEvolution();
        return first == null ? null : first.id;
    }

    public String getFinalEvolutionId(ClassId classId) {
        var def = classConfig.get(classId);
        if (def == null)
            return null;
        var fin = def.finalEvolution();
        return fin == null ? null : fin.id;
    }

    public boolean isFinalEvolution(PlayerProfile profile) {
        if (profile == null || !profile.hasClass())
            return false;

        String fin = getFinalEvolutionId(profile.getClassId());
        if (fin == null)
            return false;

        String cur = profile.getEvolution();
        if (cur == null)
            return false;

        return fin.equalsIgnoreCase(cur);
    }

    public boolean canChangeClass(Player player, PlayerProfile profile) {
        if (profile == null || !profile.hasClass())
            return true;
        if (profile.isStarterClass())
            return true;
        return hasChangePermission(player) || isFinalEvolution(profile);
    }
}
