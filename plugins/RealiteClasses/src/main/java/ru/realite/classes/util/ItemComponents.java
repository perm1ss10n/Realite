package ru.realite.classes.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import ru.realite.classes.model.ItemAmount;

import java.util.List;

public final class ItemComponents {

    private ItemComponents() {}

    /** item.minecraft.* или block.minecraft.* */
    public static String translationKey(Material mat) {
        if (mat == null) return "item.minecraft.air";
        String id = mat.getKey().getKey(); // например iron_ingot
        return (mat.isBlock() ? "block.minecraft." : "item.minecraft.") + id;
    }

    /** "Iron Ingot ×12" (имя локализуется на клиенте) */
    public static Component one(ItemAmount ia) {
        if (ia == null || ia.material() == null) return Component.text("-");

        Component name = Component.translatable(translationKey(ia.material()));
        Component tail = Component.text(" ×" + ia.amount());
        return name.append(tail);
    }

    /** "Iron Ingot ×12, Feather ×3" (локализация на клиенте) */
    public static Component listOrDash(List<ItemAmount> items) {
        if (items == null || items.isEmpty()) return Component.text("-");

        Component out = Component.empty();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) out = out.append(Component.text(", "));
            out = out.append(one(items.get(i)));
        }
        return out;
    }
}
