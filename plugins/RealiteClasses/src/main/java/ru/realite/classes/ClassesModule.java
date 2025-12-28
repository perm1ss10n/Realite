package ru.realite.classes;

import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;

import java.util.Objects;
import java.util.Set;

public final class ClassesModule implements Module {

    private final RealiteClassesPlugin plugin;
    private final ModuleMetadata metadata;

    public ClassesModule(RealiteClassesPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.metadata = new ModuleMetadata(
                new ModuleId("realite-classes"),
                "RealiteClasses",
                plugin.getDescription().getVersion(),
                Set.of()
        );
    }

    @Override
    public ModuleMetadata metadata() {
        return metadata;
    }

    @Override
    public void onLoad(ModuleContext ctx) {
        // no-op
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        plugin.initialize(ctx.core());
    }

    @Override
    public void onDisable(ModuleContext ctx) {
        plugin.shutdownModule();
    }
}
