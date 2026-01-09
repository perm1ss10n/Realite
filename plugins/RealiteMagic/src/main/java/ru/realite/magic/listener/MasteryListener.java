package ru.realite.magic.listener;

import java.util.Objects;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import ru.realite.magic.api.event.SpellCastSuccessEvent;
import ru.realite.magic.mastery.MasteryService;
import ru.realite.magic.mastery.MasteryXpSource;

public final class MasteryListener implements Listener {

    private final MasteryService masteryService;

    public MasteryListener(MasteryService masteryService) {
        this.masteryService = Objects.requireNonNull(masteryService, "masteryService");
    }

    @EventHandler
    public void onCastSuccess(SpellCastSuccessEvent event) {
        int amount = masteryService.xpForSource(MasteryXpSource.CAST_SUCCESS);
        if (amount <= 0) {
            return;
        }
        masteryService.addXp(event.playerId(), event.spellId(), amount, MasteryXpSource.CAST_SUCCESS);
    }
}
