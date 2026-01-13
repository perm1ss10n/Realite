package ru.realite.core.api.ui;

import java.util.Optional;
import org.bukkit.entity.Player;

public interface UiScreenRegistry {
    void register(UiScreen screen);

    Optional<UiScreen> screen(String id);

    boolean open(Player player, String target);
}
