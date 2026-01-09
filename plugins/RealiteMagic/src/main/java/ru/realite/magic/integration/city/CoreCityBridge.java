package ru.realite.magic.integration.city;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.integrations.CityAccessHook;
import ru.realite.core.api.quests.CityAdapter;

public final class CoreCityBridge implements CityBridge {

    private final Supplier<CityAccessHook> cityAccessHookSupplier;
    private final Supplier<CityAdapter> cityAdapterSupplier;

    public CoreCityBridge() {
        this(CoreCityBridge::resolveCityAccessHook, CoreCityBridge::resolveCityAdapter);
    }

    public CoreCityBridge(Supplier<CityAccessHook> cityAccessHookSupplier,
                          Supplier<CityAdapter> cityAdapterSupplier) {
        this.cityAccessHookSupplier = Objects.requireNonNull(cityAccessHookSupplier, "cityAccessHookSupplier");
        this.cityAdapterSupplier = Objects.requireNonNull(cityAdapterSupplier, "cityAdapterSupplier");
    }

    @Override
    public boolean isAvailable() {
        return cityAccessHookSupplier.get() != null || cityAdapterSupplier.get() != null;
    }

    @Override
    public Optional<RegionInfo> regionAt(Location location) {
        if (location == null) {
            return Optional.empty();
        }
        CityAccessHook accessHook = cityAccessHookSupplier.get();
        CityAdapter cityAdapter = cityAdapterSupplier.get();
        if (accessHook == null && cityAdapter == null) {
            return Optional.empty();
        }
        String worldName = location.getWorld() == null ? "unknown" : location.getWorld().getName();
        String plotId = accessHook == null ? null : accessHook.getPlotIdAt(location).orElse(null);
        boolean inPlot = plotId != null
                || (accessHook != null && accessHook.isInCityPlot(location))
                || (cityAdapter != null && cityAdapter.isInsideCityPlot(location));
        String cityId = cityAdapter == null ? null : cityAdapter.getCityId(location).orElse(null);
        boolean inCity = cityAdapter != null && cityAdapter.isInsideCityRegion(location);
        RegionType type = inPlot ? RegionType.PLOT : (inCity ? RegionType.CITY : RegionType.WILDERNESS);
        String regionId;
        if (plotId != null) {
            regionId = plotId;
        } else if (cityId != null) {
            regionId = cityId;
        } else {
            regionId = type.name().toLowerCase(Locale.ROOT) + ":" + worldName;
        }
        return Optional.of(new RegionInfo(regionId, type, cityId, plotId));
    }

    private static CityAccessHook resolveCityAccessHook() {
        RegisteredServiceProvider<CityAccessHook> provider =
                Bukkit.getServicesManager().getRegistration(CityAccessHook.class);
        return provider == null ? null : provider.getProvider();
    }

    private static CityAdapter resolveCityAdapter() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        return provider.getProvider().services().get(CityAdapter.class);
    }
}
