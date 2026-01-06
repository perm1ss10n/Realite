package ru.realite.core.api.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class RealiteLog {

    private final Logger logger;
    private final String tag;

    private RealiteLog(Logger logger, String tag) {
        this.logger = logger;
        this.tag = tag;
    }

    public static RealiteLog of(Logger logger, String tag) {
        return new RealiteLog(logger, tag);
    }

    public void info(String msg) { logger.info(prefix(msg)); }
    public void warn(String msg) { logger.warning(prefix(msg)); }
    public void error(String msg, Throwable t) { logger.log(Level.SEVERE, prefix(msg), t); }

    private String prefix(String msg) {
        return "[" + tag + "] " + msg;
    }
}
