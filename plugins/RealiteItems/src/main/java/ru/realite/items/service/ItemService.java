package ru.realite.items.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.items.ItemPdcKeys;

import ru.realite.items.i18n.ItemMessages;
import ru.realite.items.model.ItemDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ItemService {

    public static final NamespacedKey ITEM_ID_KEY = ItemPdcKeys.ITEM_ID;
    public static final NamespacedKey UID_KEY = ItemPdcKeys.UID;

    private final JavaPlugin plugin;
    private final ItemRegistry registry;
    private final ItemMessages messages;

    public ItemService(JavaPlugin plugin, ItemRegistry registry, ItemMessages messages) {
        this.plugin = plugin;
        this.registry = registry;
        this.messages = messages;
    }

    public ItemStack create(String itemId, int amount) {
        ItemDefinition definition = registry.get(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown item id: " + itemId));

        ItemStack stack = new ItemStack(definition.material(), Math.max(1, amount));
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(ITEM_ID_KEY, PersistentDataType.STRING, definition.id());
            if (definition.unstackable()) {
                container.set(UID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
            }
            stack.setItemMeta(meta);
        }
        render(stack);
        return stack;
    }

    public Optional<String> getItemId(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return Optional.empty();
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        return Optional.ofNullable(container.get(ITEM_ID_KEY, PersistentDataType.STRING));
    }

    public boolean isItem(ItemStack stack, String itemId) {
        return getItemId(stack).map(itemId::equals).orElse(false);
    }

    public ItemStack render(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return stack;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        String itemId = container.get(ITEM_ID_KEY, PersistentDataType.STRING);
        if (itemId == null) {
            return stack;
        }
        ItemDefinition def = registry.get(itemId).orElse(null);
        if (def == null) {
            return stack;
        }

        if (def.nameKey() != null && !def.nameKey().isBlank()) {
            meta.displayName(messages.get(def.nameKey(), ""));
        }

        if (def.loreKeys() != null && !def.loreKeys().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String loreKey : def.loreKeys()) {
                Component line = messages.get(loreKey, "");
                if (!line.equals(Component.empty())) {
                    lore.add(line);
                }
            }
            meta.lore(lore);
        }

        if (def.customModelData() != null && def.customModelData() > 0) {
            meta.setCustomModelData(def.customModelData());
        }

        meta.setEnchantmentGlintOverride(def.glow());

        if (def.unstackable() && container.get(UID_KEY, PersistentDataType.STRING) == null) {
            container.set(UID_KEY, PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        applyTags(container, def);

        stack.setItemMeta(meta);
        return stack;
    }

    private void applyTags(PersistentDataContainer container, ItemDefinition def) {
        if (container == null || def == null || def.tags() == null || def.tags().isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : def.tags().entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            NamespacedKey namespacedKey = new NamespacedKey("realite", key);
            if (container.has(namespacedKey)) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof Number number) {
                if (value instanceof Float || value instanceof Double) {
                    container.set(namespacedKey, PersistentDataType.DOUBLE, number.doubleValue());
                } else {
                    container.set(namespacedKey, PersistentDataType.INTEGER, number.intValue());
                }
                continue;
            }
            if (value instanceof Boolean bool) {
                container.set(namespacedKey, PersistentDataType.INTEGER, bool ? 1 : 0);
                continue;
            }
            if (value != null) {
                container.set(namespacedKey, PersistentDataType.STRING, String.valueOf(value));
            }
        }
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}
