package ru.realite.guilds.menu;

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.ui.menu.BaseMenu;

public abstract class GuildMenu extends BaseMenu {

    protected final GuildMenuManager manager;
    protected final GuildMessages messages;

    protected GuildMenu(GuildMenuManager manager, GuildMessages messages, int size, String titleKey) {
        super(size, messages.msg(titleKey));
        this.messages = messages;
        this.manager = manager;
    }

    protected void setButton(int slot, Material material, String nameKey, List<String> loreKeys, Consumer<Player> action) {
        List<Component> lore = loreKeys == null || loreKeys.isEmpty()
                ? null
                : loreKeys.stream().map(messages::msg).toList();
        super.setButton(slot, material, messages.msg(nameKey), lore, action);
    }

    protected void setButton(int slot, Material material, Component name, List<Component> lore, Consumer<Player> action) {
        super.setButton(slot, material, name, lore, action);
    }
}
