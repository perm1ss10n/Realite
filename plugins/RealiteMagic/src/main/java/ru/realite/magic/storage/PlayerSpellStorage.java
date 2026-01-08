package ru.realite.magic.storage;

import java.util.UUID;
import ru.realite.magic.model.PlayerSpellData;

public interface PlayerSpellStorage {

    PlayerSpellData load(UUID playerId);

    void save(UUID playerId, PlayerSpellData data);
}
