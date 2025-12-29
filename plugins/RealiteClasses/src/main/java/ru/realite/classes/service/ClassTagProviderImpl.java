package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.core.api.classes.ClassTag;
import ru.realite.core.api.classes.ClassTagProvider;

import java.util.Objects;

public final class ClassTagProviderImpl implements ClassTagProvider {

    private final ClassService classService;
    private final EvolutionService evolutionService;
    private final ClassConfigRepository classConfig;

    public ClassTagProviderImpl(ClassService classService,
                                EvolutionService evolutionService,
                                ClassConfigRepository classConfig) {
        this.classService = Objects.requireNonNull(classService, "classService");
        this.evolutionService = Objects.requireNonNull(evolutionService, "evolutionService");
        this.classConfig = Objects.requireNonNull(classConfig, "classConfig");
    }

    @Override
    public ClassTag getTag(Player player) {
        PlayerProfile profile = classService.getProfile(player);
        ClassId classId = resolveClassId(profile);
        String displayName = resolveClassName(classId);
        int stage = resolveEvolutionStage(profile);
        return new ClassTag(displayName, stage);
    }

    private ClassId resolveClassId(PlayerProfile profile) {
        if (profile != null && profile.hasClass()) {
            return profile.getClassId();
        }
        return ClassId.WANDERER;
    }

    private String resolveClassName(ClassId classId) {
        if (classId == null) {
            return "Unknown";
        }
        ClassConfigRepository.ClassDef def = classConfig.get(classId);
        if (def != null && def.name != null && !def.name.isBlank()) {
            return def.name;
        }
        return classId.name();
    }

    private int resolveEvolutionStage(PlayerProfile profile) {
        int stage = evolutionService.getEvolutionNumber(profile);
        return Math.max(stage, 1);
    }
}
