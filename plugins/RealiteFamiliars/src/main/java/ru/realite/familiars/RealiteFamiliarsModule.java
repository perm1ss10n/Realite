package ru.realite.familiars;

import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;

import java.util.Objects;
import java.util.Set;

public final class RealiteFamiliarsModule implements Module {

    private final RealiteFamiliarsPlugin plugin;
    private final ModuleMetadata metadata;

    public RealiteFamiliarsModule(RealiteFamiliarsPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.metadata = new ModuleMetadata(
                new ModuleId("realite-familiars"),
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
