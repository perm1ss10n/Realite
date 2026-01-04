package ru.realite.guilds.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildProgressionService {
    private static final int DEFAULT_LEVEL = 1;

    private final JavaPlugin plugin;
    private final GuildRepository repository;
    private final GuildMessages messages;
    private final List<ProgressionLevel> levels;

    public GuildProgressionService(JavaPlugin plugin, FileConfiguration config, GuildRepository repository,
                                   GuildMessages messages) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.levels = loadLevels(config);
    }

    public void addXp(CommandSender sender, String tag, long amount, String reason) {
        if (amount <= 0L) {
            send(sender, "admin.addxp.usage");
            return;
        }
        Guild guild = repository.getGuild(tag);
        if (guild == null) {
            send(sender, "guild.not_found");
            return;
        }
        long oldXp = Math.max(0L, guild.xp());
        long newXp = Math.max(0L, oldXp + amount);
        int oldLevel = resolveLevel(oldXp);
        int newLevel = resolveLevel(newXp);

        Guild updated = new Guild(guild.tag(), guild.name(), guild.owner(), guild.home(), guild.claim(),
                newLevel, newXp, guild.upgradeLevels());
        repository.saveGuild(updated);

        String safeReason = reason == null || reason.isBlank() ? "—" : reason;
        String amountText = String.valueOf(amount);
        String totalText = String.valueOf(newXp);
        String levelText = String.valueOf(newLevel);

        List<Player> targets = resolveOnlineMembers(updated.tag());
        for (Player player : targets) {
            messages.send(player, "xp.added",
                    "tag", updated.tag(),
                    "amount", amountText,
                    "total", totalText,
                    "level", levelText,
                    "reason", safeReason);
        }
        GuildMember senderMember = null;
        if (sender instanceof Player player) {
            senderMember = repository.getMember(player.getUniqueId());
        }
        if (senderMember == null || !updated.tag().equalsIgnoreCase(senderMember.tag())) {
            send(sender, "xp.added",
                    "tag", updated.tag(),
                    "amount", amountText,
                    "total", totalText,
                    "level", levelText,
                    "reason", safeReason);
        }
        if (newLevel > oldLevel) {
            for (Player player : targets) {
                messages.send(player, "xp.levelup",
                        "tag", updated.tag(),
                        "level", levelText);
            }
        }
    }

    private int resolveLevel(long xp) {
        int level = DEFAULT_LEVEL;
        for (ProgressionLevel entry : levels) {
            if (xp >= entry.xp()) {
                level = entry.level();
            }
        }
        return level;
    }

    private List<Player> resolveOnlineMembers(String tag) {
        return repository.getMembers().stream()
                .filter(member -> member.tag().equalsIgnoreCase(tag))
                .map(GuildMember::uuid)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<ProgressionLevel> loadLevels(FileConfiguration config) {
        List<Map<?, ?>> entries = config == null ? List.of() : config.getMapList("progression.levels");
        List<ProgressionLevel> parsed = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            if (entry == null) {
                continue;
            }
            int level = parseInt(entry.get("level"), DEFAULT_LEVEL);
            long xp = parseLong(entry.get("xp"), 0L);
            if (level < 1) {
                level = DEFAULT_LEVEL;
            }
            if (xp < 0L) {
                xp = 0L;
            }
            parsed.add(new ProgressionLevel(level, xp));
        }
        if (parsed.isEmpty()) {
            plugin.getLogger().warning("[Guilds] progression.levels is empty. Using default level 1.");
            parsed.add(new ProgressionLevel(DEFAULT_LEVEL, 0L));
        }
        parsed.sort(Comparator.comparingLong(ProgressionLevel::xp)
                .thenComparingInt(ProgressionLevel::level));
        return parsed;
    }

    private void send(CommandSender sender, String key, String... placeholders) {
        if (sender instanceof Player player) {
            messages.send(player, key, placeholders);
        } else {
            sender.sendMessage(messages.msg(key, placeholders));
        }
    }

    private int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private long parseLong(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private record ProgressionLevel(int level, long xp) {
    }
}
