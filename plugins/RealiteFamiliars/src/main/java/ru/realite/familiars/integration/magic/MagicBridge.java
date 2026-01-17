package ru.realite.familiars.integration.magic;

import org.bukkit.entity.Player;
import ru.realite.familiars.model.FamiliarInstance;

public interface MagicBridge {

    boolean isAvailable();

    void refresh(Player player, FamiliarInstance instance);

    void clear(Player player, FamiliarInstance instance);
}
