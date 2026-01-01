package ru.realite.classes.integration;

import org.bukkit.entity.Player;
import ru.realite.classes.service.ProgressionService;
import ru.realite.core.api.classes.ClassXpService;

public final class ClassXpServiceAdapter implements ClassXpService {

    private final ProgressionService progressionService;

    public ClassXpServiceAdapter(ProgressionService progressionService) {
        this.progressionService = progressionService;
    }

    @Override
    public void addXp(Player player, long amount) {
        if (progressionService == null) {
            return;
        }
        progressionService.addXp(player, amount);
    }
}
