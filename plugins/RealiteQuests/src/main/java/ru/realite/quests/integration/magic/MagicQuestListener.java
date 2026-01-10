package ru.realite.quests.integration.magic;

import java.util.Objects;
import java.util.function.Supplier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import ru.realite.core.api.quests.QuestService;
import ru.realite.magic.api.event.SpellCastSuccessEvent;
import ru.realite.magic.api.event.SpellMasteryLevelUpEvent;
import ru.realite.magic.api.event.SpellUnlockedEvent;
import ru.realite.quests.service.QuestServiceImpl;

public final class MagicQuestListener implements Listener {

    private final Supplier<QuestService> questServiceSupplier;

    public MagicQuestListener(Supplier<QuestService> questServiceSupplier) {
        this.questServiceSupplier = Objects.requireNonNull(questServiceSupplier, "questServiceSupplier");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpellUnlocked(SpellUnlockedEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleSpellUnlocked(event.playerId(), event.spellId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpellCastSuccess(SpellCastSuccessEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleSpellCastSuccess(event.playerId(), event.spellId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpellMasteryLevelUp(SpellMasteryLevelUpEvent event) {
        QuestService service = questServiceSupplier.get();
        if (!(service instanceof QuestServiceImpl questService)) {
            return;
        }
        questService.handleSpellMasteryLevelUp(event.playerId(), event.spellId(), event.newLevel());
    }
}
