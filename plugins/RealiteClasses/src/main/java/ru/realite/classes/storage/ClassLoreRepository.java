package ru.realite.classes.storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.classes.model.ClassId;

import java.io.File;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ClassLoreRepository {

    public static class ClassLoreDef {
        public final ClassId id;
        public final String displayName;
        public final Material icon;
        public final List<String> lore;
        public final boolean hiddenEnabled;
        public final Material lockedIcon;
        public final String lockedName;
        public final List<String> lockedLore;

        public ClassLoreDef(ClassId id,
                            String displayName,
                            Material icon,
                            List<String> lore,
                            boolean hiddenEnabled,
                            Material lockedIcon,
                            String lockedName,
                            List<String> lockedLore) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.lore = lore;
            this.hiddenEnabled = hiddenEnabled;
            this.lockedIcon = lockedIcon;
            this.lockedName = lockedName;
            this.lockedLore = lockedLore;
        }
    }

    private final File dataFolder;
    private final Map<ClassId, ClassLoreDef> map = new EnumMap<>(ClassId.class);

    public ClassLoreRepository(File dataFolder) {
        this.dataFolder = dataFolder;
        reload();
    }

    public void reload() {
        map.clear();

        File file = new File(dataFolder, "classes_lore.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection classes = yml.getConfigurationSection("classes");
        if (classes == null) {
            return;
        }

        for (String key : classes.getKeys(false)) {
            ClassId id = ClassId.fromString(key);
            if (id == null) {
                continue;
            }

            ConfigurationSection s = classes.getConfigurationSection(key);
            if (s == null) {
                continue;
            }

            String displayName = s.getString("displayName", id.name());
            Material icon = parseMaterial(s.getString("icon"));
            List<String> lore = s.getStringList("lore");

            ConfigurationSection hidden = s.getConfigurationSection("hidden");
            boolean hiddenEnabled = hidden != null && hidden.getBoolean("enabled", false);
            Material lockedIcon = parseMaterial(hidden != null ? hidden.getString("lockedIcon") : null);
            String lockedName = hidden != null ? hidden.getString("lockedName") : null;
            List<String> lockedLore = hidden != null ? hidden.getStringList("lockedLore") : List.of();

            map.put(id, new ClassLoreDef(
                    id,
                    displayName,
                    icon,
                    lore,
                    hiddenEnabled,
                    lockedIcon,
                    lockedName,
                    lockedLore));
        }
    }

    private Material parseMaterial(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return Material.matchMaterial(name.trim().toUpperCase());
    }

    public ClassLoreDef get(ClassId id) {
        return map.get(id);
    }
}
