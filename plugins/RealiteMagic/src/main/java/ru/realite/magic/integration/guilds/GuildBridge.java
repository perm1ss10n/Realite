package ru.realite.magic.integration.guilds;

import java.util.Optional;
import java.util.UUID;

public interface GuildBridge {
    boolean isAvailable();

    Optional<String> guildId(UUID playerId);

    Optional<String> guildRank(UUID playerId);
}
