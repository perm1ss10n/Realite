package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.model.EvolutionRequirement;
import ru.realite.classes.model.PlayerProfile;

public final class EvolutionRequirementAdapter {

    private final ClassService classService;

    public EvolutionRequirementAdapter(ClassService classService) {
        this.classService = classService;
    }

    public boolean isMet(Player player, EvolutionRequirement requirement) {
        if (requirement == null || requirement.isEmpty()) {
            return true;
        }
        PlayerProfile profile = classService.getProfile(player);
        if (profile == null) {
            return false;
        }
        for (var required : requirement.masteredClasses()) {
            if (!profile.hasMastered(required)) {
                return false;
            }
        }
        return true;
    }
}
