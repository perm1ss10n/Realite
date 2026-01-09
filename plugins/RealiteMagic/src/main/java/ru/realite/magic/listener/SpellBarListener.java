package ru.realite.magic.listener;

import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.realite.magic.cast.WarnLimiter;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class SpellBarListener implements Listener {

    private static final String WARN_KEY = "slot_change";
    private static final long WARN_WINDOW_MS = 200L;

    private final PlayerSpellService playerSpellService;
    private final SpellRegistry spellRegistry;
    private final MagicMessages messages;
    private final WarnLimiter warnLimiter = new WarnLimiter();

    public SpellBarListener(PlayerSpellService playerSpellService,
                            SpellRegistry spellRegistry,
                            MagicMessages messages) {
        this.playerSpellService = playerSpellService;
        this.spellRegistry = spellRegistry;
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        int direction = scrollDirection(event.getPreviousSlot(), event.getNewSlot());
        if (direction == 0) {
            return;
        }
        UUID playerId = player.getUniqueId();
        int current = playerSpellService.getActiveSlot(playerId);
        int next = wrapSlot(current + direction);
        playerSpellService.setActiveSlot(playerId, next);
        sendSlotActionbar(player, playerId, next);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        warnLimiter.clear(event.getPlayer().getUniqueId());
    }

    private void sendSlotActionbar(Player player, UUID playerId, int slot) {
        if (!warnLimiter.canWarn(playerId, WARN_KEY, WARN_WINDOW_MS)) {
            return;
        }
        String spellId = playerSpellService.getActiveSlotSpell(playerId).orElse(null);
        if (spellId == null) {
            player.sendActionBar(messages.msg("magic.bar.slot.empty", "slot", String.valueOf(slot)));
            return;
        }
        String spellName = displaySpellName(spellId);
        player.sendActionBar(messages.msg("magic.bar.slot.changed",
                "slot", String.valueOf(slot),
                "spell", spellName));
    }

    private String displaySpellName(String spellId) {
        SpellDefinition spell = spellRegistry.get(spellId);
        if (spell == null) {
            return spellId;
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        String raw = messages.raw(nameKey);
        return raw == null || raw.isBlank() ? spell.id() : raw;
    }

    private int scrollDirection(int previousSlot, int newSlot) {
        int delta = (newSlot - previousSlot + 9) % 9;
        if (delta == 1) {
            return 1;
        }
        if (delta == 8) {
            return -1;
        }
        return 0;
    }

    private int wrapSlot(int slot) {
        if (slot < 1) {
            return 9;
        }
        if (slot > 9) {
            return 1;
        }
        return slot;
    }
}
