package ru.realite.quests;

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleProvider;

import java.util.Collection;
import java.util.List;

public final class RealiteQuestsModuleProvider implements ModuleProvider {
    @Override
    public Collection<Module> createModules(CoreApi core) {
        return List.of(new QuestsModule());
    }
}
