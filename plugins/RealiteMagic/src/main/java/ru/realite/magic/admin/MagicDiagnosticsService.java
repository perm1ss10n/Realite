package ru.realite.magic.admin;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import ru.realite.magic.school.MagicSchool;
import ru.realite.magic.spell.ReagentItem;

public final class MagicDiagnosticsService {

    private final Map<String, LongAdder> castsBySpellId = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> failsByReasonKey = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pveImmuneByEffectType = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pveResistHitsBySchool = new ConcurrentHashMap<>();
    private final Deque<CastLogEntry> logEntries;
    private final int maxEntries;

    public MagicDiagnosticsService(int maxEntries) {
        this.maxEntries = Math.max(1, maxEntries);
        this.logEntries = new ArrayDeque<>(this.maxEntries);
    }

    public void recordSuccess(CastLogEntry entry) {
        if (entry == null) {
            return;
        }
        if (entry.spellId() != null) {
            increment(castsBySpellId, entry.spellId());
        }
        addLog(entry);
    }

    public void recordFail(CastLogEntry entry) {
        if (entry == null) {
            return;
        }
        if (entry.reasonKey() != null) {
            increment(failsByReasonKey, entry.reasonKey());
        }
        addLog(entry);
    }

    public void recordPveImmune(String effectType) {
        if (effectType == null || effectType.isBlank()) {
            return;
        }
        increment(pveImmuneByEffectType, effectType.trim().toLowerCase());
    }

    public void recordPveResistHit(MagicSchool school) {
        MagicSchool resolved = school == null ? MagicSchool.NONE : school;
        increment(pveResistHitsBySchool, resolved.name());
    }

    public List<CounterEntry> topCasts(int limit) {
        return topEntries(castsBySpellId, limit);
    }

    public List<CounterEntry> topFails(int limit) {
        return topEntries(failsByReasonKey, limit);
    }

    public List<CounterEntry> topPveImmunes(int limit) {
        return topEntries(pveImmuneByEffectType, limit);
    }

    public List<CounterEntry> topPveResistHits(int limit) {
        return topEntries(pveResistHitsBySchool, limit);
    }

    public long totalCasts() {
        return sumEntries(castsBySpellId);
    }

    public long totalFails() {
        return sumEntries(failsByReasonKey);
    }

    public List<CastLogEntry> recentLogs(LogFilter filter, int limit) {
        int safeLimit = Math.max(1, limit);
        List<CastLogEntry> result = new ArrayList<>();
        synchronized (logEntries) {
            for (CastLogEntry entry : logEntries) {
                if (!matches(entry, filter)) {
                    continue;
                }
                result.add(entry);
            }
        }
        int size = result.size();
        if (size <= safeLimit) {
            return result;
        }
        return result.subList(size - safeLimit, size);
    }

    private boolean matches(CastLogEntry entry, LogFilter filter) {
        if (filter == null || entry == null) {
            return true;
        }
        if (filter.playerName() != null) {
            String entryName = entry.player();
            if (entryName == null || !entryName.equalsIgnoreCase(filter.playerName())) {
                return false;
            }
        }
        if (filter.spellId() != null) {
            String entrySpell = entry.spellId();
            if (entrySpell == null || !entrySpell.equalsIgnoreCase(filter.spellId())) {
                return false;
            }
        }
        if (filter.failOnly() && entry.success()) {
            return false;
        }
        return true;
    }

    private void addLog(CastLogEntry entry) {
        synchronized (logEntries) {
            if (logEntries.size() >= maxEntries) {
                logEntries.removeFirst();
            }
            logEntries.addLast(entry);
        }
    }

    private void increment(Map<String, LongAdder> map, String key) {
        map.computeIfAbsent(key, ignored -> new LongAdder()).increment();
    }

    private List<CounterEntry> topEntries(Map<String, LongAdder> map, int limit) {
        int safeLimit = Math.max(1, limit);
        List<CounterEntry> entries = new ArrayList<>();
        for (Map.Entry<String, LongAdder> entry : map.entrySet()) {
            entries.add(new CounterEntry(entry.getKey(), entry.getValue().sum()));
        }
        entries.sort(Comparator.comparingLong(CounterEntry::count).reversed());
        if (entries.size() > safeLimit) {
            return entries.subList(0, safeLimit);
        }
        return entries;
    }

    private long sumEntries(Map<String, LongAdder> map) {
        long sum = 0L;
        for (LongAdder adder : map.values()) {
            sum += adder.sum();
        }
        return sum;
    }

    public record CastLogEntry(Instant time,
                               String player,
                               String spellId,
                               boolean success,
                               String reasonKey,
                               String targetType,
                               String school,
                               double manaCost,
                               long cooldownTicks,
                               String regionId,
                               int staffChargesUsed,
                               List<ReagentItem> reagentsUsed,
                               double moneyCost) {
        public CastLogEntry {
            Objects.requireNonNull(time, "time");
            reagentsUsed = reagentsUsed == null ? List.of() : List.copyOf(reagentsUsed);
        }
    }

    public record CounterEntry(String key, long count) {
    }

    public record LogFilter(String playerName, String spellId, boolean failOnly) {
        public static LogFilter empty() {
            return new LogFilter(null, null, false);
        }

        public Optional<String> normalizedPlayer() {
            return normalize(playerName);
        }

        public Optional<String> normalizedSpell() {
            return normalize(spellId);
        }

        private Optional<String> normalize(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(value.trim());
        }
    }
}
