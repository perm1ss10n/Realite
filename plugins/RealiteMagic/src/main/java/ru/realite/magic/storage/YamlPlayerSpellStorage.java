package ru.realite.magic.storage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.PlayerSpellData;

public final class YamlPlayerSpellStorage implements PlayerSpellStorage {

    private static final int CURRENT_VERSION = 2;

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
