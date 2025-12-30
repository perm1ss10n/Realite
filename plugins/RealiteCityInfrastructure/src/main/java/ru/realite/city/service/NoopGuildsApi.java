package ru.realite.city.service;

import java.util.Optional;
import java.util.UUID;

public final class NoopGuildsApi implements GuildsApi {
    @Override
    public Optional<UUID> findGuildIdByTag(String tag) {
        return Optional.empty();
    }

    @Override
    public boolean isMember(UUID guildId, UUID playerId) {
        return false;
    }
}
