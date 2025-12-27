package ru.realite.classes;

import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;

import java.util.Objects;

public final class RealiteClassesEntrypoint implements CoreModuleEntrypoint {

    private final Module module;

    public RealiteClassesEntrypoint(RealiteClassesPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        this.module = new ClassesModule(plugin);
    }

    @Override
    public Module module() {
        return module;
    }
}
