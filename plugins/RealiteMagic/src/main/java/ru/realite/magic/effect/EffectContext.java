package ru.realite.magic.effect;

import java.util.Objects;
import java.util.Random;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.target.SpellTarget;

public record EffectContext(Player caster,
                            SpellDefinition spell,
                            SpellTarget target,
                            BalanceModifiers modifiers,
                            Random rng,
                            MagicService magicService) {

    public EffectContext {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(modifiers, "modifiers");
        Objects.requireNonNull(rng, "rng");
        Objects.requireNonNull(magicService, "magicService");
    }

    public World world() {
        return caster.getWorld();
    }

    public Location casterLocation() {
        return caster.getLocation();
    }
}
