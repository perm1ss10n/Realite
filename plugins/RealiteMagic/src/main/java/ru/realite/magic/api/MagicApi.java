package ru.realite.magic.api;

public interface MagicApi {

    SpellRegistryView spellRegistry();

    PlayerSpellsApi playerSpells();

    MagicCastingApi casting();

    MagicEventsApi events();
}
