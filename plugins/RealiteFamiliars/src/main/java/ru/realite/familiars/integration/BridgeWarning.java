package ru.realite.familiars.integration;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public final class BridgeWarning {

    private final Logger logger;
    private final String message;
    private final AtomicBoolean warned = new AtomicBoolean(false);

    public BridgeWarning(Logger logger, String message) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.message = Objects.requireNonNull(message, "message");
    }

    public void warnOnce() {
        if (warned.compareAndSet(false, true)) {
            logger.warning(message);
        }
    }
}
