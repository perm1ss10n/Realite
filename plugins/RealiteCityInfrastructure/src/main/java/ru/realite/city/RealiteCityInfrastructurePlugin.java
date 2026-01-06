package ru.realite.city;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;
import ru.realite.core.api.logging.Banners;

public final class RealiteCityInfrastructurePlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private final CityInfrastructureEntrypoint entrypoint = new CityInfrastructureEntrypoint();

    @Override
    public void onEnable() {
        Banners.REALITE_CITY_WAITING(this);
    }

    @Override
    public Module module() {
        return entrypoint.module();
    }
}
