package ru.realite.magic.service;

import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.effect.BalanceModifiers;
import ru.realite.magic.integration.guilds.GuildBridge;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.SpellDefinition;

public final class GuildBonusService {

    private final JavaPlugin plugin;
    private final GuildBridge guildBridge;

    public GuildBonusService(JavaPlugin plugin, GuildBridge guildBridge) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.guildBridge = Objects.requireNonNull(guildBridge, "guildBridge");
    }

    public BalanceModifiers guildModifiers(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return BalanceModifiers.identity();
        }
        if (!plugin.getConfig().getBoolean("guilds.enabled", true)) {
            return BalanceModifiers.identity();
        }
        if (!guildBridge.isAvailable()) {
            return BalanceModifiers.identity();
        }
        Optional<String> guildId = guildBridge.guildId(player.getUniqueId());
        if (guildId.isEmpty()) {
            return BalanceModifiers.identity();
        }
        BalanceModifiers schoolBonus = readSchoolBonus(spell.school());
        BalanceModifiers rankBonus = readRankBonus(guildBridge.guildRank(player.getUniqueId()).orElse(null));
        return new BalanceModifiers(
                schoolBonus.damageMultiplier() * rankBonus.damageMultiplier(),
                schoolBonus.manaMultiplier() * rankBonus.manaMultiplier(),
                schoolBonus.cooldownMultiplier() * rankBonus.cooldownMultiplier());
    }

    private BalanceModifiers readSchoolBonus(MagicSchool school) {
        if (school == null) {
            return BalanceModifiers.identity();
        }
        String path = "guilds.bonuses." + school.name();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        return readModifiers(section);
    }

    private BalanceModifiers readRankBonus(String rankId) {
        if (rankId == null || rankId.isBlank()) {
            return BalanceModifiers.identity();
        }
        String path = "guilds.rankBonuses." + rankId;
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        return readModifiers(section);
    }

    private BalanceModifiers readModifiers(ConfigurationSection section) {
        if (section == null) {
            return BalanceModifiers.identity();
        }
        double damage = section.getDouble("damageMultiplier", 1.0);
        double mana = section.getDouble("manaMultiplier", 1.0);
        double cooldown = section.getDouble("cooldownMultiplier", 1.0);
        return new BalanceModifiers(damage, mana, cooldown);
    }
}
