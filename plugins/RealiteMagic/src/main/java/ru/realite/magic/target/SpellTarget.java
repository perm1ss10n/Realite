package ru.realite.magic.target;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public sealed interface SpellTarget permits SpellTarget.Self, SpellTarget.EntityTarget,
        SpellTarget.BlockTarget, SpellTarget.LocationTarget {

    record Self(Player player) implements SpellTarget {
    }

    record EntityTarget(LivingEntity entity) implements SpellTarget {
    }

    record BlockTarget(Block block, Location location) implements SpellTarget {
    }

    record LocationTarget(Location location) implements SpellTarget {
    }
}
