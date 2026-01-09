package ru.realite.magic.region;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.effect.BalanceModifiers;
import ru.realite.magic.integration.city.CityBridge;
import ru.realite.magic.integration.city.RegionInfo;
import ru.realite.magic.integration.city.RegionType;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.SpellDefinition;

public final class RegionRuleService {

    private final JavaPlugin plugin;
    private final CityBridge cityBridge;

    public RegionRuleService(JavaPlugin plugin, CityBridge cityBridge) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.cityBridge = Objects.requireNonNull(cityBridge, "cityBridge");
    }

    public CastPolicy castPolicy(Player player, SpellDefinition spell, Location location) {
        if (!regionsEnabled()) {
            return CastPolicy.allow();
        }
        RegionContext context = context(location);
        RuleConfig rule = resolveRule(context);
        if (!rule.allowCast()) {
            String reasonKey = rule.messageKey() == null ? "magic.region.denied.default" : rule.messageKey();
            return CastPolicy.deny(reasonKey, java.util.Map.of());
        }
        MagicSchool school = spell == null ? null : spell.school();
        if (!rule.isSchoolAllowed(school)) {
            return CastPolicy.deny("magic.region.denied.school", java.util.Map.of());
        }
        return CastPolicy.allow();
    }

    public String regionId(Location location) {
        if (!regionsEnabled()) {
            return null;
        }
        RegionContext context = context(location);
        return context.regionId();
    }

    public BalanceModifiers regionModifiers(Player player, SpellDefinition spell, Location location) {
        if (!regionsEnabled()) {
            return BalanceModifiers.identity();
        }
        RegionContext context = context(location);
        RuleConfig rule = resolveRule(context);
        BalanceModifiers base = rule.modifiers();
        BalanceModifiers schoolModifiers = schoolModifiers(context.regionId(), spell == null ? null : spell.school());
        return new BalanceModifiers(
                base.damageMultiplier() * schoolModifiers.damageMultiplier(),
                base.manaMultiplier() * schoolModifiers.manaMultiplier(),
                base.cooldownMultiplier() * schoolModifiers.cooldownMultiplier());
    }

    private boolean regionsEnabled() {
        return plugin.getConfig().getBoolean("regions.enabled", true);
    }

    private RegionContext context(Location location) {
        String worldName = location == null || location.getWorld() == null
                ? null
                : location.getWorld().getName();
        if (!cityBridge.isAvailable() || location == null) {
            return new RegionContext(worldName, null, null);
        }
        Optional<RegionInfo> info = cityBridge.regionAt(location);
        if (info.isEmpty()) {
            return new RegionContext(worldName, null, null);
        }
        RegionInfo regionInfo = info.get();
        return new RegionContext(worldName, regionInfo.regionId(), regionInfo.type());
    }

    private RuleConfig resolveRule(RegionContext context) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection regionConfig = config.getConfigurationSection("regions");
        if (regionConfig == null) {
            return RuleConfig.defaultRule();
        }
        ConfigurationSection section = null;
        if (context.regionId() != null) {
            section = regionConfig.getConfigurationSection("overrides." + context.regionId());
        }
        if (section == null && context.worldName() != null) {
            section = regionConfig.getConfigurationSection("byWorld." + context.worldName());
        }
        if (section == null && context.regionType() != null) {
            section = regionConfig.getConfigurationSection("byType." + context.regionType().name());
        }
        if (section == null) {
            section = regionConfig.getConfigurationSection("default");
        }
        if (section == null) {
            return RuleConfig.defaultRule();
        }
        return RuleConfig.fromSection(section);
    }

    private BalanceModifiers schoolModifiers(String regionId, MagicSchool school) {
        if (regionId == null || school == null) {
            return BalanceModifiers.identity();
        }
        String path = "regions.schoolModifiers." + regionId + "." + school.name();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            return BalanceModifiers.identity();
        }
        return readModifiers(section);
    }

    private BalanceModifiers readModifiers(ConfigurationSection section) {
        double damage = section.getDouble("damageMultiplier", 1.0);
        double mana = section.getDouble("manaMultiplier", 1.0);
        double cooldown = section.getDouble("cooldownMultiplier", 1.0);
        return new BalanceModifiers(damage, mana, cooldown);
    }

    private record RegionContext(String worldName, String regionId, RegionType regionType) {
    }

    private record RuleConfig(boolean allowCast,
                              String messageKey,
                              Set<MagicSchool> schoolWhitelist,
                              Set<MagicSchool> schoolBlacklist,
                              BalanceModifiers modifiers) {
        static RuleConfig defaultRule() {
            return new RuleConfig(true, null, Set.of(), Set.of(), BalanceModifiers.identity());
        }

        static RuleConfig fromSection(ConfigurationSection section) {
            boolean allowCast = section.getBoolean("allowCast", true);
            String messageKey = section.getString("messageKey");
            Set<MagicSchool> whitelist = parseSchools(section, "schoolWhitelist");
            Set<MagicSchool> blacklist = parseSchools(section, "schoolBlacklist");
            BalanceModifiers modifiers = readModifiers(section.getConfigurationSection("modifiers"));
            return new RuleConfig(allowCast, messageKey, whitelist, blacklist, modifiers);
        }

        private static Set<MagicSchool> parseSchools(ConfigurationSection section, String key) {
            Set<MagicSchool> schools = new HashSet<>();
            if (section == null) {
                return schools;
            }
            for (String raw : section.getStringList(key)) {
                MagicSchool school = MagicSchool.fromString(raw);
                if (school != null) {
                    schools.add(school);
                }
            }
            return schools;
        }

        private static BalanceModifiers readModifiers(ConfigurationSection section) {
            if (section == null) {
                return BalanceModifiers.identity();
            }
            double damage = section.getDouble("damageMultiplier", 1.0);
            double mana = section.getDouble("manaMultiplier", 1.0);
            double cooldown = section.getDouble("cooldownMultiplier", 1.0);
            return new BalanceModifiers(damage, mana, cooldown);
        }

        boolean isSchoolAllowed(MagicSchool school) {
            MagicSchool resolved = school == null ? MagicSchool.NONE : school;
            if (!schoolWhitelist.isEmpty() && !schoolWhitelist.contains(resolved)) {
                return false;
            }
            if (!schoolBlacklist.isEmpty() && schoolBlacklist.contains(resolved)) {
                return false;
            }
            return true;
        }
    }
}
