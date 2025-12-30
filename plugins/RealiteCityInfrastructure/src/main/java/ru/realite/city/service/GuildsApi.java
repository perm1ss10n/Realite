package ru.realite.city.service;

import java.util.Optional;
import java.util.UUID;

public interface GuildsApi {
    Optional<UUID> findGuildIdByTag(String tag);

    boolean isMember(UUID guildId, UUID playerId);
}
