package ru.realite.familiars;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleProvider;

import java.util.Collection;
import java.util.List;

public final class RealiteFamiliarsModuleProvider implements ModuleProvider {
    @Override
    public Collection<Module> createModules(CoreApi core) {
        RealiteFamiliarsPlugin plugin = JavaPlugin.getPlugin(RealiteFamiliarsPlugin.class);
        return List.of(new RealiteFamiliarsModule(plugin));
    }
}
