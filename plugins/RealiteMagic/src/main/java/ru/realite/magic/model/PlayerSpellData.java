package ru.realite.magic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Set;
import javax.annotation.Nullable;
import ru.realite.magic.mastery.MasteryProgress;

public final class PlayerSpellData {

    private final int version;
    private final Set<String> learned;
    private final List<String> slots;
    private final Map<String, MasteryProgress> mastery;
    private int activeSlot;
    @Nullable
    private String selected;

    public PlayerSpellData(int version) {
        this.version = version;
        this.learned = new HashSet<>();
        this.slots = new ArrayList<>(Collections.nCopies(9, null));
        this.mastery = new TreeMap<>();
        this.activeSlot = 1;
    }

    public int version() {
        return version;
    }

    public Set<String> learned() {
        return Collections.unmodifiableSet(learned);
    }

    public boolean isLearned(String spellId) {
        String normalized = normalize(spellId);
        return normalized != null && learned.contains(normalized);
    }

    public void learn(String spellId) {
        String normalized = normalize(spellId);
        if (normalized != null) {
            learned.add(normalized);
        }
    }

    public void unlearn(String spellId) {
        String normalized = normalize(spellId);
        if (normalized != null) {
            learned.remove(normalized);
        }
    }

    public Optional<String> selected() {
        return Optional.ofNullable(selected);
    }

    public void selected(@Nullable String spellId) {
        this.selected = normalize(spellId);
    }

    public List<String> slots() {
        return Collections.unmodifiableList(slots);
    }

    public Optional<String> slot(int slot) {
        int index = slotIndex(slot);
        if (index < 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(slots.get(index));
    }

    public void slot(int slot, @Nullable String spellId) {
        int index = slotIndex(slot);
        if (index < 0) {
            return;
        }
        slots.set(index, normalize(spellId));
    }

    public void slots(List<String> slots) {
        if (slots == null) {
            return;
        }
        for (int i = 0; i < this.slots.size(); i++) {
            String value = i < slots.size() ? slots.get(i) : null;
            this.slots.set(i, normalize(value));
        }
    }

    public int activeSlot() {
        return activeSlot;
    }

    public void activeSlot(int slot) {
        if (slotIndex(slot) < 0) {
            return;
        }
        this.activeSlot = slot;
    }

    public Map<String, MasteryProgress> mastery() {
        return Collections.unmodifiableMap(mastery);
    }

    public MasteryProgress masteryProgress(String spellId) {
        String normalized = normalize(spellId);
        if (normalized == null) {
            return new MasteryProgress(1, 0, 0, 0, 0);
        }
        return mastery.computeIfAbsent(normalized, id -> new MasteryProgress(1, 0, 0, 0, 0));
    }

    public void mastery(String spellId, MasteryProgress progress) {
        String normalized = normalize(spellId);
        if (normalized == null || progress == null) {
            return;
        }
        mastery.put(normalized, progress);
    }

    private int slotIndex(int slot) {
        if (slot < 1 || slot > 9) {
            return -1;
        }
        return slot - 1;
    }

    @Nullable
    private static String normalize(@Nullable String spellId) {
        if (spellId == null) {
            return null;
        }
        String trimmed = spellId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
