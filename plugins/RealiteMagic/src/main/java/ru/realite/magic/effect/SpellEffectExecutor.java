package ru.realite.magic.effect;

public interface SpellEffectExecutor {

    String type();

    EffectValidationResult validate(SpellEffectDefinition def);

    void execute(EffectContext ctx, SpellEffectDefinition def);
}
