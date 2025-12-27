package ru.realite.core;

/**
 * Абстракция над платформой (Paper/Spigot/и т.п.).
 * Сейчас здесь только логирование — это норм, дальше можно расширять.
 */
public interface Platform {
    void info(String message);
    void warn(String message);
    void debug(String message);
    void error(String message, Throwable t);

    default void infof(String format, Object... args) {
        info(String.format(format, args));
    }

    default void warnf(String format, Object... args) {
        warn(String.format(format, args));
    }

    default void debugf(String format, Object... args) {
        debug(String.format(format, args));
    }

    default void errorf(Throwable t, String format, Object... args) {
        error(String.format(format, args), t);
    }
}
