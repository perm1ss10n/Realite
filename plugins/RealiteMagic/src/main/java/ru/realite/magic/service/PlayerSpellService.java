package ru.realite.magic.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerSpellService {

    boolean hasSpell(UUID playerId, String spellId);

    UnlockResult unlock(UUID playerId, String spellId, UnlockCause cause);

    RevokeResult revoke(UUID playerId, String spellId, UnlockCause cause);

    List<String> listLearned(UUID playerId);

    Optional<String> getSelected(UUID playerId);

    SelectResult select(UUID playerId, String spellId);

    void clearSelected(UUID playerId);

    void flush(UUID playerId);

    void flushAll();

    void evict(UUID playerId);
}
