package ru.realite.classes.storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Logger;

public class XpConfigRepository {

    private final File dataFolder;
    private final Logger log;

    private boolean minerEnabled;
    private final Map<Material, Integer> minerOreXp = new EnumMap<>(Material.class);

    private boolean warriorEnabled;
    private int warriorKillMobs;
    private int warriorKillPlayers;

    private boolean archerEnabled;
    private int archerArrowKillMobs;
    private int archerArrowKillPlayers;

    private boolean alchemistEnabled;
    private int alchemistPotionTakeXp;
    private boolean alchemistAllowNormal;
    private boolean alchemistAllowSplash;
    private boolean alchemistAllowLingering;

    public XpConfigRepository(File dataFolder, Logger log) {
        this.dataFolder = dataFolder;
        this.log = log;
        reload();
    }

    public void reload() {
        File f = new File(dataFolder, "xp.yml");
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);

        // MINER
        minerEnabled = yml.getBoolean("miner.enabled", true);
        minerOreXp.clear();
        ConfigurationSection ores = yml.getConfigurationSection("miner.ores");
        if (ores != null) {
            for (String key : ores.getKeys(false)) {
                Material m = Material.matchMaterial(key);
                if (m == null) {
                    log.warning("[xp.yml] Unknown material in miner.ores: " + key);
                    continue;
                }
                int xp = Math.max(0, ores.getInt(key, 0));
                minerOreXp.put(m, xp);
            }
        }

        // WARRIOR
        warriorEnabled = yml.getBoolean("warrior.enabled", true);
        warriorKillMobs = Math.max(0, yml.getInt("warrior.kill.mobs", 5));
        warriorKillPlayers = Math.max(0, yml.getInt("warrior.kill.players", 20));

        // ARCHER
        archerEnabled = yml.getBoolean("archer.enabled", true);
        archerArrowKillMobs = Math.max(0, yml.getInt("archer.arrow-kill.mobs", 6));
        archerArrowKillPlayers = Math.max(0, yml.getInt("archer.arrow-kill.players", 25));

        // ALCHEMIST
        alchemistEnabled = yml.getBoolean("alchemist.enabled", true);
        alchemistPotionTakeXp = Math.max(0, yml.getInt("alchemist.potion-take.xp", 8));
        alchemistAllowNormal = yml.getBoolean("alchemist.potion-take.allow-normal", true);
        alchemistAllowSplash = yml.getBoolean("alchemist.potion-take.allow-splash", true);
        alchemistAllowLingering = yml.getBoolean("alchemist.potion-take.allow-lingering", true);
    }

    // ---------- getters ----------
    public boolean isMinerEnabled() { return minerEnabled; }

    public int getMinerOreXp(Material ore) {
        return minerOreXp.getOrDefault(ore, 0);
    }

    public boolean isWarriorEnabled() { return warriorEnabled; }
    public int getWarriorKillMobs() { return warriorKillMobs; }
    public int getWarriorKillPlayers() { return warriorKillPlayers; }

    public boolean isArcherEnabled() { return archerEnabled; }
    public int getArcherArrowKillMobs() { return archerArrowKillMobs; }
    public int getArcherArrowKillPlayers() { return archerArrowKillPlayers; }

    public boolean isAlchemistEnabled() { return alchemistEnabled; }
    public int getAlchemistPotionTakeXp() { return alchemistPotionTakeXp; }
    public boolean isAlchemistAllowNormal() { return alchemistAllowNormal; }
    public boolean isAlchemistAllowSplash() { return alchemistAllowSplash; }
    public boolean isAlchemistAllowLingering() { return alchemistAllowLingering; }
}
