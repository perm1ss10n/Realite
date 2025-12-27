package ru.realite.classes.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ChatTemplate {

    private static final LegacyComponentSerializer LEGACY_AMP = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SEC = LegacyComponentSerializer.legacySection();

    private ChatTemplate() {}

    /**
     * Берём строку-шаблон (legacy с &-цветами), подставляем все {vars},
     * но placeholder {itemsKey} заменяем на компонент itemsComponent.
     *
     * Пример: itemsKey = "{items}"
     */
    public static void sendWithComponent(Player player,
                                         String template,
                                         Map<String, String> vars,
                                         String itemsKey,
                                         Component itemsComponent) {

        if (player == null || template == null) return;
        if (itemsKey == null || itemsKey.isBlank()) itemsKey = "{items}";
        if (vars == null) vars = Map.of();
        if (itemsComponent == null) itemsComponent = Component.empty();

        String marker = "__ITEMS__MARKER__";

        // 1) подменяем {items} на marker
        String s = template.replace(itemsKey, marker);

        // 2) подставляем остальные переменные
        for (var e : vars.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", String.valueOf(e.getValue()));
        }

        // 3) режем по marker и склеиваем Component’ами
        String[] parts = s.split(marker, -1);

        Component msg = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                // поддерживаем и & и § на всякий случай
                Component part = deserializeLegacy(parts[i]);
                msg = msg.append(part);
            }
            if (i < parts.length - 1) {
                msg = msg.append(itemsComponent);
            }
        }

        player.sendMessage(msg);
    }

    private static Component deserializeLegacy(String text) {
        // если кто-то передал уже '§' — тоже переварим
        if (text.indexOf('§') >= 0) return LEGACY_SEC.deserialize(text);
        return LEGACY_AMP.deserialize(text);
    }
}
