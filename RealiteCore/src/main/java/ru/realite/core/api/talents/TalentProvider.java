package ru.realite.core.api.talents;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;

public interface TalentProvider {

    default Set<String> activeTalents(Player player) {
        return Set.of();
    }

    default Optional<TalentDefinition> findTalent(String talentId) {
        return Optional.empty();
    }

    default Collection<TalentDefinition> talents() {
        return Set.of();
    }
}
