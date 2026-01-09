package ru.realite.magic.integration.classes;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class NoopClassesBridge implements ClassesBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public @Nullable String getActiveClassId(Player player) {
        return null;
    }

    @Override
    public @Nullable String getActiveEvolutionId(Player player) {
        return null;
    }

    @Override
    public Component displayClassName(String classId) {
        if (classId == null || classId.isBlank()) {
            return Component.empty();
        }
        return Component.text(classId);
    }

    @Override
    public Component displayEvolutionName(String evolutionId) {
        if (evolutionId == null || evolutionId.isBlank()) {
            return Component.empty();
        }
        return Component.text(evolutionId);
    }
}
