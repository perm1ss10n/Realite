package ru.realite.classes;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleProvider;

import java.util.Collection;
import java.util.List;

public final class RealiteClassesModuleProvider implements ModuleProvider {
    @Override
    public Collection<Module> createModules(CoreApi core) {
        RealiteClassesPlugin plugin = JavaPlugin.getPlugin(RealiteClassesPlugin.class);
        return List.of(new ClassesModule(plugin));
    }
}
