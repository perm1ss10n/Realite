package ru.realite.city.integration;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import ru.realite.city.model.CityArea;
import ru.realite.city.service.PlotService;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.core.api.quests.CityAdapter;

public final class CityQuestAdapter implements CityAdapter {

    private final CityAreaRepository cityAreaRepository;
    private final PlotService plotService;

    public CityQuestAdapter(CityAreaRepository cityAreaRepository, PlotService plotService) {
        this.cityAreaRepository = cityAreaRepository;
        this.plotService = plotService;
    }

    @Override
    public boolean isInsideCityRegion(Location location) {
        if (location == null) {
            return false;
        }
        return cityAreaRepository.findContaining(location).isPresent();
    }

    @Override
    public boolean isInsideCityPlot(Location location) {
        if (location == null) {
            return false;
        }
        return plotService.findContaining(location).isPresent();
    }

    @Override
    public Optional<String> getCityId(Location location) {
        if (location == null) {
            return Optional.empty();
        }
        return cityAreaRepository.findContaining(location).map(CityArea::id);
    }

    @Override
    public boolean isMayor(Player player, String cityId) {
        return false;
    }
}
