package ru.realite.familiars.integration.classes;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface ClassesBridge {

    boolean isAvailable();

    @Nullable String getActiveClassId(Player player);
}
