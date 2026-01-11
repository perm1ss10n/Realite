package ru.realite.guilds.menu;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.realite.guilds.i18n.GuildMessages;

public abstract class GuildMenu implements InventoryHolder {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    protected final GuildMenuManager manager;
    protected final GuildMessages messages;
    protected final Inventory inventory;
    private final Map<Integer, Consumer<Player>> actions = new HashMap<>();

    protected GuildMenu(GuildMenuManager manager, GuildMessages messages, int size, String titleKey) {
        this.manager = manager;
        this.messages = messages;
        this.inventory = Bukkit.createInventory(this, size, messages.msg(titleKey));
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(Player player, int slot) {
        Consumer<Player> action = actions.get(slot);
        if (action != null) {
            action.accept(player);
        }
    }

    protected void fillFiller(Material material) {
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            filler.setItemMeta(meta);
        }
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    protected void setButton(int slot, Material material, String nameKey, List<String> loreKeys, Consumer<Player> action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(messages.msg(nameKey));
            if (loreKeys != null && !loreKeys.isEmpty()) {
                meta.lore(loreKeys.stream().map(messages::msg).toList());
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected void setButton(int slot, Material material, Component name, List<Component> lore, Consumer<Player> action) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
    }

    protected Component colored(String text) {
        return LEGACY.deserialize(text);
    }
}
