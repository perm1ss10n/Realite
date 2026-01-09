package ru.realite.magic.cast;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WarnLimiter {

    private final Map<UUID, Map<String, Long>> lastWarns = new HashMap<>();

    public boolean canWarn(UUID playerId, String key, long windowMs) {
        long now = System.currentTimeMillis();
        Map<String, Long> playerWarns = lastWarns.computeIfAbsent(playerId, id -> new HashMap<>());
        Long last = playerWarns.get(key);
        if (last != null && now - last < windowMs) {
            return false;
        }
        playerWarns.put(key, now);
        return true;
    }

    public void clear(UUID playerId) {
        lastWarns.remove(playerId);
    }
}
