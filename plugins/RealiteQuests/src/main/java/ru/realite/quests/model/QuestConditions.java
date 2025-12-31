package ru.realite.quests.model;

import java.util.List;
import java.util.Objects;

public final class QuestConditions {

    public static final QuestConditions EMPTY = new QuestConditions(false, false, false, false, List.of());

    private final boolean requireCity;
    private final boolean requireOutsideCity;
    private final boolean requireGuild;
    private final boolean requirePlot;
    private final List<String> allowedCityIds;

    public QuestConditions(boolean requireCity,
                           boolean requireOutsideCity,
                           boolean requireGuild,
                           boolean requirePlot,
                           List<String> allowedCityIds) {
        this.requireCity = requireCity;
        this.requireOutsideCity = requireOutsideCity;
        this.requireGuild = requireGuild;
        this.requirePlot = requirePlot;
        this.allowedCityIds = List.copyOf(Objects.requireNonNullElse(allowedCityIds, List.of()));
    }

    public boolean requireCity() {
        return requireCity;
    }

    public boolean requireOutsideCity() {
        return requireOutsideCity;
    }

    public boolean requireGuild() {
        return requireGuild;
    }

    public boolean requirePlot() {
        return requirePlot;
    }

    public List<String> allowedCityIds() {
        return allowedCityIds;
    }

    public boolean isEmpty() {
        return !requireCity
                && !requireOutsideCity
                && !requireGuild
                && !requirePlot
                && (allowedCityIds == null || allowedCityIds.isEmpty());
    }
}
