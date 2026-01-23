package ru.realite.core.boss.core.context;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Optional;

public record DeathContext(EntityDeathEvent event, Optional<LivingEntity> killer) {
    public DeathContext {
        if (event == null) {
            throw new IllegalArgumentException("event is null");
        }
        killer = killer == null ? Optional.empty() : killer;
    }
}
