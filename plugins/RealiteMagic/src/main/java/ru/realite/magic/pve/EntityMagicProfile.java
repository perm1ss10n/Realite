package ru.realite.magic.pve;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import ru.realite.magic.school.MagicSchool;

public record EntityMagicProfile(
        double damageTakenMultiplier,
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

        // PotionEffectType implements Keyed in modern Bukkit/Paper
        NamespacedKey key = (type instanceof Keyed keyed) ? keyed.getKey() : null;
        if (key == null) {
            return false;
        }

        // Normalize the configured set to uppercase comparisons
        // Supported config formats:
        // 1) "SPEED" (key.value() -> "speed")
        // 2) "MINECRAFT:SPEED" or "minecraft:speed" (key.toString())
        String valueUpper = key.getKey().toUpperCase(Locale.ROOT); // e.g. SPEED
        String fullUpper = key.toString().toUpperCase(Locale.ROOT); // e.g. MINECRAFT:SPEED

        return immunePotionEffects.contains(valueUpper) || immunePotionEffects.contains(fullUpper);
    }
}