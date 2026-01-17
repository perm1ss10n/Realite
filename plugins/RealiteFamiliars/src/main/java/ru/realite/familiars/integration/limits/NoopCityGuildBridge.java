package ru.realite.familiars.integration.limits;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import ru.realite.familiars.integration.BridgeWarning;

public final class NoopCityGuildBridge implements CityGuildBridge {

    private final BridgeWarning warning;

    public NoopCityGuildBridge(Logger logger) {
        this.warning = new BridgeWarning(Objects.requireNonNull(logger, "logger"),
                "[Familiars] City/Guild bridge not present.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public OptionalInt maxActive(Player player) {
        warning.warnOnce();
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt maxSummoned(Player player) {
        warning.warnOnce();
        return OptionalInt.empty();
    }
}
