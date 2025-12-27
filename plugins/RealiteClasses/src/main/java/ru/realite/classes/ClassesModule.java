package ru.realite.classes;

import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;

import java.util.Objects;

public final class ClassesModule implements Module {

    private final RealiteClassesPlugin plugin;

    public ClassesModule(RealiteClassesPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public String id() {
        return "classes";
    }

    @Override
    public void onEnable(CoreApi core) {
        plugin.initialize(core);
    }

    @Override
    public void onDisable() {
        plugin.shutdownModule();
    }
}
