package ru.realite.magic.integration.items;

import org.bukkit.NamespacedKey;

public final class MagicItemTags {

    public static final String STAFF = "magic.staff";
    public static final String STAFF_CHARGES = "magic.staff.charges";
    public static final String STAFF_MAX_CHARGES = "magic.staff.maxCharges";
    public static final String STAFF_SCHOOL = "magic.staff.school";
    public static final String RUNE = "magic.rune";
    public static final String RUNE_DAMAGE_MULTIPLIER = "magic.rune.damageMultiplier";
    public static final String RUNE_MANA_MULTIPLIER = "magic.rune.manaMultiplier";
    public static final String RUNE_COOLDOWN_MULTIPLIER = "magic.rune.cooldownMultiplier";
    public static final String RUNE_SCHOOL = "magic.rune.school";
    public static final String MASTERY_XP_BONUS = "magic.mastery.xpBonus";

    private MagicItemTags() {
    }

    public static NamespacedKey key(String rawKey) {
        return new NamespacedKey("realite", rawKey);
    }
}
