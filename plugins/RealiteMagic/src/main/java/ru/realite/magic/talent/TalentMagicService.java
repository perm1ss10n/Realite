package ru.realite.magic.talent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import ru.realite.core.api.talents.TalentDefinition;
import ru.realite.core.api.talents.TalentMagicDefinition;
import ru.realite.core.api.talents.TalentMagicModifiers;
import ru.realite.core.api.talents.TalentMagicOnDamage;
import ru.realite.core.api.talents.TalentMagicPotionEffect;
import ru.realite.magic.cast.CastDeliveryType;
import ru.realite.magic.cast.CastExecutionPlan;
import ru.realite.magic.effect.BalanceModifiers;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.integration.talents.TalentsBridge;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.SpellDefinition;

public final class TalentMagicService {

    private final TalentsBridge talentsBridge;
    private final Logger logger;
    private final Set<String> warnedTalents = new HashSet<>();

    public TalentMagicService(TalentsBridge talentsBridge, Logger logger) {
        this.talentsBridge = Objects.requireNonNull(talentsBridge, "talentsBridge");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    public BalanceModifiers modifiers(Player player, SpellDefinition spell, CastExecutionPlan plan) {
        if (player == null || spell == null) {
            return BalanceModifiers.identity();
        }
        Set<String> activeTalents = talentsBridge.activeTalents(player);
        if (activeTalents == null || activeTalents.isEmpty()) {
            return BalanceModifiers.identity();
        }
        double damage = 1.0;
        double mana = 1.0;
        double cooldown = 1.0;
        for (String talentId : activeTalents) {
            TalentDefinition talent = talentsBridge.findTalent(talentId).orElse(null);
            if (talent == null) {
                warnOnce(talentId, "Missing talent definition for active talent.");
                continue;
            }
            TalentMagicDefinition magic = talent.magic();
            if (magic == null) {
                continue;
            }
            if (!matchesFilters(talent.id(), magic, spell)) {
                continue;
            }
            TalentMagicModifiers modifiers = magic.modifiers();
            if (modifiers == null) {
                continue;
            }
            if (!isValidMultiplier(modifiers.damageMultiplier())
                    || !isValidMultiplier(modifiers.manaMultiplier())
                    || !isValidMultiplier(modifiers.cooldownMultiplier())) {
                warnOnce(talent.id(), "Invalid talent modifier multipliers.");
                continue;
            }
            damage *= modifiers.damageMultiplier();
            mana *= modifiers.manaMultiplier();
            cooldown *= modifiers.cooldownMultiplier();
        }
        return new BalanceModifiers(damage, mana, cooldown);
    }

    public List<SpellEffectDefinition> extraEffects(Player player, SpellDefinition spell, CastExecutionPlan plan) {
        if (player == null || spell == null || plan == null) {
            return List.of();
        }
        Set<String> activeTalents = talentsBridge.activeTalents(player);
        if (activeTalents == null || activeTalents.isEmpty()) {
            return List.of();
        }
        List<SpellEffectDefinition> effects = new ArrayList<>();
        for (String talentId : activeTalents) {
            TalentDefinition talent = talentsBridge.findTalent(talentId).orElse(null);
            if (talent == null) {
                continue;
            }
            TalentMagicDefinition magic = talent.magic();
            if (magic == null) {
                continue;
            }
            if (!matchesFilters(talent.id(), magic, spell)) {
                continue;
            }
            TalentMagicOnDamage onDamage = magic.onDamage();
            if (onDamage == null) {
                continue;
            }
            if (!hasEffect(spell, "damage")) {
                continue;
            }
            SpellEffectDefinition extra = buildOnDamageEffect(talent.id(), onDamage);
            if (extra != null) {
                effects.add(extra);
            }
        }
        return effects;
    }

    private SpellEffectDefinition buildOnDamageEffect(String talentId, TalentMagicOnDamage onDamage) {
        double chance = onDamage.chance();
        if (!Double.isFinite(chance) || chance <= 0.0) {
            return null;
        }
        if (chance > 1.0) {
            warnOnce(talentId, "Talent onDamage chance is greater than 1.0, clamping.");
            chance = 1.0;
        }
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return null;
        }
        TalentMagicPotionEffect potion = onDamage.applyPotion();
        if (potion == null || potion.effect() == null || potion.effect().isBlank()) {
            warnOnce(talentId, "Talent onDamage.applyPotion is missing effect.");
            return null;
        }
        PotionEffectType type = resolvePotionType(potion.effect());
        if (type == null) {
            warnOnce(talentId, "Talent onDamage.applyPotion has unknown effect: " + potion.effect());
            return null;
        }
        if (potion.durationTicks() <= 0) {
            warnOnce(talentId, "Talent onDamage.applyPotion has non-positive durationTicks.");
            return null;
        }
        if (potion.amplifier() < 0) {
            warnOnce(talentId, "Talent onDamage.applyPotion has negative amplifier.");
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("effect", type.getKey().getKey());
        params.put("durationTicks", potion.durationTicks());
        params.put("amplifier", potion.amplifier());
        return new SpellEffectDefinition("potion", params);
    }

    private PotionEffectType resolvePotionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String key = raw.trim().toLowerCase(Locale.ROOT);
        return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(key));
    }

    private boolean matchesFilters(String talentId, TalentMagicDefinition magic, SpellDefinition spell) {
        String schoolFilter = magic.school();
        if (schoolFilter != null) {
            MagicSchool school = MagicSchool.fromString(schoolFilter);
            if (school == null) {
                warnOnce(talentId, "Talent magic.school is invalid: " + schoolFilter);
                return false;
            }
            if (spell.school() != school) {
                return false;
            }
        }
        String deliveryFilter = magic.delivery();
        if (deliveryFilter != null) {
            CastDeliveryType delivery = parseDelivery(deliveryFilter);
            if (delivery == null) {
                warnOnce(talentId, "Talent magic.delivery is invalid: " + deliveryFilter);
                return false;
            }
            CastDeliveryType actual = spell.castDelivery();
            if (actual == null) {
                actual = CastDeliveryType.INSTANT;
            }
            if (actual != delivery) {
                return false;
            }
        }
        String effectFilter = magic.effect();
        if (effectFilter != null) {
            if (!hasEffect(spell, effectFilter)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasEffect(SpellDefinition spell, String effect) {
        if (spell.effects() == null || effect == null) {
            return false;
        }
        String normalized = effect.trim().toLowerCase(Locale.ROOT);
        for (SpellEffectDefinition definition : spell.effects()) {
            if (definition.type().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }

    private CastDeliveryType parseDelivery(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return CastDeliveryType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isValidMultiplier(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    private void warnOnce(String talentId, String message) {
        String key = talentId + ":" + message;
        if (!warnedTalents.add(key)) {
            return;
        }
        logger.warning("Talent " + talentId + ": " + message);
    }
}
