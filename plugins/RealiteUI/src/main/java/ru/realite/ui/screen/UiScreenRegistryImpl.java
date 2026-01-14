package ru.realite.ui.screen;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;

public final class UiScreenRegistryImpl implements UiScreenRegistry {

    private final Map<String, UiScreen> screens = new ConcurrentHashMap<>();

    @Override
    public void register(UiScreen screen) {
        if (screen == null || screen.id() == null) {
            return;
        }
        screens.put(normalize(screen.id()), screen);
    }

    @Override
    public Optional<UiScreen> screen(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(screens.get(normalize(id)));
    }

    @Override
    public boolean open(Player player, String target) {
        if (player == null || target == null || target.isBlank()) {
            return false;
        }
        String trimmed = target.trim();
        String[] parts = trimmed.split(":", 2);
        String id = parts[0];
        String payload = parts.length > 1 ? parts[1] : null;
        UiScreen screen = screens.get(normalize(id));
        if (screen == null) {
            return false;
        }
        screen.open(player, payload);
        return true;
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
