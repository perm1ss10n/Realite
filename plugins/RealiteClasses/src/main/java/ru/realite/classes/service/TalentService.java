package ru.realite.classes.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.core.api.talents.TalentDefinition;
import ru.realite.core.api.talents.TalentProvider;
import ru.realite.core.api.talents.TalentRequirements;

public final class TalentService implements TalentProvider {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;

    public TalentService(ClassService classService, ClassConfigRepository classConfig) {
        this.classService = Objects.requireNonNull(classService, "classService");
        this.classConfig = Objects.requireNonNull(classConfig, "classConfig");
    }

    @Override
    public Set<String> activeTalents(Player player) {
        if (player == null) {
            return Set.of();
        }
        PlayerProfile profile = classService.getProfile(player);
        if (profile == null || !profile.hasClass()) {
            return Set.of();
        }
        Set<String> active = new HashSet<>();
        ClassId classId = profile.getClassId();
        String evolutionId = profile.getEvolution();
        for (TalentDefinition talent : classConfig.talents()) {
            if (isAvailable(talent, classId, evolutionId)) {
                active.add(talent.id());
            }
        }
        return active;
    }

    @Override
    public Optional<TalentDefinition> findTalent(String talentId) {
        if (talentId == null || talentId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(classConfig.getTalent(normalize(talentId)));
    }

    @Override
    public Collection<TalentDefinition> talents() {
        return classConfig.talents();
    }

    private boolean isAvailable(TalentDefinition talent, ClassId classId, String evolutionId) {
        if (talent == null) {
            return false;
        }
        TalentRequirements requirements = talent.requirements();
        if (requirements == null) {
            return true;
        }
        String requiredClass = requirements.classId();
        if (requiredClass != null) {
            ClassId required = ClassId.fromString(requiredClass);
            if (required == null || required != classId) {
                return false;
            }
        }
        String requiredEvolution = requirements.evolutionId();
        if (requiredEvolution != null) {
            if (evolutionId == null || !requiredEvolution.equalsIgnoreCase(evolutionId)) {
                return false;
            }
        }
        return true;
    }

    private String normalize(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
