package ru.realite.magic.api;

import org.bukkit.entity.Player;
import ru.realite.magic.cast.CastAttemptResult;

public interface MagicCastingApi {

    CastAttemptResult tryCastSelected(Player player);

    CastAttemptResult tryCast(Player player, String spellId);
}
