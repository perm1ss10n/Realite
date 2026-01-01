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
        List<String> postIntroQuests = normalizeQuestList(config.getStringList(base + "postIntroQuests"));
        return new ClassBackstoryDefinition(normalized, title, pages, introQuestId, postIntroQuests);
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

    private List<String> normalizeQuestList(List<String> questIds) {
        if (questIds == null || questIds.isEmpty()) {
            return List.of();
        }
        return questIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .toList();
    }
}
