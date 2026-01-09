package ru.realite.magic.api;

import java.util.Optional;
import java.util.UUID;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellUnlockSource;
import ru.realite.magic.service.UnlockResult;

public interface PlayerSpellsApi {

    boolean hasSpell(UUID playerId, String spellId);

    UnlockResult unlock(UUID playerId, String spellId, SpellUnlockSource source);

    SelectResult select(UUID playerId, String spellId);

    Optional<String> selected(UUID playerId);
}
