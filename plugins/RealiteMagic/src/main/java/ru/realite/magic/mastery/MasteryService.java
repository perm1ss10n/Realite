package ru.realite.magic.mastery;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.api.event.SpellMasteryLevelUpEvent;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MasteryService {

    private static final int MIN_LEVEL = 1;

    private final JavaPlugin plugin;
    private final MagicMessages messages;
    private final SpellRegistry spellRegistry;
    private final MasteryProgressRepository repository;

    public MasteryService(JavaPlugin plugin,
                          MagicMessages messages,
                          SpellRegistry spellRegistry,
                          MasteryProgressRepository repository) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.repository = Objects.requireNonNull(repository, "repository");
        validateConfig();
    }

    public MasteryProgress getProgress(UUID playerId, String spellId) {
        return repository.getOrCreate(playerId, spellId);
    }

    public MasteryModifiers modifiers(UUID playerId, String spellId) {
        if (!enabled()) {
            return MasteryModifiers.identity();
        }
        MasteryProgress progress = getProgress(playerId, spellId);
        int level = Math.max(MIN_LEVEL, progress.level());
        int maxLevel = maxLevel();
        int effectiveLevel = Math.min(level, maxLevel);
        int steps = Math.max(0, effectiveLevel - 1);
        double damage = 1.0 + perLevelDamage() * steps;
        double mana = 1.0 + perLevelMana() * steps;
        double cooldown = 1.0 + perLevelCooldown() * steps;
        return new MasteryModifiers(damage, mana, cooldown);
    }

    public LevelUpResult addXp(UUID playerId, String spellId, int amount, MasteryXpSource source) {
        MasteryProgress progress = repository.getOrCreate(playerId, spellId);
        int oldLevel = progress.level();
        if (!enabled() || amount <= 0 || !repository.isLearned(playerId, spellId)) {
            int xpRequired = xpRequiredForLevel(oldLevel);
            int xpToNext = xpToNextLevel(oldLevel, progress.xp());
            return new LevelUpResult(oldLevel, oldLevel, progress.xp(), xpToNext, xpRequired, false);
        }
        int maxLevel = maxLevel();
        int currentLevel = Math.min(Math.max(MIN_LEVEL, oldLevel), maxLevel);
        int xpInLevel = Math.max(0, progress.xp());
        addCounter(progress, source);
        int remaining = amount;
        while (remaining > 0 && currentLevel < maxLevel) {
            int xpRequired = xpRequiredForLevel(currentLevel);
            int needed = Math.max(0, xpRequired - xpInLevel);
            if (needed == 0) {
                currentLevel++;
                xpInLevel = 0;
                continue;
            }
            if (remaining >= needed) {
                remaining -= needed;
                currentLevel++;
                xpInLevel = 0;
            } else {
                xpInLevel += remaining;
                remaining = 0;
            }
        }
        progress.level(currentLevel);
        progress.xp(xpInLevel);
        repository.markDirty(playerId);
        boolean leveledUp = currentLevel != oldLevel;
        if (leveledUp) {
            notifyLevelUp(playerId, spellId, currentLevel);
        }
        int xpRequired = xpRequiredForLevel(currentLevel);
        int xpToNext = xpToNextLevel(currentLevel, xpInLevel);
        return new LevelUpResult(oldLevel, currentLevel, xpInLevel, xpToNext, xpRequired, leveledUp);
    }

    public int xpToNext(UUID playerId, String spellId) {
        MasteryProgress progress = getProgress(playerId, spellId);
        return xpToNextLevel(progress.level(), progress.xp());
    }

    public void reloadConfig() {
        validateConfig();
    }

    public int xpForSource(MasteryXpSource source) {
        FileConfiguration config = plugin.getConfig();
        return switch (source) {
            case CAST_SUCCESS -> config.getInt("mastery.xp.castSuccess", 1);
            case HIT -> config.getInt("mastery.xp.hit", 2);
            case KILL -> config.getInt("mastery.xp.kill", 5);
        };
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("mastery.enabled", false);
    }

    private int maxLevel() {
        int configured = plugin.getConfig().getInt("mastery.maxLevel", MIN_LEVEL);
        return Math.max(MIN_LEVEL, configured);
    }

    private double perLevelDamage() {
        return plugin.getConfig().getDouble("mastery.modifiers.perLevel.damageMultiplier", 0.0);
    }

    private double perLevelMana() {
        return plugin.getConfig().getDouble("mastery.modifiers.perLevel.manaMultiplier", 0.0);
    }

    private double perLevelCooldown() {
        return plugin.getConfig().getDouble("mastery.modifiers.perLevel.cooldownMultiplier", 0.0);
    }

    private int xpRequiredForLevel(int level) {
        int safeLevel = Math.max(MIN_LEVEL, level);
        FileConfiguration config = plugin.getConfig();
        String mode = config.getString("mastery.progression.mode", "LINEAR");
        if (mode != null && mode.trim().equalsIgnoreCase("TABLE")) {
            List<Integer> table = config.getIntegerList("mastery.progression.table");
            if (table.isEmpty()) {
                return linearRequirement(safeLevel);
            }
            int index = Math.min(safeLevel - 1, table.size() - 1);
            return Math.max(1, table.get(index));
        }
        return linearRequirement(safeLevel);
    }

    private int linearRequirement(int level) {
        FileConfiguration config = plugin.getConfig();
        int base = config.getInt("mastery.progression.linear.base", 20);
        int perLevel = config.getInt("mastery.progression.linear.perLevel", 10);
        return Math.max(1, base + perLevel * (level - 1));
    }

    private int xpToNextLevel(int level, int xpInLevel) {
        int maxLevel = maxLevel();
        if (level >= maxLevel) {
            return 0;
        }
        int required = xpRequiredForLevel(level);
        return Math.max(0, required - Math.max(0, xpInLevel));
    }

    private void addCounter(MasteryProgress progress, MasteryXpSource source) {
        switch (source) {
            case CAST_SUCCESS -> progress.casts(progress.casts() + 1);
            case HIT -> progress.hits(progress.hits() + 1);
            case KILL -> progress.kills(progress.kills() + 1);
        }
    }

    private void notifyLevelUp(UUID playerId, String spellId, int level) {
        Bukkit.getPluginManager().callEvent(new SpellMasteryLevelUpEvent(playerId, spellId, level));
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.sendMessage(messages.msg("magic.mastery.level_up",
                "spell", displaySpellName(spellId),
                "level", String.valueOf(level)));
    }

    private String displaySpellName(String spellId) {
        SpellDefinition spell = spellRegistry.get(spellId);
        if (spell == null) {
            return spellId;
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        String raw = messages.raw(nameKey);
        return raw == null || raw.isBlank() ? spell.id() : raw;
    }

    private void validateConfig() {
        FileConfiguration config = plugin.getConfig();
        int maxLevel = config.getInt("mastery.maxLevel", MIN_LEVEL);
        if (maxLevel < MIN_LEVEL) {
            plugin.getLogger().warning("mastery.maxLevel must be >= 1. Using 1.");
        }
        String mode = config.getString("mastery.progression.mode", "LINEAR");
        if (mode != null && mode.trim().toUpperCase(Locale.ROOT).equals("TABLE")) {
            List<Integer> table = config.getIntegerList("mastery.progression.table");
            if (table.size() < Math.max(MIN_LEVEL, maxLevel)) {
                plugin.getLogger().warning("mastery.progression.table has fewer entries than mastery.maxLevel.");
            }
        }
    }
}
