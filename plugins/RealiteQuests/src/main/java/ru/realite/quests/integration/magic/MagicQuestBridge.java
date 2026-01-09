package ru.realite.quests.integration.magic;

import java.util.Optional;
import ru.realite.magic.api.MagicApi;

public interface MagicQuestBridge {

    boolean isAvailable();

    Optional<MagicApi> api();

    void refresh();
}
