package ru.realite.magic.integration.guilds;

import java.util.Optional;
import java.util.UUID;

public final class NoopGuildBridge implements GuildBridge {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public Optional<String> guildId(UUID playerId) {
        return Optional.empty();
    }

    @Override
    public Optional<String> guildRank(UUID playerId) {
        return Optional.empty();
    }
}
