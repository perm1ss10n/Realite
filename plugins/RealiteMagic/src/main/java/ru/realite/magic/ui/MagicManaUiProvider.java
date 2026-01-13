package ru.realite.magic.ui;

import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiProvider;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiSnapshot;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;

public final class MagicManaUiProvider implements UiProvider {

    public static final UiProviderId ID = new UiProviderId("magic.mana");
    private static final String PERMISSION_USE = "realite.magic.use";

    private final MagicService magicService;
    private final PlayerSpellService playerSpellService;

    public MagicManaUiProvider(MagicService magicService, PlayerSpellService playerSpellService) {
        this.magicService = magicService;
        this.playerSpellService = playerSpellService;
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
        int current = (int) Math.round(magicService.getMana(player));
        int max = (int) Math.round(magicService.getMaxMana(player));
        return Optional.of(new UiSnapshot(current, max));
    }

    @Override
    public boolean isAvailable(Player player) {
        if (!player.hasPermission(PERMISSION_USE)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        return !playerSpellService.listLearned(playerId).isEmpty();
    }
}
