package ru.realite.core;

import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Контекст ядра — то, что Core передаёт модулям.
 * Тут лежит доступ к Platform, папке данных, и любым общим штукам.
 */
public final class CoreContext {

    private final JavaPlugin plugin;
    private final Platform platform;
    private final Path dataDirectory;

    public CoreContext(JavaPlugin plugin, Platform platform) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.dataDirectory = plugin.getDataFolder().toPath();
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public Platform platform() {
        return platform;
    }

    public Path dataDirectory() {
        return dataDirectory;
    }
}
