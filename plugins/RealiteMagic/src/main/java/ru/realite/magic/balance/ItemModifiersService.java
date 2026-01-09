package ru.realite.magic.balance;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.MagicItemTags;
import ru.realite.magic.requirements.CheckResult;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.SpellDefinition;

public final class ItemModifiersService {

    private final JavaPlugin plugin;
    private final ItemsBridge itemsBridge;

    public ItemModifiersService(JavaPlugin plugin, ItemsBridge itemsBridge) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.itemsBridge = Objects.requireNonNull(itemsBridge, "itemsBridge");
    }

    public ItemModifiers modifiers(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return ItemModifiers.identity();
        }
        Optional<RuneInfo> runeInfo = findRune(player);
        if (runeInfo.isEmpty()) {
            return ItemModifiers.identity();
        }
        if (!isRuneSchoolMatch(runeInfo.get().school(), spell.school())) {
            return ItemModifiers.identity();
        }
        RuneInfo rune = runeInfo.get();
        return new ItemModifiers(
                rune.damageMultiplier(),
                rune.manaMultiplier(),
                rune.cooldownMultiplier());
    }

    public CheckResult checkRuneSchool(Player player, SpellDefinition spell) {
        if (!strictSchoolMatch()) {
            return new CheckResult.Ok();
        }
        Optional<RuneInfo> runeInfo = findRune(player);
        if (runeInfo.isEmpty()) {
            return new CheckResult.Ok();
        }
        if (isRuneSchoolMatch(runeInfo.get().school(), spell == null ? null : spell.school())) {
            return new CheckResult.Ok();
        }
        return new CheckResult.Fail("magic.rune.school_mismatch", java.util.Map.of());
    }

    private Optional<RuneInfo> findRune(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null) {
            return Optional.empty();
        }
        if (itemsBridge.readInt(offhand, MagicItemTags.RUNE).orElse(0) <= 0) {
            return Optional.empty();
        }
        double damageMultiplier = itemsBridge.readDouble(offhand, MagicItemTags.RUNE_DAMAGE_MULTIPLIER).orElse(1.0);
        double manaMultiplier = itemsBridge.readDouble(offhand, MagicItemTags.RUNE_MANA_MULTIPLIER).orElse(1.0);
        double cooldownMultiplier = itemsBridge.readDouble(offhand, MagicItemTags.RUNE_COOLDOWN_MULTIPLIER).orElse(1.0);
        String school = itemsBridge.readString(offhand, MagicItemTags.RUNE_SCHOOL).orElse(null);
        return Optional.of(new RuneInfo(damageMultiplier, manaMultiplier, cooldownMultiplier, school));
    }

    private boolean isRuneSchoolMatch(String runeSchool, MagicSchool spellSchool) {
        if (runeSchool == null || runeSchool.isBlank()) {
            return true;
        }
        if (spellSchool == null) {
            return false;
        }
        String normalized = runeSchool.trim().toUpperCase(Locale.ROOT);
        return normalized.equals(spellSchool.name());
    }

    private boolean strictSchoolMatch() {
        FileConfiguration config = plugin.getConfig();
        return config.getBoolean("runes.strictSchoolMatch", false);
    }

    private record RuneInfo(double damageMultiplier,
                            double manaMultiplier,
                            double cooldownMultiplier,
                            String school) {
    }
}
