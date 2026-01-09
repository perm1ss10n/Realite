package ru.realite.magic.debug;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.target.SpellTargetType;

public final class DebugService {

    private final MagicMessages messages;
    private final ConcurrentMap<UUID, DebugSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> castsBySpell = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> failsByReason = new ConcurrentHashMap<>();

    public DebugService(MagicMessages messages) {
        this.messages = messages;
    }

    public boolean enableGlobal(Player admin) {
        DebugSession session = sessions.computeIfAbsent(admin.getUniqueId(), id -> new DebugSession());
        session.enabled = true;
        return true;
    }

    public boolean disableGlobal(Player admin) {
        DebugSession session = sessions.get(admin.getUniqueId());
        if (session == null) {
            return false;
        }
        session.enabled = false;
        cleanupSession(admin.getUniqueId(), session);
        return true;
    }

    public boolean togglePlayer(Player admin, UUID targetId) {
        DebugSession session = sessions.computeIfAbsent(admin.getUniqueId(), id -> new DebugSession());
        if (session.players.remove(targetId)) {
            cleanupSession(admin.getUniqueId(), session);
            return false;
        }
        session.players.add(targetId);
        return true;
    }

    public void recordSuccess(String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return;
        }
        castsBySpell.computeIfAbsent(spellId, key -> new LongAdder()).increment();
    }

    public void recordFailure(String reasonKey) {
        if (reasonKey == null || reasonKey.isBlank()) {
            return;
        }
        failsByReason.computeIfAbsent(reasonKey, key -> new LongAdder()).increment();
    }

    public void logCast(Player caster,
                        String spellId,
                        SpellTargetType targetType,
                        double mana,
                        long globalCooldownTicks,
                        long spellCooldownTicks,
                        @Nullable String reasonKey) {
        if (sessions.isEmpty()) {
            return;
        }
        UUID casterId = caster.getUniqueId();
        List<UUID> stale = new ArrayList<>();
        for (var entry : sessions.entrySet()) {
            UUID adminId = entry.getKey();
            DebugSession session = entry.getValue();
            if (!session.enabled && !session.players.contains(casterId)) {
                continue;
            }
            Player admin = Bukkit.getPlayer(adminId);
            if (admin == null) {
                stale.add(adminId);
                continue;
            }
            String manaFormatted = formatNumber(mana);
            String targetValue = targetType == null ? "NONE" : targetType.name();
            String reasonValue = reasonKey == null ? "ok" : reasonKey;
            admin.sendMessage(messages.msg("magic.cmd.debug.cast",
                    "player", caster.getName(),
                    "spell", spellId,
                    "target", targetValue,
                    "mana", manaFormatted,
                    "globalCooldown", String.valueOf(globalCooldownTicks),
                    "spellCooldown", String.valueOf(spellCooldownTicks),
                    "reason", reasonValue));
        }
        for (UUID id : stale) {
            sessions.remove(id);
        }
    }

    public void sendStats(CommandSender sender) {
        sendStatsBlock(sender, "casts", castsBySpell);
        sendStatsBlock(sender, "fails", failsByReason);
    }

    private void sendStatsBlock(CommandSender sender, String type, ConcurrentMap<String, LongAdder> data) {
        sender.sendMessage(messages.msg("magic.cmd.debug.stats.header",
                "type", type));
        List<Map.Entry<String, Long>> entries = topEntries(data, 5);
        if (entries.isEmpty()) {
            sender.sendMessage(messages.msg("magic.cmd.debug.stats.entry",
                    "key", "-",
                    "value", "0"));
            return;
        }
        for (Map.Entry<String, Long> entry : entries) {
            sender.sendMessage(messages.msg("magic.cmd.debug.stats.entry",
                    "key", entry.getKey(),
                    "value", String.valueOf(entry.getValue())));
        }
    }

    private List<Map.Entry<String, Long>> topEntries(ConcurrentMap<String, LongAdder> data, int limit) {
        List<Map.Entry<String, Long>> entries = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : data.entrySet()) {
            entries.add(Map.entry(entry.getKey(), entry.getValue().sum()));
        }
        entries.sort(Comparator.comparingLong(Map.Entry<String, Long>::getValue).reversed());
        if (entries.size() > limit) {
            return entries.subList(0, limit);
        }
        return entries;
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private void cleanupSession(UUID adminId, DebugSession session) {
        if (!session.enabled && session.players.isEmpty()) {
            sessions.remove(adminId);
        }
    }

    private static final class DebugSession {
        private boolean enabled;
        private final Set<UUID> players = ConcurrentHashMap.newKeySet();
    }
}
