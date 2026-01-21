package ru.realite.core.boss.core;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.context.DamageContext;
import ru.realite.core.boss.core.context.DeathContext;
import ru.realite.core.boss.core.context.SpawnContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRealiteBoss implements RealiteBoss {
    private final String bossId;
    private final UUID instanceId;
    private final List<BossPhase> phases;
    private final List<BossAbility> abilities;
    private final double maxHp;

    private LivingEntity entity;
    private double hp;
    private BossPhase phase;

    protected AbstractRealiteBoss(String bossId,
                                 double maxHp,
                                 List<BossPhase> phases,
                                 List<BossAbility> abilities) {
        if (bossId == null || bossId.isBlank()) {
            throw new IllegalArgumentException("bossId is blank");
        }
        if (maxHp <= 0) {
            throw new IllegalArgumentException("maxHp must be positive");
        }
        this.bossId = bossId;
        this.instanceId = UUID.randomUUID();
        this.maxHp = maxHp;
        this.phases = normalizePhases(phases);
        this.abilities = new ArrayList<>(Objects.requireNonNullElse(abilities, List.of()));
        this.phase = this.phases.isEmpty() ? null : this.phases.get(this.phases.size() - 1);
    }

    @Override
    public String bossId() {
        return bossId;
    }

    @Override
    public UUID instanceId() {
        return instanceId;
    }

    @Override
    public LivingEntity getEntity() {
        return entity;
    }

    @Override
    public boolean isAlive() {
        return entity != null && !entity.isDead();
    }

    @Override
    public double getMaxHp() {
        return maxHp;
    }

    @Override
    public double getHp() {
        return hp;
    }

    @Override
    public void setHp(double hp) {
        double clamped = Math.max(0.0, Math.min(maxHp, hp));
        this.hp = clamped;
        if (entity != null) {
            AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double maxAllowed = attribute == null ? clamped : attribute.getValue();
            entity.setHealth(Math.min(clamped, maxAllowed));
        }
    }

    @Override
    public BossPhase getPhase() {
        return phase;
    }

    @Override
    public void spawn(SpawnContext ctx) {
        entity = spawnEntity(ctx);
        if (entity == null) {
            throw new IllegalStateException("spawnEntity returned null for " + bossId);
        }
        applyMaxHealth(entity);
        setHp(maxHp);
        phase = resolvePhase();
        if (phase != null) {
            phase.onEnter(this);
        }
        onSpawned(ctx);
    }

    @Override
    public void tick() {
        syncHpFromEntity();
        updatePhaseIfNeeded();
        for (BossAbility ability : abilities) {
            ability.tick(this);
        }
    }

    @Override
    public void despawn(DespawnReason reason) {
        if (entity != null) {
            entity.remove();
        }
        entity = null;
    }

    @Override
    public void onDamage(DamageContext ctx) {
        syncHpFromEntity();
    }

    @Override
    public void onDeath(DeathContext ctx) {
    }

    protected abstract LivingEntity spawnEntity(SpawnContext ctx);

    protected void onSpawned(SpawnContext ctx) {
    }

    protected List<BossAbility> abilities() {
        return abilities;
    }

    protected void updatePhaseIfNeeded() {
        BossPhase next = resolvePhase();
        if (next == null || next == phase) {
            return;
        }
        if (phase != null) {
            phase.onExit(this);
        }
        phase = next;
        phase.onEnter(this);
    }

    protected BossPhase resolvePhase() {
        if (phases.isEmpty()) {
            return null;
        }
        double hpPct = maxHp <= 0 ? 0.0 : (hp / maxHp);
        for (BossPhase candidate : phases) {
            if (candidate.shouldEnter(hpPct)) {
                return candidate;
            }
        }
        return phases.get(phases.size() - 1);
    }

    protected void applyMaxHealth(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(maxHp);
        }
        entity.setHealth(Math.min(maxHp, attribute == null ? maxHp : attribute.getValue()));
    }

    protected void syncHpFromEntity() {
        if (entity == null) {
            return;
        }
        this.hp = Math.min(maxHp, entity.getHealth());
    }

    private List<BossPhase> normalizePhases(List<BossPhase> phases) {
        List<BossPhase> data = new ArrayList<>(Objects.requireNonNullElse(phases, List.of()));
        data.sort(Comparator.comparingDouble(BossPhase::enterWhenHpPctAtMost));
        return data;
    }
}
