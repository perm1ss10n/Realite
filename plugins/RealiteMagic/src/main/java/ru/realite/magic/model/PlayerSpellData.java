package ru.realite.magic.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;

public final class PlayerSpellData {

    private final int version;
    private final Set<String> learned;
    @Nullable
    private String selected;

    public PlayerSpellData(int version) {
        this.version = version;
        this.learned = new HashSet<>();
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
