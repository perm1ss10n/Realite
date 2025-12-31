package ru.realite.quests.i18n;

import ru.realite.core.api.Config;
import ru.realite.core.api.ModuleContext;
import ru.realite.quests.util.Text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class QuestsMessages {

    private final Config messages;

    public QuestsMessages(ModuleContext ctx, String lang, ClassLoader cl) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(cl, "cl");
        String target = lang == null || lang.isBlank() ? "ru" : lang.trim().toLowerCase(Locale.ROOT);
        String resource = "lang/messages_" + target + ".yml";
        this.messages = ctx.configs().loadOrCreateDefault(
                ctx.dataFolder().resolve(resource),
                resource,
                cl
        );
    }

    public String raw(String key) {
        return messages.getString(key, "&cMissing message: &f" + key);
    }

    public String get(String key) {
        return Text.c(raw(key));
    }

    public String format(String key, Map<String, String> placeholders) {
        return Text.c(formatRaw(key, placeholders));
    }

    public String formatRaw(String key, Map<String, String> placeholders) {
        String raw = raw(key);
        for (var entry : toMap(placeholders).entrySet()) {
            raw = raw.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return raw;
    }

    private Map<String, String> toMap(Map<String, String> placeholders) {
        if (placeholders == null) {
            return new HashMap<>();
        }
        return placeholders;
    }
}
