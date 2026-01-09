package ru.realite.magic.mastery;

import java.util.UUID;

public interface MasteryProgressRepository {

    MasteryProgress getOrCreate(UUID playerId, String spellId);

    boolean isLearned(UUID playerId, String spellId);

    void markDirty(UUID playerId);
}
