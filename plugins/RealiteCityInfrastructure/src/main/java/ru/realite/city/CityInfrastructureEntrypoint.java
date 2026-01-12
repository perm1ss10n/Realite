package ru.realite.city;

import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;

public final class CityInfrastructureEntrypoint implements CoreModuleEntrypoint {

    private final Module module = new CityInfrastructureModule();

    @Override
    public Module module() {
        return module;
    }
}
