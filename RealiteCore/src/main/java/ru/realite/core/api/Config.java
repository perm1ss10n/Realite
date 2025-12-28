package ru.realite.core.api;

import java.util.List;

/**
 * Конфигурация модуля.
 */
public interface Config {
    String getString(String path, String def);

    int getInt(String path, int def);

    boolean getBoolean(String path, boolean def);

    double getDouble(String path, double def);

    List<String> getStringList(String path);

    boolean contains(String path);

    void set(String path, Object value);

    void save();

    void reload();
}
