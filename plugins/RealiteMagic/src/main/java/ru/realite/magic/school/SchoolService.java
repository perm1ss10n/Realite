package ru.realite.magic.school;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.model.MageState;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.spell.SpellDefinition;

public final class SchoolService {

    private final JavaPlugin plugin;
    private final ClassesBridge classesBridge;
    private final MagicMessages messages;
    private final Function<Player, MageState> stateProvider;

    public SchoolService(JavaPlugin plugin,
                         ClassesBridge classesBridge,
                         MagicMessages messages,
                         Function<Player, MageState> stateProvider) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.classesBridge = Objects.requireNonNull(classesBridge, "classesBridge");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.stateProvider = Objects.requireNonNull(stateProvider, "stateProvider");
    }

    public SchoolModifiers modifiersFor(Player player, SpellDefinition spell) {
        if (!enabled() || player == null || spell == null) {
            return SchoolModifiers.identity();
        }
        MagicSchool school = spell.school();
        if (school == null || school == MagicSchool.NONE) {
            return SchoolModifiers.identity();
        }
        SchoolModifiers base = schoolModifiers("schools.modifiers." + school.name());
        if (!classesBridge.isAvailable()) {
            return base;
        }
        String classId = classesBridge.getActiveClassId(player);
        ConfigurationSection bonus = classBonusSection(classId);
        if (bonus == null) {
            return base;
        }
        MagicSchool bonusSchool = MagicSchool.fromString(bonus.getString("school"));
        if (bonusSchool == null || bonusSchool != school) {
            return base;
        }
        double damage = base.damageMultiplier() * bonus.getDouble("damageMultiplier", 1.0);
        double mana = base.manaMultiplier() * bonus.getDouble("manaMultiplier", 1.0);
        double cooldown = base.cooldownMultiplier() * bonus.getDouble("cooldownMultiplier", 1.0);
        return new SchoolModifiers(damage, mana, cooldown);
    }

    public CheckResult conflictReason(Player player, SpellDefinition spell) {
        if (!enabled() || player == null || spell == null) {
            return new CheckResult.Ok();
        }
        MagicSchool school = spell.school();
        if (school == null || school == MagicSchool.NONE) {
            return new CheckResult.Ok();
        }
        MageState state = stateProvider.apply(player);
        MagicSchool lastSchool = state.lastSchool();
        if (lastSchool == null || lastSchool == MagicSchool.NONE || lastSchool == school) {
            return new CheckResult.Ok();
        }
        long windowMs = (long) (conflictWindowSeconds() * 1000L);
        if (windowMs <= 0) {
            return new CheckResult.Ok();
        }
        if (System.currentTimeMillis() - state.lastSchoolTime() > windowMs) {
            return new CheckResult.Ok();
        }
        if (!isConflict(lastSchool, school)) {
            return new CheckResult.Ok();
        }
        String lastName = schoolName(lastSchool);
        String nextName = schoolName(school);
        return new CheckResult.Fail("magic.school.conflict",
                Map.of("a", lastName, "b", nextName, "other", lastName));
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("schools.enabled", false);
    }

    private double conflictWindowSeconds() {
        return plugin.getConfig().getDouble("schools.conflictWindowSeconds", 10.0);
    }

    private boolean isConflict(MagicSchool lastSchool, MagicSchool nextSchool) {
        FileConfiguration config = plugin.getConfig();
        List<Map<?, ?>> conflicts = config.getMapList("schools.conflicts");
        if (conflicts == null || conflicts.isEmpty()) {
            return false;
        }
        for (Map<?, ?> conflict : conflicts) {
            if (conflict == null) {
                continue;
            }
            MagicSchool a = MagicSchool.fromString(value(conflict.get("a")));
            MagicSchool b = MagicSchool.fromString(value(conflict.get("b")));
            if (a == null || b == null) {
                continue;
            }
            if ((a == lastSchool && b == nextSchool) || (a == nextSchool && b == lastSchool)) {
                return true;
            }
        }
        return false;
    }

    private SchoolModifiers schoolModifiers(String path) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) {
            return SchoolModifiers.identity();
        }
        double damage = section.getDouble("damageMultiplier", 1.0);
        double mana = section.getDouble("manaMultiplier", 1.0);
        double cooldown = section.getDouble("cooldownMultiplier", 1.0);
        return new SchoolModifiers(damage, mana, cooldown);
    }

    private ConfigurationSection classBonusSection(String classId) {
        if (classId == null || classId.isBlank()) {
            return null;
        }
        String basePath = "schools.classBonuses.";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(basePath + classId);
        if (section != null) {
            return section;
        }
        String lower = classId.toLowerCase(Locale.ROOT);
        if (lower.equals(classId)) {
            return null;
        }
        return plugin.getConfig().getConfigurationSection(basePath + lower);
    }

    private String schoolName(MagicSchool school) {
        if (school == null) {
            return "";
        }
        String key = "magic.school.name." + school.name();
        String raw = messages.raw(key);
        if (raw.contains("Missing message")) {
            return school.name();
        }
        return raw;
    }

    private String value(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }
}
