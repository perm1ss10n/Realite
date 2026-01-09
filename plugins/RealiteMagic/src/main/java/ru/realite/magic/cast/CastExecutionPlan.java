package ru.realite.magic.cast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.realite.magic.spell.SpellDefinition;

public record CastExecutionPlan(SpellDefinition spell,
                                Player caster,
                                List<LivingEntity> targets,
                                Location origin,
                                @Nullable Location impactLocation,
                                @Nullable LivingEntity primaryTarget,
                                Map<String, Object> meta) {

    public CastExecutionPlan {
        Objects.requireNonNull(spell, "spell");
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(origin, "origin");
        targets = targets == null ? List.of() : List.copyOf(targets);
        meta = meta == null ? Map.of() : Map.copyOf(new HashMap<>(meta));
    }
}
