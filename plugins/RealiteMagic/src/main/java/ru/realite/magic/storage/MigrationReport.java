package ru.realite.magic.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MigrationReport {

    private final List<String> entries = new ArrayList<>();

    public void recordInitialized(String field, Object value) {
        record("initialized", field, value);
    }

    public void recordDefault(String field, Object value) {
        record("default", field, value);
    }

    public void recordNote(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        entries.add(message);
    }

    public boolean hasEntries() {
        return !entries.isEmpty();
    }

    public List<String> entries() {
        return Collections.unmodifiableList(entries);
    }

    private void record(String type, String field, Object value) {
        if (field == null || field.isBlank()) {
            return;
        }
        String rendered = value == null ? "null" : String.valueOf(value);
        entries.add(type + " " + field + "=" + rendered);
    }
}
