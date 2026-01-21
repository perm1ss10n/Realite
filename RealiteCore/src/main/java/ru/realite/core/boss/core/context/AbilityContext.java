package ru.realite.core.boss.core.context;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public record AbilityContext(Player caster, LivingEntity target) {
}
