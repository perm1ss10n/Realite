package ru.realite.core.impl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;
import ru.realite.core.api.Platform;

import java.util.List;
import java.util.Objects;

/**
 * Адаптер модуля для Bukkit-плагина.
 */
public final class BukkitPluginModuleAdapter implements Module {

    private final String moduleId;
    private final String pluginName;
    private final List<String> dependsOn;
    private Module delegate;
    private boolean enabled;

    public BukkitPluginModuleAdapter(String moduleId, String pluginName, List<String> dependsOn) {
        this.moduleId = Objects.requireNonNull(moduleId, "moduleId");
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName");
        this.dependsOn = List.copyOf(Objects.requireNonNull(dependsOn, "dependsOn"));
        if (moduleId.isBlank()) {
            throw new IllegalArgumentException("moduleId is blank");
        }
        if (pluginName.isBlank()) {
            throw new IllegalArgumentException("pluginName is blank");
        }
    }

    @Override
    public String id() {
        return moduleId;
    }

    @Override
    public List<String> dependsOn() {
        return dependsOn;
    }

    @Override
    public void onEnable(CoreApi core) throws Exception {
        if (enabled) {
            core.platform().warn("Module adapter '" + moduleId + "' is already enabled.");
            return;
        }
        Platform log = core.platform();
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin plugin = pluginManager.getPlugin(pluginName);
        if (plugin == null) {
            throw new IllegalStateException("Plugin not found: " + pluginName);
        }

        if (!plugin.isEnabled()) {
            throw new IllegalStateException(
                    "Plugin '" + pluginName + "' is not enabled for module '" + moduleId + "'. " +
                            "Enable the plugin before activating this module."
            );
        }

        if (!(plugin instanceof CoreModuleEntrypoint entrypoint)) {
            throw new IllegalStateException("Plugin '" + pluginName + "' does not implement CoreModuleEntrypoint");
        }

        Module module = entrypoint.module();
        if (module == null) {
            throw new IllegalStateException("Entrypoint module is null for plugin '" + pluginName + "'");
        }

        this.delegate = module;
        module.onEnable(core);
        enabled = true;
    }

    @Override
    public void onDisable() throws Exception {
        if (!enabled || delegate == null) {
            return;
        }
        delegate.onDisable();
        enabled = false;
    }
}
