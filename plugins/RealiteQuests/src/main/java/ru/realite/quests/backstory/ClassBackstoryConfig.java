package ru.realite.quests.backstory;

import ru.realite.core.api.Config;

import java.util.List;
import java.util.Locale;

public final class ClassBackstoryConfig {

    private final Config config;

    public ClassBackstoryConfig(Config config) {
        this.config = config;
    }

    public ClassBackstoryDefinition get(String classId) {
        String normalized = normalize(classId);
        if (normalized == null) {
            return null;
        }
        String base = "classes." + normalized + ".";
        if (!config.contains("classes." + normalized)) {
            return null;
        }
        String title = config.getString(base + "title", normalized);
        List<String> pages = config.getStringList(base + "pages");
        String introQuestId = config.getString(base + "introQuestId", null);
        if (introQuestId != null && introQuestId.isBlank()) {
            introQuestId = null;
        }
        return new ClassBackstoryDefinition(normalized, title, pages, introQuestId);
    }

    private String normalize(String classId) {
        if (classId == null) {
            return null;
        }
        String trimmed = classId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
