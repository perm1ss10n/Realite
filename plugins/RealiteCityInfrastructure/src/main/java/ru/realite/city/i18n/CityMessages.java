package ru.realite.city.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import ru.realite.core.api.Config;

import java.util.Map;

public final class CityMessages {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final Config config;

    public CityMessages(Config config) {
        this.config = config;
    }

    /** Получить сообщение как Component */
    public Component get(String key, String def) {
        String raw = config.getString(key, def);
        if (raw == null) raw = "";
        return LEGACY.deserialize(raw);
    }

    /** Отправить сообщение без плейсхолдеров */
    public void send(CommandSender sender, String key, String def) {
        sender.sendMessage(get(key, def));
    }

    /** Отправить сообщение с плейсхолдерами {var} */
    public void send(CommandSender sender, String key, String def, Map<String, String> vars) {
        String raw = config.getString(key, def);
        if (raw == null) raw = "";

        for (var e : vars.entrySet()) {
            raw = raw.replace("{" + e.getKey() + "}", e.getValue());
        }

        sender.sendMessage(LEGACY.deserialize(raw));
    }
}
