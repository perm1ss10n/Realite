package ru.realite.city.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.city.i18n.CityMessages;

import java.util.ArrayList;
import java.util.List;

public final class CityMenuItemFactory {

    private CityMenuItemFactory() {
    }

    public static ItemStack create(
            Material material,
            Component name,
            List<Component> lore,
            boolean available) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (available) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        } else {
            meta.displayName(name.color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        List<Component> renderedLore = new ArrayList<>();
        if (lore != null) {
            for (Component line : lore) {
                renderedLore.add(line.decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.lore(renderedLore);
        item.setItemMeta(meta);
        return item;
    }

    public static Component unavailableReason(CityMessages messages, String key, String fallback) {
        return messages.get(key, fallback).color(NamedTextColor.GRAY);
    }
}
