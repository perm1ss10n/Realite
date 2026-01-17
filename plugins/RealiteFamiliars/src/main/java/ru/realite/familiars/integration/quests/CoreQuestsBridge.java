package ru.realite.familiars.integration.quests;

import java.util.Objects;
import ru.realite.core.api.EventBus;

public final class CoreQuestsBridge implements QuestsBridge {

    private final EventBus eventBus;

    public CoreQuestsBridge(EventBus eventBus) {
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void publish(FamiliarQuestEvent event) {
        if (event == null) {
            return;
        }
        eventBus.publish(event);
    }
}
