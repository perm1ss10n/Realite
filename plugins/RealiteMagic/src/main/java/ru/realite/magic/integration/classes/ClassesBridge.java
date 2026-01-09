package ru.realite.magic.integration.classes;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface ClassesBridge {

    @Nullable String getActiveClassId(Player player);

    @Nullable String getActiveEvolutionId(Player player);

    Component displayClassName(String classId);

    Component displayEvolutionName(String evolutionId);
}
