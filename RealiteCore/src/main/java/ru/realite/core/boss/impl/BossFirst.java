package ru.realite.core.boss.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.core.AbstractRealiteBoss;
import ru.realite.core.boss.core.context.SpawnContext;

import java.util.List;

public final class BossFirst extends AbstractRealiteBoss {
    public static final String ID = "boss_first";

    public BossFirst() {
        super(ID, 200.0, List.of(
                new BossPhase("PHASE_1", 1.0),
                new BossPhase("PHASE_2", 0.5)
        ), List.of());
    }

    @Override
    protected LivingEntity spawnEntity(SpawnContext ctx) {
        Location location = ctx.location();
        if (location.getWorld() == null) {
            throw new IllegalStateException("Spawn location has no world");
        }
        return location.getWorld().spawn(location, Zombie.class, spawned -> {
            spawned.customName(Component.text("First Boss"));
            spawned.setCustomNameVisible(true);
        });
    }
}
