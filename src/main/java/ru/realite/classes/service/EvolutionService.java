package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;

public class EvolutionService {

    private final ClassConfigRepository classConfig;
    private final String changePermission;

    public EvolutionService(ClassConfigRepository classConfig, String changePermission) {
        this.classConfig = classConfig;
        this.changePermission = changePermission;
    }

    public boolean hasChangePermission(Player player) {
        return player.hasPermission(changePermission);
    }

    public String getFirstEvolutionId(ClassId classId) {
        var def = classConfig.get(classId);
        if (def == null) return null;
        var first = def.firstEvolution();
        return first == null ? null : first.id;
    }

    public String getFinalEvolutionId(ClassId classId) {
        var def = classConfig.get(classId);
        if (def == null) return null;
        var fin = def.finalEvolution();
        return fin == null ? null : fin.id;
    }

    public boolean isFinalEvolution(PlayerProfile profile) {
        if (profile == null || !profile.hasClass()) return false;

        String fin = getFinalEvolutionId(profile.getClassId());
        if (fin == null) return false;

        String cur = profile.getEvolution();
        if (cur == null) return false;

        return fin.equalsIgnoreCase(cur);
    }

    /**
     * Идеальное правило смены класса:
     * - если класса нет -> это первичный выбор, разрешаем
     * - иначе -> perm ИЛИ финальная эволюция
     */
    public boolean canChangeClass(Player player, PlayerProfile profile) {
        if (profile == null || !profile.hasClass()) return true;
        return hasChangePermission(player) || isFinalEvolution(profile);
    }
}
