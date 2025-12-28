package ru.realite.city;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;

public final class RealiteCityInfrastructurePlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private final CityInfrastructureEntrypoint entrypoint = new CityInfrastructureEntrypoint();

    @Override
    public void onEnable() {
        getLogger().info("RealiteCityInfrastructure loaded. Waiting for module enable.");
    }

    @Override
    public Module module() {
        return entrypoint.module();
    }
}
