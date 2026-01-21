package ru.realite.core.boss.core.context;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Optional;

public record DamageContext(EntityDamageEvent event, Optional<LivingEntity> damager) {
    public DamageContext {
        if (event == null) {
            throw new IllegalArgumentException("event is null");
        }
        damager = damager == null ? Optional.empty() : damager;
    }
}
