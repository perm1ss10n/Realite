package ru.realite.magic.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.mastery.MasteryProgress;
import ru.realite.magic.model.PlayerSpellData;

public final class YamlPlayerSpellStorage implements PlayerSpellStorage {

    private static final int CURRENT_VERSION = 3;

    private final File spellsDir;
    private final JavaPlugin plugin;
    private final MagicMessages messages;

    public YamlPlayerSpellStorage(JavaPlugin plugin, MagicMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.spellsDir = new File(plugin.getDataFolder(), "playerdata/spells");
        if (!spellsDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            spellsDir.mkdirs();
        }
    }

    @Override
    public PlayerSpellData load(UUID playerId) {
        File file = file(playerId);
        if (!file.exists()) {
            return new PlayerSpellData(CURRENT_VERSION);
        }

        YamlConfiguration yml = new YamlConfiguration();
        try {
            yml.load(file);
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().log(Level.WARNING, messages.raw("magic.storage.load_fail"), e);
            return new PlayerSpellData(CURRENT_VERSION);
        }

        int version = yml.getInt("version", CURRENT_VERSION);
        PlayerSpellData data = new PlayerSpellData(CURRENT_VERSION);
        for (String spellId : yml.getStringList("learned")) {
            data.learn(spellId);
        }
        data.selected(yml.getString("selected", null));
        data.activeSlot(yml.getInt("activeSlot", 1));
        List<String> slots = new ArrayList<>();
        List<?> rawSlots = yml.getList("slots");
        if (rawSlots != null) {
            for (Object value : rawSlots) {
                if (value == null) {
                    slots.add(null);
                } else {
                    slots.add(value.toString());
                }
            }
        }
        data.slots(slots);
        ConfigurationSection masterySection = yml.getConfigurationSection("mastery");
        if (masterySection != null) {
            for (String spellId : masterySection.getKeys(false)) {
                ConfigurationSection section = masterySection.getConfigurationSection(spellId);
                if (section == null) {
                    continue;
                }
                int level = section.getInt("level", 1);
                int xp = section.getInt("xp", 0);
                long casts = section.getLong("casts", 0L);
                long hits = section.getLong("hits", 0L);
                long kills = section.getLong("kills", 0L);
                data.mastery(spellId, new MasteryProgress(level, xp, casts, hits, kills));
            }
        }
        if (version < 2) {
            data.slot(1, data.selected().orElse(null));
        }
        return data;
    }

    @Override
    public void save(UUID playerId, PlayerSpellData data) {
        File file = file(playerId);
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("version", data.version());
        List<String> learned = new ArrayList<>(data.learned());
        learned.sort(String::compareTo);
        yml.set("learned", learned);
        yml.set("selected", data.selected().orElse(null));
        yml.set("slots", new ArrayList<>(data.slots()));
        yml.set("activeSlot", data.activeSlot());
        Map<String, Object> masteryMap = new LinkedHashMap<>();
        for (Map.Entry<String, MasteryProgress> entry : data.mastery().entrySet()) {
            MasteryProgress progress = entry.getValue();
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("level", progress.level());
            value.put("xp", progress.xp());
            value.put("casts", progress.casts());
            value.put("hits", progress.hits());
            value.put("kills", progress.kills());
            masteryMap.put(entry.getKey(), value);
        }
        if (!masteryMap.isEmpty()) {
            yml.set("mastery", masteryMap);
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, messages.raw("magic.storage.save_fail"), e);
        }
    }

    private File file(UUID playerId) {
        return new File(spellsDir, playerId.toString() + ".yml");
    }
}
