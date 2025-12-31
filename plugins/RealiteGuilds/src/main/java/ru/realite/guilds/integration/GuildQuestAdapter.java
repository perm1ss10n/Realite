package ru.realite.guilds.integration;

import java.util.Optional;
import org.bukkit.entity.Player;
import ru.realite.core.api.quests.GuildAdapter;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildQuestAdapter implements GuildAdapter {

    private final GuildRepository repository;
    private final GuildRankService rankService;

    public GuildQuestAdapter(GuildRepository repository, GuildRankService rankService) {
        this.repository = repository;
        this.rankService = rankService;
    }

    @Override
    public boolean isInGuild(Player player) {
        return player != null && repository.getMember(player.getUniqueId()) != null;
    }

    @Override
    public Optional<String> getGuildTag(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        return member == null ? Optional.empty() : Optional.of(member.tag());
    }

    @Override
    public Optional<String> getGuildRankId(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null || rankService.getRank(member.role()) == null) {
            return Optional.empty();
        }
        return Optional.of(member.role());
    }
}
