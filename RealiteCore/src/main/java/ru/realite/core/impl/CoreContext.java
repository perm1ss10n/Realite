package ru.realite.core.impl;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.EventBus;
import ru.realite.core.api.Platform;
import ru.realite.core.api.Services;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Контекст ядра — то, что Core передаёт модулям.
 * Тут лежит доступ к Platform, папке данных, и любым общим штукам.
 */
public final class CoreContext implements CoreApi {

    private final JavaPlugin plugin;
    private final Platform platform;
    private final Services services;
    private final EventBus eventBus;
    private final Path dataDirectory;

    public CoreContext(JavaPlugin plugin, Platform platform, Services services, EventBus eventBus) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.services = Objects.requireNonNull(services, "services");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.dataDirectory = plugin.getDataFolder().toPath();
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    @Override
    public Platform platform() {
        return platform;
    }

    @Override
    public Services services() {
        return services;
    }

    @Override
    public EventBus events() {
        return eventBus;
    }

    @Override
    public Path dataDirectory() {
        return dataDirectory;
    }
}
