package ru.realite.classes.service;

import java.util.Optional;
import org.bukkit.entity.Player;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.core.api.classes.ClassProfile;
import ru.realite.core.api.classes.ClassProfileProvider;

public final class ClassProfileProviderImpl implements ClassProfileProvider {

    private final ClassService classService;

    public ClassProfileProviderImpl(ClassService classService) {
        this.classService = classService;
    }

    @Override
    public Optional<ClassProfile> getProfile(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        PlayerProfile profile = classService.getProfile(player);
        if (profile == null) {
            return Optional.empty();
        }
        String classId = profile.getClassId() != null ? profile.getClassId().name() : null;
        String evolutionId = profile.getEvolution();
        return Optional.of(new ClassProfile(classId, evolutionId));
    }
}
