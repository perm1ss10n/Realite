package ru.realite.classes.storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.EvolutionDef;
import ru.realite.classes.model.EvolutionRequirement;
import ru.realite.classes.model.HiddenClassUnlock;
import ru.realite.classes.model.ItemAmount;
import ru.realite.core.api.talents.TalentDefinition;
import ru.realite.core.api.talents.TalentMagicDefinition;
import ru.realite.core.api.talents.TalentMagicModifiers;
import ru.realite.core.api.talents.TalentMagicOnDamage;
import ru.realite.core.api.talents.TalentMagicPotionEffect;
import ru.realite.core.api.talents.TalentRequirements;

import java.io.File;
import java.util.*;

public class ClassConfigRepository {

    public static class ClassDef {
        public final ClassId id;
        public final boolean hidden;
        public final java.util.Set<ClassId> requiresMastered;
        public final String name;
        public final List<String> lore;
        public final Material icon;

        public final List<String> effects;

        public final int xpPerLevel;
        public final double xpPerMoney;

        public final List<EvolutionDef> evolutions;

        public ClassDef(ClassId id, Material icon, String name, List<String> lore, List<String> effects,
                        int xpPerLevel, double xpPerMoney, List<EvolutionDef> evolutions,
                        boolean hidden, java.util.Set<ClassId> requiresMastered) {
            this.id = id;
            this.icon = icon;
            this.name = name;
            this.lore = lore;
            this.effects = effects;
            this.xpPerLevel = xpPerLevel;
            this.xpPerMoney = xpPerMoney;
            this.evolutions = evolutions;
            this.hidden = hidden;
            this.requiresMastered = requiresMastered;
        }

        public EvolutionDef firstEvolution() {
            if (evolutions == null || evolutions.isEmpty())
                return null;
            return evolutions.get(0);
        }

        public EvolutionDef finalEvolution() {
            if (evolutions == null || evolutions.isEmpty())
                return null;
            return evolutions.get(evolutions.size() - 1);
        }

        public EvolutionDef findEvolution(String id) {
            if (id == null)
                return null;
            if (evolutions == null)
                return null;
            for (EvolutionDef e : evolutions) {
                if (e.id.equalsIgnoreCase(id))
                    return e;
            }
            return null;
        }

        public EvolutionDef nextEvolution(String currentId) {
            if (evolutions == null || evolutions.isEmpty())
                return null;

            // если currentId пустой — значит первая стадия
            if (currentId == null || currentId.isBlank()) {
                return evolutions.get(0);
            }

            for (int i = 0; i < evolutions.size(); i++) {
                if (evolutions.get(i).id.equalsIgnoreCase(currentId)) {
                    int next = i + 1;
                    if (next >= evolutions.size())
                        return null;
                    return evolutions.get(next);
                }
            }
            return null;
        }
    }

    private final File dataFolder;
    private final Map<ClassId, ClassDef> map = new EnumMap<>(ClassId.class);
    private final Map<ClassId, HiddenClassUnlock> hiddenUnlocks = new EnumMap<>(ClassId.class);
    private final Map<String, TalentDefinition> talents = new HashMap<>();

    public ClassConfigRepository(File dataFolder) {
        this.dataFolder = dataFolder;
        // ✅ ВАЖНО: сразу грузим данные, иначе map всегда пустая
        reload();
    }

    public void reload() {
        map.clear();
        hiddenUnlocks.clear();
        talents.clear();

        File file = new File(dataFolder, "classes.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection classes = yml.getConfigurationSection("classes");
        if (classes == null)
            return;

        for (String key : classes.getKeys(false)) {
            ClassId id = ClassId.fromString(key);
            if (id == null)
                continue;

            ConfigurationSection s = classes.getConfigurationSection(key);
            if (s == null)
                continue;

            String name = s.getString("name", id.name());
            List<String> lore = s.getStringList("lore");

            Material icon = null;
            String iconName = s.getString("icon");
            if (iconName != null) {
                icon = Material.matchMaterial(iconName.trim().toUpperCase());
            }

            List<String> effects = s.getStringList("effects");

            int xpPerLevel = s.getInt("xp-per-level", 100);
            double xpPerMoney = s.getDouble("xp-per-money", 0.0);

            List<EvolutionDef> evolutions = parseEvolutions(s.getMapList("evolutions"));
            boolean hidden = s.getBoolean("hidden", false);

            java.util.Set<ClassId> req = new java.util.HashSet<>();
            for (String rid : s.getStringList("requires-mastered")) {
                ClassId cid = ClassId.fromString(rid);
                if (cid != null)
                    req.add(cid);
            }

            map.put(id, new ClassDef(id, icon, name, lore, effects, xpPerLevel, xpPerMoney, evolutions, hidden, req));
        }

        ConfigurationSection talentSection = yml.getConfigurationSection("talents");
        if (talentSection != null) {
            for (String key : talentSection.getKeys(false)) {
                ConfigurationSection s = talentSection.getConfigurationSection(key);
                if (s == null) {
                    continue;
                }
                String id = normalizeId(s.getString("id", key));
                if (id == null) {
                    continue;
                }
                String nameKey = s.getString("nameKey", "talents." + id + ".name");
                String descriptionKey = s.getString("descriptionKey", "talents." + id + ".desc");
                TalentRequirements requirements = parseTalentRequirements(s.getConfigurationSection("requirements"));
                TalentMagicDefinition magic = parseTalentMagic(s.getConfigurationSection("magic"));
                talents.put(id, new TalentDefinition(id, nameKey, descriptionKey, requirements, magic));
            }
        }

        ConfigurationSection hiddenClasses = yml.getConfigurationSection("hiddenClasses");
        if (hiddenClasses == null)
            return;

        for (String key : hiddenClasses.getKeys(false)) {
            ClassId id = ClassId.fromString(key);
            if (id == null)
                continue;

            ConfigurationSection s = hiddenClasses.getConfigurationSection(key);
            if (s == null)
                continue;

            String questId = s.getString("requireQuestCompleted", null);
            EvolutionRequirement requirement = parseEvolutionRequirement(s.getConfigurationSection("requireEvolution"));
            hiddenUnlocks.put(id, new HiddenClassUnlock(questId, requirement));
        }
    }

    private List<EvolutionDef> parseEvolutions(List<Map<?, ?>> list) {
        List<EvolutionDef> out = new ArrayList<>();
        if (list == null)
            return out;

        for (Map<?, ?> raw : list) {
            String id = str(raw.get("id"));
            String title = str(raw.get("title"));
            int requiredLevel = intv(raw.get("required-level"), 0);

            Map<?, ?> cost = map(raw.get("cost"));
            double costMoney = cost != null ? doublev(cost.get("money"), 0) : 0;
            List<ItemAmount> costItems = cost != null ? parseItems(cost.get("items")) : List.of();

            Map<?, ?> rewards = map(raw.get("rewards"));
            double rewardMoney = rewards != null ? doublev(rewards.get("money"), 0) : 0;
            List<ItemAmount> rewardItems = rewards != null ? parseItems(rewards.get("items")) : List.of();

            if (id == null || id.isBlank())
                continue;
            if (title == null || title.isBlank())
                title = id;

            out.add(new EvolutionDef(id, title, requiredLevel, costMoney, costItems, rewardMoney, rewardItems));
        }
        return out;
    }

    private List<ItemAmount> parseItems(Object obj) {
        if (!(obj instanceof List<?> l))
            return List.of();

        List<ItemAmount> out = new ArrayList<>();
        for (Object o : l) {
            if (!(o instanceof String s))
                continue;
            String[] parts = s.trim().split(":");
            if (parts.length != 2)
                continue;

            Material mat = Material.matchMaterial(parts[0].trim().toUpperCase());
            int amt;
            try {
                amt = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                continue;
            }

            if (mat == null || amt <= 0)
                continue;
            out.add(new ItemAmount(mat, amt));
        }
        return out;
    }

    private TalentRequirements parseTalentRequirements(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String classId = trim(section.getString("classId"));
        String evolutionId = trim(section.getString("evolutionId"));
        if (classId == null && evolutionId == null) {
            return null;
        }
        return new TalentRequirements(classId, evolutionId);
    }

    private TalentMagicDefinition parseTalentMagic(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        String school = trim(section.getString("school"));
        String delivery = trim(section.getString("delivery"));
        String effect = trim(section.getString("effect"));
        TalentMagicModifiers modifiers = parseTalentModifiers(section.getConfigurationSection("modifiers"));
        TalentMagicOnDamage onDamage = parseTalentOnDamage(section.getConfigurationSection("onDamage"));
        return new TalentMagicDefinition(school, delivery, effect, modifiers, onDamage);
    }

    private TalentMagicModifiers parseTalentModifiers(ConfigurationSection section) {
        if (section == null) {
            return TalentMagicModifiers.identity();
        }
        double damage = section.getDouble("damageMultiplier", 1.0);
        double mana = section.getDouble("manaMultiplier", 1.0);
        double cooldown = section.getDouble("cooldownMultiplier", 1.0);
        return new TalentMagicModifiers(damage, mana, cooldown);
    }

    private TalentMagicOnDamage parseTalentOnDamage(ConfigurationSection section) {
        if (section == null) {
            return null;
        }
        double chance = section.getDouble("chance", 0.0);
        TalentMagicPotionEffect potion = null;
        ConfigurationSection applyPotion = section.getConfigurationSection("applyPotion");
        if (applyPotion != null) {
            String effect = trim(applyPotion.getString("effect"));
            int duration = applyPotion.getInt("durationTicks", 0);
            int amplifier = applyPotion.getInt("amplifier", 0);
            if (effect != null) {
                potion = new TalentMagicPotionEffect(effect, duration, amplifier);
            }
        }
        return new TalentMagicOnDamage(chance, potion);
    }

    private String normalizeId(String raw) {
        String id = trim(raw);
        if (id == null) {
            return null;
        }
        return id.toLowerCase(java.util.Locale.ROOT);
    }

    private String trim(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int intv(Object o, int def) {
        if (o == null)
            return def;
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static double doublev(Object o, double def) {
        if (o == null)
            return def;
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    private static Map<?, ?> map(Object o) {
        if (o instanceof Map<?, ?> m)
            return m;
        return null;
    }

    private EvolutionRequirement parseEvolutionRequirement(ConfigurationSection section) {
        if (section == null)
            return null;

        java.util.Set<ClassId> mastered = new java.util.HashSet<>();
        for (String raw : section.getStringList("mastered")) {
            ClassId id = ClassId.fromString(raw);
            if (id != null) {
                mastered.add(id);
            }
        }
        return EvolutionRequirement.fromMastered(mastered);
    }

    public ClassDef get(ClassId id) {
        return map.get(id);
    }

    public Collection<ClassDef> all() {
        return map.values();
    }

    public HiddenClassUnlock getHiddenUnlock(ClassId id) {
        return hiddenUnlocks.get(id);
    }

    public TalentDefinition getTalent(String id) {
        if (id == null) {
            return null;
        }
        return talents.get(id.toLowerCase(java.util.Locale.ROOT));
    }

    public Collection<TalentDefinition> talents() {
        return talents.values();
    }
}
