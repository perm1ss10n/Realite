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
import javax.annotation.Nullable;
import ru.realite.magic.api.event.SpellSelectedEvent;
import ru.realite.magic.api.event.SpellUnlockedEvent;
import ru.realite.magic.integration.events.MagicEventPublisher;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.PlayerSpellData;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.storage.PlayerSpellStorage;

public final class PlayerSpellServiceImpl implements PlayerSpellService {

    private static final String SLOT_INVALID = "magic.slot.invalid";
    private static final String SLOT_SET_FAIL = "magic.slot.set.fail";

    private final PlayerSpellStorage storage;
    private final SpellRegistry registry;
    private final MagicMessages messages;
    private final MagicEventPublisher eventPublisher;
    private final Map<UUID, PlayerSpellData> cache = new HashMap<>();
    private final Set<UUID> dirty = new HashSet<>();

    public PlayerSpellServiceImpl(PlayerSpellStorage storage,
                                  SpellRegistry registry,
                                  MagicMessages messages,
                                  MagicEventPublisher eventPublisher) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.registry = Objects.requireNonNull(registry, "registry");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    @Override
    public boolean hasSpell(UUID playerId, String spellId) {
        return data(playerId).isLearned(spellId);
    }

    @Override
    public UnlockResult unlock(UUID playerId, String spellId, SpellUnlockSource source) {
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
        eventPublisher.publish(new SpellUnlockedEvent(playerId, normalized, source));
        return new UnlockResult.Ok(false);
    }

    @Override
    public RevokeResult revoke(UUID playerId, String spellId, SpellUnlockSource source) {
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
        markDirty(playerId);
        boolean removedFromSlots = clearSlotsForSpell(data, normalized);
        if (removedFromSlots) {
            markDirty(playerId);
        }
        String selected = data.selected().orElse(null);
        if (selected != null && selected.equals(normalized)) {
            updateSelected(playerId, data, null);
        }
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
        int activeSlot = data.activeSlot();
        data.slot(activeSlot, normalized);
        markDirty(playerId);
        updateSelected(playerId, data, normalized);
        return new SelectResult.Ok();
    }

    @Override
    public void clearSelected(UUID playerId) {
        PlayerSpellData data = data(playerId);
        String previous = data.selected().orElse(null);
        if (previous == null) {
            return;
        }
        int activeSlot = data.activeSlot();
        data.slot(activeSlot, null);
        markDirty(playerId);
        updateSelected(playerId, data, null);
    }

    @Override
    public Optional<String> getSlot(UUID playerId, int slot) {
        return data(playerId).slot(slot);
    }

    @Override
    public SetSlotResult setSlot(UUID playerId, int slot, @Nullable String spellId) {
        if (!isValidSlot(slot)) {
            return new SetSlotResult.Fail(SLOT_INVALID);
        }
        String normalized = normalize(spellId);
        PlayerSpellData data = data(playerId);
        if (normalized != null) {
            if (resolveSpell(normalized) == null) {
                return new SetSlotResult.Fail(SLOT_SET_FAIL);
            }
            if (!data.isLearned(normalized)) {
                return new SetSlotResult.Fail(SLOT_SET_FAIL);
            }
        }
        String previous = data.slot(slot).orElse(null);
        if (Objects.equals(previous, normalized)) {
            return new SetSlotResult.Ok();
        }
        data.slot(slot, normalized);
        markDirty(playerId);
        if (data.activeSlot() == slot) {
            updateSelected(playerId, data, normalized);
        }
        return new SetSlotResult.Ok();
    }

    @Override
    public int getActiveSlot(UUID playerId) {
        return data(playerId).activeSlot();
    }

    @Override
    public SetActiveSlotResult setActiveSlot(UUID playerId, int slot) {
        if (!isValidSlot(slot)) {
            return new SetActiveSlotResult.Fail(SLOT_INVALID);
        }
        PlayerSpellData data = data(playerId);
        if (data.activeSlot() == slot) {
            return new SetActiveSlotResult.Ok();
        }
        data.activeSlot(slot);
        markDirty(playerId);
        String spellId = data.slot(slot).orElse(null);
        updateSelected(playerId, data, spellId);
        return new SetActiveSlotResult.Ok();
    }

    @Override
    public Optional<String> getActiveSlotSpell(UUID playerId) {
        PlayerSpellData data = data(playerId);
        return data.slot(data.activeSlot());
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

    private boolean isValidSlot(int slot) {
        return slot >= 1 && slot <= 9;
    }

    private void updateSelected(UUID playerId, PlayerSpellData data, @Nullable String next) {
        String previous = data.selected().orElse(null);
        if (Objects.equals(previous, next)) {
            return;
        }
        data.selected(next);
        markDirty(playerId);
        eventPublisher.publish(new SpellSelectedEvent(playerId, previous, next));
    }

    private boolean clearSlotsForSpell(PlayerSpellData data, String spellId) {
        boolean changed = false;
        for (int slot = 1; slot <= 9; slot++) {
            Optional<String> current = data.slot(slot);
            if (current.isPresent() && current.get().equals(spellId)) {
                data.slot(slot, null);
                changed = true;
            }
        }
        return changed;
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
