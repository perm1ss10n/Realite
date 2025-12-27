package ru.realite.core;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Реализация Platform для Paper/Spigot.
 * Оборачивает стандартный Bukkit Logger.
 */
public final class PaperPlatform implements Platform {

    private final Logger logger;

    public PaperPlatform(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
    }

    @Override
    public void debug(String message) {
        // В Bukkit нет отдельного debug-уровня, используем INFO с префиксом
        logger.info("[DEBUG] " + message);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.log(Level.SEVERE, message, t);
    }
}
