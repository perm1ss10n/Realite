package ru.realite.core.api.ui;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;

public interface UiScreen {
    String id();

    void open(Player player, @Nullable String payload);

    default void open(Player player) {
        open(player, null);
    }
}
