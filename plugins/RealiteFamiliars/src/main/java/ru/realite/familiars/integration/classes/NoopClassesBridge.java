package ru.realite.familiars.integration.classes;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.realite.familiars.integration.BridgeWarning;

public final class NoopClassesBridge implements ClassesBridge {

    private final BridgeWarning warning;

    public NoopClassesBridge(Logger logger) {
        this.warning = new BridgeWarning(Objects.requireNonNull(logger, "logger"),
                "[Familiars] ClassesBridge not present.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public @Nullable ClassTierInfo getActiveClassInfo(Player player) {
        warning.warnOnce();
        return null;
    }
}
