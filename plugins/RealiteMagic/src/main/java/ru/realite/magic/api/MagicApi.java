package ru.realite.magic.api;

import java.util.UUID;

public interface MagicApi {

    SpellRegistryView spellRegistry();

    PlayerSpellsApi playerSpells();

    MagicCastingApi casting();

    MagicEventsApi events();

    int masteryLevel(UUID playerId, String spellId);
}
