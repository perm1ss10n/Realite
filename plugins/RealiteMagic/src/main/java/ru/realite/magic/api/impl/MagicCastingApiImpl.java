package ru.realite.magic.api.impl;

import java.util.Objects;
import org.bukkit.entity.Player;
import ru.realite.magic.api.MagicCastingApi;
import ru.realite.magic.cast.CastAttemptResult;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicCastingApiImpl implements MagicCastingApi {

    private final MagicService magicService;
    private final SpellRegistry spellRegistry;

    public MagicCastingApiImpl(MagicService magicService, SpellRegistry spellRegistry) {
        this.magicService = Objects.requireNonNull(magicService, "magicService");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
    }

    @Override
    public CastAttemptResult tryCastSelected(Player player) {
        return magicService.tryCastSelected(player);
    }

    @Override
    public CastAttemptResult tryCast(Player player, String spellId) {
        SpellDefinition spell = spellRegistry.find(spellId).orElse(null);
        return magicService.tryCast(player, spell);
    }
}
