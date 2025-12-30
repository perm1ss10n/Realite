package ru.realite.guilds.service;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildService {

    private final FileConfiguration config;
    private final GuildRepository repository;
    private final GuildMessages messages;

    public GuildService(FileConfiguration config, GuildRepository repository, GuildMessages messages) {
        this.config = config;
        this.repository = repository;
        this.messages = messages;
    }

    public void create(Player player, String tagRaw, String nameRaw) {
        String tag = tagRaw == null ? "" : tagRaw.trim().toUpperCase(Locale.ROOT);
        if (tag.isBlank()) {
            messages.send(player, "usage.create");
            return;
        }
        int min = config.getInt("guild.tag.min", 2);
        int max = config.getInt("guild.tag.max", 4);
        if (tag.length() < min || tag.length() > max) {
            messages.send(player, "error.guild.tag-length",
                    "min", String.valueOf(min),
                    "max", String.valueOf(max));
            return;
        }
        String regex = config.getString("guild.tag.regex", "^[A-Z0-9]+$");
        if (regex != null && !Pattern.matches(regex, tag)) {
            messages.send(player, "error.guild.tag-regex");
            return;
        }
        if (repository.getGuild(tag) != null) {
            messages.send(player, "error.guild.tag-exists");
            return;
        }
        String name = nameRaw == null ? "" : nameRaw.trim();
        if (name.isBlank()) {
            messages.send(player, "error.guild.name-empty");
            return;
        }
        int nameMax = config.getInt("guild.name.max", 24);
        if (name.length() > nameMax) {
            messages.send(player, "error.guild.name-length", "max", String.valueOf(nameMax));
            return;
        }
        UUID ownerId = player.getUniqueId();
        Guild guild = new Guild(tag, name, ownerId);
        repository.saveGuild(guild);
        repository.saveMember(new GuildMember(ownerId, tag, "owner"));
        messages.send(player, "success.create", "tag", tag, "name", name);
    }

    public void info(Player player, String tagRaw) {
        Guild guild;
        if (tagRaw == null || tagRaw.isBlank()) {
            GuildMember member = repository.getMember(player.getUniqueId());
            if (member == null) {
                messages.send(player, "error.guild.no-member");
                return;
            }
            guild = repository.getGuild(member.tag());
        } else {
            guild = repository.getGuild(tagRaw);
        }
        if (guild == null) {
            messages.send(player, "error.guild.not-found");
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(guild.owner());
        String ownerName = owner.getName() == null ? guild.owner().toString() : owner.getName();
        messages.send(player, "info.format",
                "tag", guild.tag(),
                "name", guild.name(),
                "owner", ownerName);
    }

    public void disband(Player player) {
        if (!config.getBoolean("features.disband.enabled", false)) {
            messages.send(player, "error.guild.disband-disabled");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no-member");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(player, "error.guild.not-found");
            return;
        }
        if (!guild.owner().equals(player.getUniqueId())) {
            messages.send(player, "error.guild.disband-not-owner");
            return;
        }
        repository.removeGuild(guild.tag());
        repository.removeMembersByTag(guild.tag());
        messages.send(player, "success.disband", "tag", guild.tag());
    }

    public int getCreateCost() {
        return config.getInt("guild.create.cost", 0);
    }
}
