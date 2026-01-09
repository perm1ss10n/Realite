package ru.realite.magic.admin.override;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MagicOverrideService {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final Map<UUID, Map<BypassType, Instant>> overrides = new java.util.concurrent.ConcurrentHashMap<>();

    public boolean bypassRequirements(UUID playerId) {
        return isActive(playerId, BypassType.REQUIREMENTS);
    }

    public boolean bypassCooldown(UUID playerId) {
        return isActive(playerId, BypassType.COOLDOWN);
    }

    public boolean bypassMana(UUID playerId) {
        return isActive(playerId, BypassType.MANA);
    }

    public boolean bypassReagents(UUID playerId) {
        return isActive(playerId, BypassType.REAGENTS);
    }

    public boolean bypassEconomy(UUID playerId) {
        return isActive(playerId, BypassType.ECONOMY);
    }

    public boolean bypassStaffCharges(UUID playerId) {
        return isActive(playerId, BypassType.STAFF);
    }

    public void setBypass(UUID playerId, BypassType type, boolean enabled, Duration ttl) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(type, "type");
        if (!enabled) {
            clearBypass(playerId, type);
            return;
        }
        Duration effectiveTtl = ttl == null || ttl.isNegative() || ttl.isZero() ? DEFAULT_TTL : ttl;
        Instant expiresAt = Instant.now().plus(effectiveTtl);
        overrides.compute(playerId, (id, map) -> {
            Map<BypassType, Instant> next = map == null ? new EnumMap<>(BypassType.class) : map;
            next.put(type, expiresAt);
            return next;
        });
    }

    public void clearBypass(UUID playerId, BypassType type) {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(type, "type");
        overrides.computeIfPresent(playerId, (id, map) -> {
            map.remove(type);
            return map.isEmpty() ? null : map;
        });
    }

    public void setBypassAll(UUID playerId, boolean enabled, Duration ttl) {
        for (BypassType type : BypassType.values()) {
            setBypass(playerId, type, enabled, ttl);
        }
    }

    public Map<BypassType, Instant> list(UUID playerId) {
        Map<BypassType, Instant> map = overrides.get(playerId);
        if (map == null) {
            return Map.of();
        }
        cleanupExpired(playerId, map);
        map = overrides.get(playerId);
        return map == null ? Map.of() : Map.copyOf(map);
    }

    public Map<UUID, Map<BypassType, Instant>> listAll() {
        Map<UUID, Map<BypassType, Instant>> snapshot = new java.util.HashMap<>();
        for (Map.Entry<UUID, Map<BypassType, Instant>> entry : overrides.entrySet()) {
            cleanupExpired(entry.getKey(), entry.getValue());
            Map<BypassType, Instant> active = overrides.get(entry.getKey());
            if (active != null && !active.isEmpty()) {
                snapshot.put(entry.getKey(), Map.copyOf(active));
            }
        }
        return snapshot;
    }

    private boolean isActive(UUID playerId, BypassType type) {
        if (playerId == null || type == null) {
            return false;
        }
        Map<BypassType, Instant> map = overrides.get(playerId);
        if (map == null) {
            return false;
        }
        Instant expiresAt = map.get(type);
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            clearBypass(playerId, type);
            return false;
        }
        return true;
    }

    private void cleanupExpired(UUID playerId, Map<BypassType, Instant> map) {
        Instant now = Instant.now();
        map.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
        if (map.isEmpty()) {
            overrides.remove(playerId);
        }
    }

    public enum BypassType {
        REQUIREMENTS,
        COOLDOWN,
        MANA,
        REAGENTS,
        ECONOMY,
        STAFF
    }
}
