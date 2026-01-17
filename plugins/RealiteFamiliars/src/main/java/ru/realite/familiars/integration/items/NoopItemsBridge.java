package ru.realite.familiars.integration.items;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import org.bukkit.inventory.ItemStack;
import ru.realite.familiars.integration.BridgeWarning;

public final class NoopItemsBridge implements ItemsBridge {

    private final BridgeWarning warning;

    public NoopItemsBridge(Logger logger) {
        this.warning = new BridgeWarning(Objects.requireNonNull(logger, "logger"),
                "[Familiars] ItemsBridge not present.");
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<String> getItemId(ItemStack stack) {
        warning.warnOnce();
        return Optional.empty();
    }

    @Override
    public boolean isItem(ItemStack stack, String itemId) {
        warning.warnOnce();
        return false;
    }

    @Override
    public Optional<Integer> readInt(ItemStack stack, String key) {
        warning.warnOnce();
        return Optional.empty();
    }

    @Override
    public Optional<String> readString(ItemStack stack, String key) {
        warning.warnOnce();
        return Optional.empty();
    }
}
