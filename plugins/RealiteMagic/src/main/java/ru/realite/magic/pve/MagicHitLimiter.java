package ru.realite.magic.pve;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class MagicHitLimiter {

    private final Map<HitKey, Deque<Long>> hitsByKey = new HashMap<>();

    public boolean allowHit(UUID caster, UUID target, EntityMagicProfile profile) {
        Objects.requireNonNull(profile, "profile");
        int maxHits = profile.maxHitsPerWindow();
        int windowMs = profile.windowMs();
        if (maxHits <= 0 || windowMs <= 0) {
            return true;
        }
        if (caster == null || target == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        HitKey key = new HitKey(caster, target);
        Deque<Long> hits = hitsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        while (!hits.isEmpty() && now - hits.peekFirst() >= windowMs) {
            hits.removeFirst();
        }
        if (hits.size() >= maxHits) {
            if (hits.isEmpty()) {
                hitsByKey.remove(key);
            }
            return false;
        }
        hits.addLast(now);
        return true;
    }

    private record HitKey(UUID caster, UUID target) {
    }
}
