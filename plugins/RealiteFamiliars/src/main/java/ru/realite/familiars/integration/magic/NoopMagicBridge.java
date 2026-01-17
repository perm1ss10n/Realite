package ru.realite.familiars.integration.magic;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import ru.realite.familiars.integration.BridgeWarning;
import ru.realite.familiars.model.FamiliarInstance;

public final class NoopMagicBridge implements MagicBridge {

    private final BridgeWarning warning;

    public NoopMagicBridge(Logger logger) {
        this.warning = new BridgeWarning(Objects.requireNonNull(logger, "logger"),
                "[Familiars] MagicBridge not present.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void refresh(Player player, FamiliarInstance instance) {
        warning.warnOnce();
    }

    @Override
    public void clear(Player player, FamiliarInstance instance) {
        warning.warnOnce();
    }
}
