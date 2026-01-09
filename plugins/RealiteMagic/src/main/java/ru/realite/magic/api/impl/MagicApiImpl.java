package ru.realite.magic.api.impl;

import java.util.Objects;
import ru.realite.magic.api.MagicApi;
import ru.realite.magic.api.MagicCastingApi;
import ru.realite.magic.api.MagicEventsApi;
import ru.realite.magic.api.PlayerSpellsApi;
import ru.realite.magic.api.SpellRegistryView;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicApiImpl implements MagicApi {

    private final SpellRegistryView spellRegistry;
    private final PlayerSpellsApi playerSpells;
    private final MagicCastingApi casting;
    private final MagicEventsApi events;

    public MagicApiImpl(SpellRegistry registry,
                        PlayerSpellService playerSpellService,
                        MagicService magicService) {
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(playerSpellService, "playerSpellService");
        Objects.requireNonNull(magicService, "magicService");
        this.spellRegistry = new SpellRegistryViewImpl(registry);
        this.playerSpells = new PlayerSpellsApiImpl(playerSpellService);
        this.casting = new MagicCastingApiImpl(magicService, registry);
        this.events = new MagicEventsApiImpl();
    }

    @Override
    public SpellRegistryView spellRegistry() {
        return spellRegistry;
    }

    @Override
    public PlayerSpellsApi playerSpells() {
        return playerSpells;
    }

    @Override
    public MagicCastingApi casting() {
        return casting;
    }

    @Override
    public MagicEventsApi events() {
        return events;
    }
}
