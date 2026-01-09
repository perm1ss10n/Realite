package ru.realite.magic.api.impl;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import ru.realite.magic.api.PlayerSpellsApi;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.SelectResult;
import ru.realite.magic.service.SpellUnlockSource;
import ru.realite.magic.service.UnlockResult;

public final class PlayerSpellsApiImpl implements PlayerSpellsApi {

    private final PlayerSpellService playerSpellService;

    public PlayerSpellsApiImpl(PlayerSpellService playerSpellService) {
        this.playerSpellService = Objects.requireNonNull(playerSpellService, "playerSpellService");
    }

    @Override
    public boolean hasSpell(UUID playerId, String spellId) {
        return playerSpellService.hasSpell(playerId, spellId);
    }

    @Override
    public UnlockResult unlock(UUID playerId, String spellId, SpellUnlockSource source) {
        return playerSpellService.unlock(playerId, spellId, source);
    }

    @Override
    public SelectResult select(UUID playerId, String spellId) {
        return playerSpellService.select(playerId, spellId);
    }

    @Override
    public Optional<String> selected(UUID playerId) {
        return playerSpellService.getSelected(playerId);
    }
}
