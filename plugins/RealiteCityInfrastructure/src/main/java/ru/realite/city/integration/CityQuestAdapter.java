package ru.realite.city.integration;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import ru.realite.city.model.CityArea;
import ru.realite.city.model.Plot;
import ru.realite.city.service.PlotService;
import ru.realite.city.storage.CityAreaRepository;
import ru.realite.city.storage.PlotMemberRepository;
import ru.realite.city.storage.PlotRepository;
import ru.realite.core.api.quests.CityAdapter;

public final class CityQuestAdapter implements CityAdapter {

    private final CityAreaRepository cityAreaRepository;
    private final PlotService plotService;
    private final PlotRepository plotRepository;
    private final PlotMemberRepository plotMemberRepository;

    public CityQuestAdapter(CityAreaRepository cityAreaRepository,
                            PlotService plotService,
                            PlotRepository plotRepository,
                            PlotMemberRepository plotMemberRepository) {
        this.cityAreaRepository = cityAreaRepository;
        this.plotService = plotService;
        this.plotRepository = plotRepository;
        this.plotMemberRepository = plotMemberRepository;
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

    @Override
    public boolean hasPlotResidency(Player player,
                                    boolean countOwner,
                                    boolean countMember,
                                    boolean mustBeInsideCity) {
        if (player == null) {
            return false;
        }
        if (!countOwner && !countMember) {
            return false;
        }
        if (countOwner) {
            for (Plot plot : plotRepository.findByOwner(player.getUniqueId())) {
                if (!isPlotInsideCity(plot, mustBeInsideCity)) {
                    continue;
                }
                return true;
            }
        }
        if (countMember) {
            for (Plot plot : plotRepository.findAll()) {
                if (!plotMemberRepository.isMember(plot.id(), player.getUniqueId())) {
                    continue;
                }
                if (!isPlotInsideCity(plot, mustBeInsideCity)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private boolean isPlotInsideCity(Plot plot, boolean mustBeInsideCity) {
        if (!mustBeInsideCity) {
            return true;
        }
        if (plot == null) {
            return false;
        }
        World world = Bukkit.getWorld(plot.world());
        if (world == null) {
            return false;
        }
        Location location = new Location(world, plot.x1(), plot.y1(), plot.z1());
        return cityAreaRepository.findContaining(location).isPresent();
    }
}
