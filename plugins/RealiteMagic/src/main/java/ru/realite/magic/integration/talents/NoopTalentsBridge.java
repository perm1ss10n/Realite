package ru.realite.magic.integration.talents;

import java.util.Optional;
import java.util.Set;
import org.bukkit.entity.Player;
import ru.realite.core.api.talents.TalentDefinition;

public final class NoopTalentsBridge implements TalentsBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Set<String> activeTalents(Player player) {
        return Set.of();
    }

    @Override
    public Optional<TalentDefinition> findTalent(String talentId) {
        return Optional.empty();
    }
}
