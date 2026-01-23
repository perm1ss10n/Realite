package ru.realite.core.boss.api;

import org.bukkit.entity.LivingEntity;
import ru.realite.core.boss.core.DespawnReason;
import ru.realite.core.boss.core.context.DamageContext;
import ru.realite.core.boss.core.context.DeathContext;
import ru.realite.core.boss.core.context.SpawnContext;

import java.util.UUID;

public interface RealiteBoss {
    String bossId();

    UUID instanceId();

    LivingEntity getEntity();

    boolean isAlive();

    double getMaxHp();

    double getHp();

    void setHp(double hp);

    BossPhase getPhase();

    void spawn(SpawnContext ctx);

    void tick();

    void despawn(DespawnReason reason);

    void onDamage(DamageContext ctx);

    void onDeath(DeathContext ctx);

    default double bossBarRange() {
        return 48.0;
    }
}
