package ru.realite.core;

public interface Platform {
    void info(String message);
    void warn(String message);
    void error(String message, Throwable t);
}
