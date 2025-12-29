package ru.realite.city;

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleProvider;

import java.util.Collection;
import java.util.List;

public final class CityModuleProvider implements ModuleProvider {
    @Override
    public Collection<Module> createModules(CoreApi core) {
        return List.of(new CityInfrastructureModule());
    }
}
