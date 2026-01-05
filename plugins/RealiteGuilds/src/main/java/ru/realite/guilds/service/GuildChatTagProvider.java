package ru.realite.guilds.service;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ru.realite.core.api.guilds.GuildTagProvider;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildChatTagProvider implements GuildTagProvider {

    private final GuildRepository repository;
    private final GuildMessages messages;
    private final GuildRankService rankService;

    public GuildChatTagProvider(GuildRepository repository, GuildMessages messages, GuildRankService rankService) {
        this.repository = repository;
        this.messages = messages;
        this.rankService = rankService;
    }

    @Override
    public Optional<Component> getTag(Player player) {
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null || member.tag() == null || member.tag().isBlank()) {
            return Optional.empty();
        }

        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            return Optional.empty();
        }

        // Минимальный нейтральный тег. Раскраску/декор пусть делает RealiteChat.
        return Optional.of(Component.text("[" + guild.tag() + "] "));
    }

    @Override
    public Optional<Component> getHover(Player player) {
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null || member.tag() == null || member.tag().isBlank()) {
            return Optional.empty();
        }

        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            return Optional.empty();
        }

        int members = repository.countMembersByTag(guild.tag());
        String rankName = resolveRankName(member);

        // Если хочешь локализовать — можно сделать через messages keys,
        // но это рабочий минимум без новых ключей.
        Component hover = Component.text(guild.name())
                .append(Component.text("\n"))
                .append(Component.text("Tag: " + guild.tag()))
                .append(Component.text("\n"))
                .append(Component.text("Rank: " + rankName))
                .append(Component.text("\n"))
                .append(Component.text("Members: " + members));

        return Optional.of(hover);
    }

    private String resolveRankName(GuildMember member) {
        GuildRankService.GuildRank rank = rankService.getRank(member.role());
        if (rank == null) {
            return member.role();
        }
        String raw = messages.raw(rank.displayNameKey());
        if (raw == null || raw.isBlank()) {
            return rank.id();
        }
        return raw;
    }
}
