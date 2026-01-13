package ru.realite.classes.ui;

import java.util.Optional;
import org.bukkit.entity.Player;
import ru.realite.classes.RealiteClassesPlugin;
import ru.realite.classes.model.ClassLevelXpData;
import ru.realite.core.api.ui.UiProvider;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiSnapshot;

public final class ClassLevelXpUiProvider implements UiProvider {

    public static final UiProviderId ID = new UiProviderId("classes.level_xp");

    private final RealiteClassesPlugin plugin;

    public ClassLevelXpUiProvider(RealiteClassesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public UiProviderId id() {
        return ID;
    }

    @Override
    public Optional<UiSnapshot> snapshot(Player player) {
        if (!isAvailable(player)) {
            return Optional.empty();
        }
        var service = plugin.getClassLevelXpService();
        if (service == null) {
            return Optional.empty();
        }
        Optional<ClassLevelXpData> data = service.getLevelXp(player);
        return data.map(levelXp -> new UiSnapshot(levelXp.currentXp(), levelXp.maxXpForLevel()));
    }

    @Override
    public boolean isAvailable(Player player) {
        return plugin.getClassService() != null && plugin.getClassService().getProfile(player) != null;
    }
}
