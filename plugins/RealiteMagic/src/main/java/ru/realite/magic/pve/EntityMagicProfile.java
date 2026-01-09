package ru.realite.magic.pve;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.potion.PotionEffectType;
import ru.realite.magic.school.MagicSchool;

public record EntityMagicProfile(double damageTakenMultiplier,
                                 Map<MagicSchool, Double> schoolDamageTaken,
                                 Set<String> immuneEffects,
                                 Set<String> immunePotionEffects,
                                 boolean immuneKnockback,
                                 boolean immunePull,
                                 boolean immuneTeleport,
                                 int maxHitsPerWindow,
                                 int windowMs) {

    public EntityMagicProfile {
        Objects.requireNonNull(schoolDamageTaken, "schoolDamageTaken");
        Objects.requireNonNull(immuneEffects, "immuneEffects");
        Objects.requireNonNull(immunePotionEffects, "immunePotionEffects");
        schoolDamageTaken = Map.copyOf(schoolDamageTaken);
        immuneEffects = Set.copyOf(immuneEffects);
        immunePotionEffects = Set.copyOf(immunePotionEffects);
    }

    public double schoolMultiplier(MagicSchool school) {
        if (school == null) {
            return 1.0;
        }
        return schoolDamageTaken.getOrDefault(school, 1.0);
    }

    public boolean isEffectImmune(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        return immuneEffects.contains(type.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isPotionImmune(PotionEffectType type) {
        if (type == null) {
            return false;
        }
        String name = type.getName();
        if (name == null || name.isBlank()) {
            return false;
        }
        return immunePotionEffects.contains(name.toUpperCase(Locale.ROOT));
    }
}
