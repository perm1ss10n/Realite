package ru.realite.familiars.integration.quests;

import java.util.Objects;
import java.util.logging.Logger;
import ru.realite.familiars.integration.BridgeWarning;

public final class NoopQuestsBridge implements QuestsBridge {

    private final BridgeWarning warning;

    public NoopQuestsBridge(Logger logger) {
        this.warning = new BridgeWarning(Objects.requireNonNull(logger, "logger"),
                "[Familiars] QuestsBridge not present.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void publish(FamiliarQuestEvent event) {
        warning.warnOnce();
    }
}
