package ru.realite.classes.core;

import ru.realite.core.CoreContext;
import ru.realite.core.Platform;
import ru.realite.core.Services;

/**
 * Единая точка доступа к RealiteCore из модуля Classes.
 */
public final class CoreAccess {

    private CoreAccess() {}

    public static CoreContext core() {
        return Services.require(CoreContext.class);
    }

    public static Platform log() {
        return Services.require(Platform.class);
    }
}
