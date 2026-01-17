package ru.realite.familiars.integration.limits;

import java.util.OptionalInt;
import org.bukkit.entity.Player;

public interface CityGuildBridge {

    boolean isAvailable();

    OptionalInt maxActive(Player player);

    OptionalInt maxSummoned(Player player);
}
