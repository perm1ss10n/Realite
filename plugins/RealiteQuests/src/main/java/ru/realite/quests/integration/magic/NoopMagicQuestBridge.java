package ru.realite.quests.integration.magic;

import java.util.Optional;
import ru.realite.magic.api.MagicApi;

public final class NoopMagicQuestBridge implements MagicQuestBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<MagicApi> api() {
        return Optional.empty();
    }

    @Override
    public void refresh() {
        // no-op
    }
}
