package ru.realite.familiars;

import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;

import java.util.Objects;

public final class RealiteFamiliarsEntrypoint implements CoreModuleEntrypoint {

    private final Module module;

    public RealiteFamiliarsEntrypoint(RealiteFamiliarsPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.module = new RealiteFamiliarsModule(plugin);
    }

    @Override
    public Module module() {
        return module;
    }
}
