package ru.realite.classes.util;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Map;

public final class ChatTemplate {

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
                                         BaseComponent itemsComponent) {

        String marker = "__ITEMS__MARKER__";

        // 1) подменяем {items} на marker
        String s = template.replace(itemsKey, marker);

        // 2) подставляем остальные переменные
        for (var e : vars.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }

        // 3) режем по marker и склеиваем компонентами
        String[] parts = s.split(marker, -1);

        TextComponent msg = new TextComponent("");
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                msg.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', parts[i])));
            }
            if (i < parts.length - 1) {
                msg.addExtra(itemsComponent);
            }
        }

        player.spigot().sendMessage(msg);
    }
}
