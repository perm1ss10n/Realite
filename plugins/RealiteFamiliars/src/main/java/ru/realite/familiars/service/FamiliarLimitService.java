package ru.realite.familiars.service;

import org.bukkit.entity.Player;
import ru.realite.familiars.config.FamiliarLimits;
import ru.realite.familiars.config.FamiliarLimitsRepository;
import ru.realite.familiars.integration.classes.ClassTierInfo;
import ru.realite.familiars.integration.classes.ClassesBridge;

import java.util.Objects;
import java.util.OptionalInt;

public final class FamiliarLimitService {

    private final ClassesBridge classesBridge;
    private FamiliarLimitsRepository limitsRepository;

    public FamiliarLimitService(ClassesBridge classesBridge, FamiliarLimitsRepository limitsRepository) {
        this.classesBridge = Objects.requireNonNull(classesBridge, "classesBridge");
        this.limitsRepository = limitsRepository;
    }

    public void updateRepository(FamiliarLimitsRepository limitsRepository) {
        this.limitsRepository = limitsRepository;
    }

    public FamiliarLimitInfo resolveLimit(Player player) {
        FamiliarLimits limits = limitsRepository != null
                ? limitsRepository.limits()
                : new FamiliarLimits(1, java.util.Map.of());
        int limit = limits.defaultLimit();
        String source = "default";

        ClassTierInfo info = classesBridge.getActiveClassInfo(player);
        if (info != null) {
            OptionalInt classLimit = limits.getLimit(info.classId(), info.evolutionTier());
            if (classLimit.isPresent()) {
                limit = classLimit.getAsInt();
                source = "class:" + info.classId() + " tier:" + info.evolutionTier();
            }
        }

        return new FamiliarLimitInfo(limit, source);
    }
}
