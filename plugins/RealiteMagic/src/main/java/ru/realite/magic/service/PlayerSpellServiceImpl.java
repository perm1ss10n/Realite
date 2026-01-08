package ru.realite.magic.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.PlayerSpellData;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.storage.PlayerSpellStorage;

public final class PlayerSpellServiceImpl implements PlayerSpellService {

    private final PlayerSpellStorage storage;
    private final SpellRegistry registry;
    private final MagicMessages messages;
    private final Map<UUID, PlayerSpellData> cache = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public PlayerSpellServiceImpl(PlayerSpellStorage storage,
                                  SpellRegistry registry,
                                  MagicMessages messages) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @Override
    public boolean hasSpell(UUID playerId, String spellId) {
        return data(playerId).isLearned(spellId);
    }

    @Override
    public UnlockResult unlock(UUID playerId, String spellId, UnlockCause cause) {
        String normalized = normalize(spellId);
        SpellDefinition spell = resolveSpell(normalized);
        if (spell == null) {
            return new UnlockResult.Fail(SpellActionReason.UNKNOWN_SPELL);
        }
        PlayerSpellData data = data(playerId);
        if (data.isLearned(normalized)) {
            return new UnlockResult.Ok(true);
        }
        data.learn(normalized);
        markDirty(playerId);
        return new UnlockResult.Ok(false);
    }

    @Override
    public RevokeResult revoke(UUID playerId, String spellId, UnlockCause cause) {
        String normalized = normalize(spellId);
        SpellDefinition spell = resolveSpell(normalized);
        if (spell == null) {
            return new RevokeResult.Fail(SpellActionReason.UNKNOWN_SPELL);
        }
        PlayerSpellData data = data(playerId);
        if (!data.isLearned(normalized)) {
            return new RevokeResult.Ok(false);
        }
        data.unlearn(normalized);
        data.selected().ifPresent(selected -> {
            if (selected.equals(normalized)) {
                data.selected(null);
            }
        });
        markDirty(playerId);
        return new RevokeResult.Ok(true);
    }

    @Override
    public List<String> listLearned(UUID playerId) {
        List<String> learned = new ArrayList<>(data(playerId).learned());
        learned.sort(String::compareToIgnoreCase);
        return learned;
    }

    @Override
    public Optional<String> getSelected(UUID playerId) {
        return data(playerId).selected();
    }

    @Override
    public SelectResult select(UUID playerId, String spellId) {
        String normalized = normalize(spellId);
        SpellDefinition spell = resolveSpell(normalized);
        if (spell == null) {
            return new SelectResult.Fail(SpellActionReason.UNKNOWN_SPELL);
        }
        PlayerSpellData data = data(playerId);
        if (!data.isLearned(normalized)) {
            return new SelectResult.Fail(SpellActionReason.NOT_LEARNED);
        }
        data.selected(normalized);
        markDirty(playerId);
        return new SelectResult.Ok();
    }

    @Override
    public void clearSelected(UUID playerId) {
        PlayerSpellData data = data(playerId);
        if (data.selected().isEmpty()) {
            return;
        }
        data.selected(null);
        markDirty(playerId);
    }

    @Override
    public void flush(UUID playerId) {
        if (!dirty.contains(playerId)) {
            return;
        }
        PlayerSpellData data = cache.get(playerId);
        if (data == null) {
            dirty.remove(playerId);
            return;
        }
        storage.save(playerId, data);
        dirty.remove(playerId);
    }

    @Override
    public void flushAll() {
        Set<UUID> pending = new HashSet<>(dirty);
        for (UUID playerId : pending) {
            flush(playerId);
        }
    }

    @Override
    public void evict(UUID playerId) {
        cache.remove(playerId);
        dirty.remove(playerId);
    }

    private PlayerSpellData data(UUID playerId) {
        return cache.computeIfAbsent(playerId, storage::load);
    }

    private void markDirty(UUID playerId) {
        dirty.add(playerId);
    }

    private SpellDefinition resolveSpell(String spellId) {
        if (spellId == null) {
            return null;
        }
        return registry.get(spellId);
    }

    private String normalize(String spellId) {
        if (spellId == null) {
            return null;
        }
        String trimmed = spellId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String displayName(String spellId) {
        if (spellId == null) {
            return "";
        }
        SpellDefinition spell = registry.get(spellId);
        if (spell == null) {
            return spellId;
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        return messages.raw(nameKey);
    }
}
