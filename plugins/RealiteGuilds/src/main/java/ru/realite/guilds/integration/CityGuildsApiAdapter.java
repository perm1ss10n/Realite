package ru.realite.guilds.integration;

import ru.realite.city.service.GuildsApi;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class CityGuildsApiAdapter implements GuildsApi {

    private final GuildRepository repository;

    public CityGuildsApiAdapter(GuildRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<UUID> findGuildIdByTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return Optional.empty();
        }
        if (repository.getGuild(tag) == null) {
            return Optional.empty();
        }
        return Optional.of(guildIdForTag(tag));
    }

    @Override
    public boolean isMember(UUID guildId, UUID playerId) {
        if (guildId == null || playerId == null) {
            return false;
        }
        GuildMember member = repository.getMember(playerId);
        if (member == null) {
            return false;
        }
        String tag = member.tag();
        if (tag == null || tag.isBlank()) {
            return false;
        }
        return guildId.equals(guildIdForTag(tag));
    }

    private UUID guildIdForTag(String tag) {
        String normalized = tag == null ? "" : tag.trim().toUpperCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(("guild:" + normalized).getBytes(StandardCharsets.UTF_8));
    }
}
