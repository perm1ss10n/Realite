package ru.realite.classes.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.model.HudMode;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class YamlProfileRepository {

    private final File playersDir;

    public YamlProfileRepository(File dataFolder) {
        this.playersDir = new File(dataFolder, "players");
        if (!playersDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            playersDir.mkdirs();
        }
    }

    private File file(UUID uuid) {
        return new File(playersDir, uuid.toString() + ".yml");
    }

    public PlayerProfile load(UUID uuid) {
        File f = file(uuid);
        PlayerProfile p = new PlayerProfile(uuid);

        if (!f.exists())
            return p;

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);

        p.setClassId(ClassId.fromString(yml.getString("class")));
        p.setEvolution(yml.getString("evolution", null));
        p.setClassLevel(yml.getInt("classLevel", 0));
        p.setClassXp(yml.getLong("classXp", 0L));

        // старые поля
        p.setEvolutionRewardTaken(yml.getBoolean("evolutionRewardTaken", false));
        p.setLastClassChange(yml.getLong("lastClassChange", 0L));
        p.setEvolutionNotified(yml.getBoolean("evolutionNotified", false));

        // новые поля
        p.setStarterClass(yml.getBoolean("starterClass", false));

        var sec = yml.getConfigurationSection("maxLevelByClass");
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                map.put(key, sec.getInt(key, 0));
            }
        }
        p.setMaxLevelByClass(map);

        p.setMasteredClasses(new java.util.HashSet<>(yml.getStringList("masteredClasses")));
        p.setHudMode(HudMode.fromString(yml.getString("hudMode")));

        return p;
    }

    public void save(PlayerProfile profile) {
        File f = file(profile.getUuid());
        YamlConfiguration yml = new YamlConfiguration();

        if (profile.getClassId() != null)
            yml.set("class", profile.getClassId().name());
        yml.set("evolution", profile.getEvolution());
        yml.set("classLevel", profile.getClassLevel());
        yml.set("classXp", profile.getClassXp());

        // старые поля
        yml.set("evolutionRewardTaken", profile.isEvolutionRewardTaken());
        yml.set("lastClassChange", profile.getLastClassChange());
        yml.set("evolutionNotified", profile.isEvolutionNotified());

        // новые поля
        yml.set("starterClass", profile.isStarterClass());

        yml.set("maxLevelByClass", null);
        for (var e : profile.getMaxLevelByClass().entrySet()) {
            yml.set("maxLevelByClass." + e.getKey(), e.getValue());
        }

        yml.set("masteredClasses", new java.util.ArrayList<>(profile.getMasteredClasses()));

        try {
            yml.save(f);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save profile " + profile.getUuid(), e);
        }

        yml.set("hudMode", profile.getHudMode().name());
    }
}
