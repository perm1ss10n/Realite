package ru.realite.familiars.integration.classes;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface ClassesBridge {

    boolean isAvailable();

    @Nullable ClassTierInfo getActiveClassInfo(Player player);

    default @Nullable String getActiveClassId(Player player) {
        ClassTierInfo info = getActiveClassInfo(player);
        if (info == null) {
            return null;
        }
        return info.classId();
    }
}
