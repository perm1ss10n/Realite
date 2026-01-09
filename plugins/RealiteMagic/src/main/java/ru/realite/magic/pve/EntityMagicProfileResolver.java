package ru.realite.magic.pve;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.school.MagicSchool;

public final class EntityMagicProfileResolver {

    private final EntityMagicProfile defaultProfile;
    private final Map<EntityType, EntityMagicProfile> byEntityType;
    private final Map<String, EntityMagicProfile> byTag;
    private final NamespacedKey profileKey;

    public EntityMagicProfileResolver(JavaPlugin plugin, FileConfiguration config, boolean logWarnings) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(config, "config");
        Logger logger = logWarnings ? plugin.getLogger() : null;
        this.profileKey = new NamespacedKey(plugin, "pve_profile");
        ConfigurationSection pveSection = config.getConfigurationSection("pve");
        ConfigurationSection defaultsSection = pveSection == null
                ? null
                : pveSection.getConfigurationSection("defaults");
        Map<MagicSchool, Double> baseSchool = defaultSchoolMap();
        Map<MagicSchool, Double> defaultSchool = readSchoolMultipliers(defaultsSection == null
                ? null
                : defaultsSection.getConfigurationSection("schoolDamageTaken"), baseSchool, logger, "pve.defaults");
        double damageTakenMultiplier = readDouble(defaultsSection, "damageTakenMultiplier", 1.0, logger, "pve.defaults");
        Set<String> immuneEffects = readStringSet(defaultsSection, "immuneEffects", false, logger, "pve.defaults");
        Set<String> immunePotionEffects = readStringSet(defaultsSection, "immunePotionEffects", true, logger, "pve.defaults");
        boolean immuneKnockback = readBoolean(defaultsSection, "immuneKnockback", false);
        boolean immunePull = readBoolean(defaultsSection, "immunePull", false);
        boolean immuneTeleport = readBoolean(defaultsSection, "immuneTeleport", false);
        int maxHitsPerWindow = readInt(defaultsSection, "maxHitsPerWindow", 0, logger, "pve.defaults");
        int windowMs = readInt(defaultsSection, "windowMs", 0, logger, "pve.defaults");
        defaultProfile = new EntityMagicProfile(
                damageTakenMultiplier,
                defaultSchool,
                immuneEffects,
                immunePotionEffects,
                immuneKnockback,
                immunePull,
                immuneTeleport,
                maxHitsPerWindow,
                windowMs);
        byTag = readByTag(pveSection == null ? null : pveSection.getConfigurationSection("byTag"),
                defaultProfile,
                logger);
        byEntityType = readByEntityType(pveSection == null ? null : pveSection.getConfigurationSection("byEntityType"),
                defaultProfile,
                logger);
    }

    public EntityMagicProfile resolve(LivingEntity entity) {
        if (entity == null) {
            return defaultProfile;
        }
        String taggedProfile = entity.getPersistentDataContainer().get(profileKey, PersistentDataType.STRING);
        if (taggedProfile != null) {
            String normalized = taggedProfile.trim().toLowerCase(Locale.ROOT);
            EntityMagicProfile profile = byTag.get(normalized);
            if (profile != null) {
                return profile;
            }
        }
        for (String tag : entity.getScoreboardTags()) {
            if (tag == null || tag.isBlank()) {
                continue;
            }
            EntityMagicProfile profile = byTag.get(tag.trim().toLowerCase(Locale.ROOT));
            if (profile != null) {
                return profile;
            }
        }
        EntityMagicProfile profile = byEntityType.get(entity.getType());
        return profile == null ? defaultProfile : profile;
    }

    private Map<EntityType, EntityMagicProfile> readByEntityType(ConfigurationSection section,
                                                                 EntityMagicProfile defaults,
                                                                 Logger logger) {
        Map<EntityType, EntityMagicProfile> result = new EnumMap<>(EntityType.class);
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            EntityType type;
            try {
                type = EntityType.valueOf(key.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                warn(logger, "[Magic PVE] Unknown entityType '" + key + "' in pve.byEntityType");
                continue;
            }
            ConfigurationSection entry = section.getConfigurationSection(key);
            EntityMagicProfile profile = readProfile(entry, defaults, logger, "pve.byEntityType." + key);
            result.put(type, profile);
        }
        return result;
    }

    private Map<String, EntityMagicProfile> readByTag(ConfigurationSection section,
                                                      EntityMagicProfile defaults,
                                                      Logger logger) {
        Map<String, EntityMagicProfile> result = new HashMap<>();
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            EntityMagicProfile profile = readProfile(entry, defaults, logger, "pve.byTag." + key);
            result.put(key.trim().toLowerCase(Locale.ROOT), profile);
        }
        return result;
    }

    private EntityMagicProfile readProfile(ConfigurationSection section,
                                            EntityMagicProfile defaults,
                                            Logger logger,
                                            String context) {
        if (section == null) {
            return defaults;
        }
        double damageTakenMultiplier = readDouble(section, "damageTakenMultiplier", defaults.damageTakenMultiplier(), logger, context);
        Map<MagicSchool, Double> schoolDamage = readSchoolMultipliers(
                section.getConfigurationSection("schoolDamageTaken"),
                defaults.schoolDamageTaken(),
                logger,
                context);
        Set<String> immuneEffects = mergeStringSet(defaults.immuneEffects(),
                readStringSet(section, "immuneEffects", false, logger, context));
        Set<String> immunePotionEffects = mergeStringSet(defaults.immunePotionEffects(),
                readStringSet(section, "immunePotionEffects", true, logger, context));
        boolean immuneKnockback = readBoolean(section, "immuneKnockback", defaults.immuneKnockback());
        boolean immunePull = readBoolean(section, "immunePull", defaults.immunePull());
        boolean immuneTeleport = readBoolean(section, "immuneTeleport", defaults.immuneTeleport());
        int maxHitsPerWindow = readInt(section, "maxHitsPerWindow", defaults.maxHitsPerWindow(), logger, context);
        int windowMs = readInt(section, "windowMs", defaults.windowMs(), logger, context);
        return new EntityMagicProfile(
                damageTakenMultiplier,
                schoolDamage,
                immuneEffects,
                immunePotionEffects,
                immuneKnockback,
                immunePull,
                immuneTeleport,
                maxHitsPerWindow,
                windowMs);
    }

    private Map<MagicSchool, Double> defaultSchoolMap() {
        Map<MagicSchool, Double> base = new EnumMap<>(MagicSchool.class);
        for (MagicSchool school : MagicSchool.values()) {
            base.put(school, 1.0);
        }
        return base;
    }

    private Map<MagicSchool, Double> readSchoolMultipliers(ConfigurationSection section,
                                                            Map<MagicSchool, Double> base,
                                                            Logger logger,
                                                            String context) {
        Map<MagicSchool, Double> result = new EnumMap<>(MagicSchool.class);
        result.putAll(base);
        if (section == null) {
            return result;
        }
        for (String key : section.getKeys(false)) {
            MagicSchool school = MagicSchool.fromString(key);
            if (school == null) {
                warn(logger, "[Magic PVE] Unknown school '" + key + "' in " + context + ".schoolDamageTaken");
                continue;
            }
            double value = section.getDouble(key, result.getOrDefault(school, 1.0));
            if (value < 0) {
                warn(logger, "[Magic PVE] Negative multiplier for " + school + " in " + context + ": " + value);
            }
            result.put(school, value);
        }
        return result;
    }

    private double readDouble(ConfigurationSection section,
                              String path,
                              double fallback,
                              Logger logger,
                              String context) {
        if (section == null || !section.isSet(path)) {
            return fallback;
        }
        double value = section.getDouble(path, fallback);
        if (value < 0) {
            warn(logger, "[Magic PVE] Negative multiplier for " + context + "." + path + ": " + value);
        }
        return value;
    }

    private int readInt(ConfigurationSection section,
                        String path,
                        int fallback,
                        Logger logger,
                        String context) {
        if (section == null || !section.isSet(path)) {
            return fallback;
        }
        int value = section.getInt(path, fallback);
        if (value < 0) {
            warn(logger, "[Magic PVE] Negative value for " + context + "." + path + ": " + value);
            return 0;
        }
        return value;
    }

    private boolean readBoolean(ConfigurationSection section, String path, boolean fallback) {
        if (section == null || !section.isSet(path)) {
            return fallback;
        }
        return section.getBoolean(path, fallback);
    }

    private Set<String> readStringSet(ConfigurationSection section,
                                      String path,
                                      boolean uppercase,
                                      Logger logger,
                                      String context) {
        if (section == null || !section.isSet(path)) {
            return Set.of();
        }
        Object raw = section.get(path);
        Set<String> result = new HashSet<>();
        if (raw instanceof String str) {
            String value = str.trim();
            if (!value.isBlank()) {
                result.add(formatString(value, uppercase));
            }
            return result;
        }
        if (raw instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                if (entry == null) {
                    continue;
                }
                String value = String.valueOf(entry).trim();
                if (value.isBlank()) {
                    continue;
                }
                result.add(formatString(value, uppercase));
            }
            return result;
        }
        warn(logger, "[Magic PVE] Invalid list for " + context + "." + path);
        return Set.of();
    }

    private String formatString(String value, boolean uppercase) {
        return uppercase
                ? value.trim().toUpperCase(Locale.ROOT)
                : value.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> mergeStringSet(Set<String> base, Set<String> extra) {
        if (extra == null || extra.isEmpty()) {
            return base;
        }
        Set<String> merged = new HashSet<>(base);
        merged.addAll(extra);
        return merged;
    }

    private void warn(Logger logger, String message) {
        if (logger == null) {
            return;
        }
        logger.warning(message);
    }
}
