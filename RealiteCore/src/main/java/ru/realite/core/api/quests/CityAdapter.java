package ru.realite.core.api.quests;

import java.util.Optional;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Адаптер для проверки условий, связанных с городами.
 */
public interface CityAdapter {

    /**
     * Проверяет, находится ли локация внутри городской области (CityRegion).
     */
    default boolean isInsideCityRegion(Location location) {
        return false;
    }

    /**
     * Проверяет, находится ли локация внутри городского участка (Plot).
     */
    default boolean isInsideCityPlot(Location location) {
        return false;
    }

    /**
     * Возвращает идентификатор города по локации.
     */
    default Optional<String> getCityId(Location location) {
        return Optional.empty();
    }

    /**
     * Проверяет, является ли игрок мэром города.
     */
    default boolean isMayor(Player player, String cityId) {
        return false;
    }

    /**
     * Проверяет, владеет ли игрок участком или является участником участка.
     */
    default boolean hasPlotResidency(Player player,
                                     boolean countOwner,
                                     boolean countMember,
                                     boolean mustBeInsideCity) {
        return false;
    }
}
