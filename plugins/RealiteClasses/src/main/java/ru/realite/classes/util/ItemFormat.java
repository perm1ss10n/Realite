package ru.realite.classes.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.classes.model.ItemAmount;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class ItemFormat {

    private ItemFormat() {}

    /** Красивое имя предмета: кастомное displayName -> I18N name (если есть) -> из MATERIAL_ENUM */
    public static String displayName(Material material) {
        if (material == null) return "Item";

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        // 1) Если у предмета есть displayName (обычно не будет для ванильных, но пусть будет)
        if (meta != null && meta.hasDisplayName()) {
            return stripColor(meta.getDisplayName());
        }

        // 2) Попробуем ItemStack#getI18NDisplayName() (есть в CraftBukkit/Paper, но не везде)
        try {
            Method m = ItemStack.class.getMethod("getI18NDisplayName");
            Object res = m.invoke(stack);
            if (res instanceof String s && !s.isBlank()) {
                return s;
            }
        } catch (Throwable ignored) {
            // если метода нет — ок, идём дальше
        }

        // 3) Фоллбек: IRON_INGOT -> Iron Ingot
        return prettifyEnum(material.name());
    }

    public static String format(ItemAmount ia) {
        if (ia == null) return "-";
        String name = displayName(ia.material());
        return name + " ×" + ia.amount();
    }

    public static String formatList(List<ItemAmount> items) {
        if (items == null || items.isEmpty()) return "-";
        return items.stream()
                .map(ItemFormat::format)
                .collect(Collectors.joining(", "));
    }

    private static String prettifyEnum(String enumName) {
        String s = enumName.toLowerCase(Locale.ROOT).replace('_', ' ');
        // Title Case
        StringBuilder out = new StringBuilder(s.length());
        boolean up = true;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (up && Character.isLetter(c)) {
                out.append(Character.toUpperCase(c));
                up = false;
            } else {
                out.append(c);
            }
            if (c == ' ') up = true;
        }
        return out.toString();
    }

    private static String stripColor(String s) {
        // если где-то прилетит §-цвет
        return s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
    }
}
