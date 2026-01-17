package ru.realite.familiars.ui;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import ru.realite.familiars.config.Messages;

public final class FamiliarActionBarService {

    private static final Duration COOLDOWN = Duration.ofSeconds(2);

    private final Messages messages;
    private final Map<UUID, Map<String, Instant>> lastSent = new ConcurrentHashMap<>();

    public FamiliarActionBarService(Messages messages) {
        this.messages = messages;
    }

    public void send(Player player, String key) {
        send(player, key, Map.of());
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        if (player == null || key == null) {
            return;
        }
        if (!shouldSend(player.getUniqueId(), key)) {
            return;
        }
        player.sendActionBar(messages.get(key, placeholders));
    }

    public void sendForReasons(Player player, List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return;
        }
        resolveReasonKey(reasons).ifPresent(key -> send(player, key));
    }

    private boolean shouldSend(UUID playerId, String key) {
        Instant now = Instant.now();
        Map<String, Instant> playerMap = lastSent.computeIfAbsent(playerId, id -> new ConcurrentHashMap<>());
        Instant last = playerMap.get(key);
        if (last != null && last.plus(COOLDOWN).isAfter(now)) {
            return false;
        }
        playerMap.put(key, now);
        return true;
    }

    private Optional<String> resolveReasonKey(List<String> reasons) {
        for (String reason : reasons) {
            if (reason == null) {
                continue;
            }
            String lower = reason.toLowerCase();
            if (lower.contains("class") && lower.contains("not allowed")) {
                return Optional.of("actionbar.class");
            }
            if (lower.startsWith("limit reached")) {
                return Optional.of("actionbar.limit");
            }
            if (lower.contains("cooldown")) {
                return Optional.of("actionbar.cooldown");
            }
        }
        return Optional.empty();
    }
}
