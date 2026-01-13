package ru.realite.classes.service;

import java.util.Optional;
import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassLevelXpData;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;

/**
 * Рассчитывает прогресс уровня/XP класса.
 */
public final class ClassLevelXpService {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;

    public ClassLevelXpService(ClassService classService, ClassConfigRepository classConfig) {
        this.classService = classService;
        this.classConfig = classConfig;
    }

    public Optional<ClassLevelXpData> getLevelXp(Player player) {
        PlayerProfile profile = classService.getProfile(player);
        if (profile == null) {
            return Optional.empty();
        }
        int xpPerLevel = 100;
        if (profile.getClassId() != null) {
            var def = classConfig.get(profile.getClassId());
            if (def != null) {
                xpPerLevel = Math.max(1, def.xpPerLevel);
            }
        }

        long totalXp = profile.getClassXp();
        int currentXp = (int) (totalXp % xpPerLevel);
        int level = profile.getClassLevel();
        return Optional.of(new ClassLevelXpData(level, currentXp, xpPerLevel));
    }
}
