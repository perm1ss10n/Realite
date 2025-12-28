package ru.realite.quests;

import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;

public final class RealiteQuestsEntrypoint implements CoreModuleEntrypoint {

    private final Module module = new QuestsModule();

    @Override
    public Module module() {
        return module;
    }
}
