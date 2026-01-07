package ru.realite.items.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import ru.realite.items.i18n.ItemMessages;
import ru.realite.items.model.ItemDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ItemService {

    public static final NamespacedKey ITEM_ID_KEY = new NamespacedKey("realite", "item_id");
    public static final NamespacedKey UID_KEY = new NamespacedKey("realite", "uid");

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

        stack.setItemMeta(meta);
        return stack;
    }

    public JavaPlugin plugin() {
        return plugin;
    }
}
