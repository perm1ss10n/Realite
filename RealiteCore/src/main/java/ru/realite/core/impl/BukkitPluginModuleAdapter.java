package ru.realite.core.impl;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.Platform;

import java.util.Objects;

/**
 * Адаптер модуля для Bukkit-плагина.
 */
public final class BukkitPluginModuleAdapter implements Module {

    private final ModuleMetadata metadata;
    private final String pluginName;
    private Module delegate;

    public BukkitPluginModuleAdapter(ModuleMetadata metadata, String pluginName) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.pluginName = Objects.requireNonNull(pluginName, "pluginName");
        if (pluginName.isBlank()) {
            throw new IllegalArgumentException("pluginName is blank");
        }
    }

    @Override
    public ModuleMetadata metadata() {
        return metadata;
    }

    @Override
    public void onLoad(ModuleContext ctx) throws Exception {
        if (delegate != null) {
            return;
        }
        Platform log = ctx.logger();
        PluginManager pluginManager = Bukkit.getPluginManager();
        Plugin plugin = pluginManager.getPlugin(pluginName);
        if (plugin == null) {
            throw new IllegalStateException("Plugin not found: " + pluginName);
        }

        if (!(plugin instanceof CoreModuleEntrypoint entrypoint)) {
            throw new IllegalStateException("Plugin '" + pluginName + "' does not implement CoreModuleEntrypoint");
        }

        Module module = entrypoint.module();
        if (module == null) {
            throw new IllegalStateException("Entrypoint module is null for plugin '" + pluginName + "'");
        }

        if (!module.metadata().id().equals(metadata.id())) {
            log.warn("Module metadata mismatch for plugin '" + pluginName + "': adapter="
                    + metadata.id() + ", module=" + module.metadata().id());
        }

        this.delegate = module;
        delegate.onLoad(ctx);
    }

    @Override
    public void onEnable(ModuleContext ctx) throws Exception {
        if (delegate == null) {
            onLoad(ctx);
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin == null || !plugin.isEnabled()) {
            throw new IllegalStateException(
                    "Plugin '" + pluginName + "' is not enabled for module '" + metadata.id() + "'."
            );
        }
        delegate.onEnable(ctx);
    }

    @Override
    public void onDisable(ModuleContext ctx) throws Exception {
        if (delegate == null) {
            return;
        }
        delegate.onDisable(ctx);
    }
}
