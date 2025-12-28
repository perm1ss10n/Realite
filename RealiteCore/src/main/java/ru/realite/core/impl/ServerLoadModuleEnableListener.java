package ru.realite.core.impl;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.HandlerList;
import ru.realite.core.api.ModuleManager;
import ru.realite.core.api.Platform;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class ServerLoadModuleEnableListener implements Listener {

    private final ModuleManager modules;
    private final Platform platform;
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    ServerLoadModuleEnableListener(ModuleManager modules, Platform platform) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (!enabled.compareAndSet(false, true)) {
            return;
        }
        platform.info("Server loaded. Enabling core modules...");
        modules.enableAll();
        HandlerList.unregisterAll(this);
    }
}
