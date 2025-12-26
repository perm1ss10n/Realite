package ru.realite.classes.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import org.bukkit.Material;
import ru.realite.classes.model.ItemAmount;

import java.util.ArrayList;
import java.util.List;

public final class ItemComponents {

    private ItemComponents() {}

    /** item.minecraft.* или block.minecraft.* */
    public static String translationKey(Material mat) {
        if (mat == null) return "item.minecraft.air";
        String id = mat.getKey().getKey(); // например iron_ingot
        // Для блоков ключ block.minecraft.*, иначе item.minecraft.*
        return (mat.isBlock() ? "block.minecraft." : "item.minecraft.") + id;
    }

    /** "Iron Ingot ×12" (но имя локализуется на клиенте) */
    public static BaseComponent one(ItemAmount ia) {
        if (ia == null || ia.material() == null) return new TextComponent("-");
        TranslatableComponent name = new TranslatableComponent(translationKey(ia.material()));
        TextComponent tail = new TextComponent(" ×" + ia.amount());
        name.addExtra(tail);
        return name;
    }

    /** "Iron Ingot ×12, Feather ×3" (локализация на клиенте) */
    public static BaseComponent listOrDash(List<ItemAmount> items) {
        if (items == null || items.isEmpty()) return new TextComponent("-");

        List<BaseComponent> parts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) parts.add(new TextComponent(", "));
            parts.add(one(items.get(i)));
        }

        TextComponent out = new TextComponent("");
        for (BaseComponent p : parts) out.addExtra(p);
        return out;
    }
}
