package ru.realite.magic.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.EffectTargetType;
import ru.realite.magic.effect.EffectValidationResult;
import ru.realite.magic.effect.SpellEffectDefinition;
import ru.realite.magic.effect.SpellEffectExecutor;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRequirements;
import ru.realite.magic.target.SpellTargetDefinition;
import ru.realite.magic.target.SpellTargetType;

public final class SpellSchemaValidator {

    private final EffectExecutorRegistry effectRegistry;

    public SpellSchemaValidator(EffectExecutorRegistry effectRegistry) {
        this.effectRegistry = Objects.requireNonNull(effectRegistry, "effectRegistry");
    }

    public SchemaReport validate(SpellDefinition spell, String fileName) {
        SchemaReport report = new SchemaReport();
        if (spell == null) {
            report.addError(fileName, null, "spell", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "spell", "value", "null"));
            return report;
        }
        String spellId = spell.id();
        if (spellId == null || spellId.isBlank()) {
            report.addError(fileName, null, "id", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "id"));
        }
        if (spell.type() == null) {
            report.addError(fileName, spellId, "type", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "type"));
        }
        if (spell.nameKey() == null || spell.nameKey().isBlank()) {
            report.addError(fileName, spellId, "nameKey", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "nameKey"));
        }
        if (spell.descKey() == null || spell.descKey().isBlank()) {
            report.addError(fileName, spellId, "descKey", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "descKey"));
        }
        if (spell.school() == null) {
            report.addError(fileName, spellId, "school", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "school", "value", "null"));
        }
        if (spell.mana() < 0) {
            report.addError(fileName, spellId, "mana", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "mana", "value", String.valueOf(spell.mana())));
        }
        if (spell.cooldownTicks() < 0) {
            report.addError(fileName, spellId, "cooldownTicks", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "cooldownTicks", "value", String.valueOf(spell.cooldownTicks())));
        }
        if (spell.range() <= 0) {
            report.addError(fileName, spellId, "range", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "range", "value", String.valueOf(spell.range())));
        }
        if (spell.damage() < 0) {
            report.addError(fileName, spellId, "damage", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "damage", "value", String.valueOf(spell.damage())));
        }
        if (spell.castTrigger() == null) {
            report.addError(fileName, spellId, "cast.trigger", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "cast.trigger"));
        }
        if (spell.castItemId() != null && spell.castItemId().isBlank()) {
            report.addError(fileName, spellId, "cast.itemId", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "cast.itemId", "value", spell.castItemId()));
        }
        if (spell.staffChargesCost() != null && spell.staffChargesCost() < 0) {
            report.addError(fileName, spellId, "cast.staffChargesCost", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "cast.staffChargesCost", "value", String.valueOf(spell.staffChargesCost())));
        }
        validateRequirements(report, spell, fileName, spellId);
        validateTarget(report, spell.target(), fileName, spellId);
        validateEffects(report, spell, fileName, spellId);
        return report;
    }

    private void validateRequirements(SchemaReport report,
                                      SpellDefinition spell,
                                      String fileName,
                                      String spellId) {
        SpellRequirements requirements = spell.requirements();
        if (requirements == null) {
            report.addError(fileName, spellId, "requirements", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "requirements"));
            return;
        }
        if (isBlank(requirements.classId())) {
            report.addError(fileName, spellId, "requirements.class",
                    "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "requirements.class", "value", String.valueOf(requirements.classId())));
        }
        if (isBlank(requirements.evolutionId())) {
            report.addError(fileName, spellId, "requirements.evolution",
                    "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "requirements.evolution", "value", String.valueOf(requirements.evolutionId())));
        }
        if (isBlank(requirements.requiredItemId())) {
            report.addError(fileName, spellId, "requirements.requiredItemId",
                    "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "requirements.requiredItemId",
                            "value", String.valueOf(requirements.requiredItemId())));
        }
    }

    private void validateTarget(SchemaReport report,
                                SpellTargetDefinition target,
                                String fileName,
                                String spellId) {
        if (target == null) {
            report.addError(fileName, spellId, "target", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "target"));
            return;
        }
        SpellTargetType type = target.type();
        if (type == null || !(type == SpellTargetType.SELF
                || type == SpellTargetType.ENTITY
                || type == SpellTargetType.BLOCK)) {
            report.addError(fileName, spellId, "target.type", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "target.type", "value", String.valueOf(type)));
        }
        if (target.maxDistance() < 0) {
            report.addError(fileName, spellId, "target.maxDistance", "magic.cmd.spells.errors.invalid_value",
                    Map.of("field", "target.maxDistance", "value", String.valueOf(target.maxDistance())));
        }
    }

    private void validateEffects(SchemaReport report,
                                 SpellDefinition spell,
                                 String fileName,
                                 String spellId) {
        List<SpellEffectDefinition> effects = spell.effects();
        if (effects == null || effects.isEmpty()) {
            report.addError(fileName, spellId, "effects", "magic.cmd.spells.errors.missing_field",
                    Map.of("field", "effects"));
            return;
        }
        for (int i = 0; i < effects.size(); i++) {
            SpellEffectDefinition effect = effects.get(i);
            String prefix = "effects[" + i + "]";
            if (effect == null) {
                report.addError(fileName, spellId, prefix, "magic.cmd.spells.errors.invalid_value",
                        Map.of("field", "effects[" + i + "]", "value", "null"));
                continue;
            }
            if (effect.type() == null || effect.type().isBlank()) {
                report.addError(fileName, spellId, prefix + ".type", "magic.cmd.spells.errors.missing_field",
                        Map.of("field", "effects[" + i + "].type"));
                continue;
            }
            SpellEffectExecutor executor = effectRegistry.find(effect.type()).orElse(null);
            if (executor == null) {
                report.addError(fileName, spellId, prefix + ".type", "magic.cmd.spells.errors.unknown_effect",
                        Map.of("type", effect.type()));
                continue;
            }
            EffectValidationResult validation = executor.validate(effect);
            if (!validation.isValid()) {
                Map<String, String> placeholders = new HashMap<>(validation.placeholders());
                String param = placeholders.get("param");
                String paramPath = param == null || param.isBlank() ? prefix : prefix + "." + param;
                report.addError(fileName, spellId, paramPath, validation.messageKey(), placeholders);
                continue;
            }
            EffectTargetType effectTarget = EffectTargetType.from(effect.params().get("target"));
            if (effectTarget != null && !isTargetCompatible(spell.target(), effectTarget)) {
                report.addError(fileName, spellId, prefix + ".target",
                        "magic.cmd.spells.errors.invalid_value",
                        Map.of("field", prefix + ".target", "value", String.valueOf(effectTarget)));
            }
        }
    }

    private boolean isTargetCompatible(SpellTargetDefinition target, EffectTargetType effectTarget) {
        if (target == null || effectTarget == null) {
            return false;
        }
        SpellTargetType spellTargetType = target.type();
        if (spellTargetType == null) {
            return false;
        }
        return switch (effectTarget) {
            case ENTITY -> spellTargetType == SpellTargetType.ENTITY || spellTargetType == SpellTargetType.SELF;
            case LOCATION -> spellTargetType == SpellTargetType.LOCATION
                    || spellTargetType == SpellTargetType.BLOCK
                    || spellTargetType == SpellTargetType.ENTITY
                    || spellTargetType == SpellTargetType.SELF
                    || spellTargetType == SpellTargetType.NONE;
        };
    }

    private boolean isBlank(String value) {
        return value != null && value.isBlank();
    }
}
