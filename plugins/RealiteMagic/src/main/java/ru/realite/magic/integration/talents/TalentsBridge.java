package ru.realite.magic.integration.talents;

import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;
import ru.realite.core.api.talents.TalentDefinition;

public interface TalentsBridge {

    boolean isAvailable();

    Set<String> activeTalents(Player player);

    default Optional<TalentDefinition> findTalent(String talentId) {
        return Optional.empty();
    }
}
