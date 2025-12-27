package ru.realite.classes.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Platform;

public final class CoreAccess {

    private static CoreApi core;

    public static CoreApi core() {
        if (core == null) {
            RegisteredServiceProvider<CoreApi> rsp =
                    Bukkit.getServicesManager().getRegistration(CoreApi.class);

            if (rsp == null) {
                throw new IllegalStateException("CoreApi not found. Is RealiteCore enabled?");
            }
            core = rsp.getProvider();
        }
        return core;
    }

    public static Platform log() {
        return core().platform();
    }

    private CoreAccess() {}
}
