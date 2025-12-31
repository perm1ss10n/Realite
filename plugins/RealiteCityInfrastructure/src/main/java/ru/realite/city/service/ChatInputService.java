package ru.realite.city.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.city.CityConfig;
import ru.realite.city.i18n.CityMessages;
import ru.realite.city.model.PlotOwnerType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputService {

    public enum Mode {
        ADD_TRUSTED,
        SET_OWNER_PLAYER,
        SET_OWNER_GUILD
    }

    private record PendingInput(Mode mode, String plotId, UUID token, BukkitTask task) {
    }

    private final JavaPlugin plugin;
    private final CityConfig config;
    private final CityMessages messages;
    private final PlotService plotService;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputService(JavaPlugin plugin,
                            CityConfig config,
                            CityMessages messages,
                            PlotService plotService) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.plotService = plotService;
    }

    public boolean hasPending(Player player) {
        if (player == null) {
            return false;
        }
        return pendingInputs.containsKey(player.getUniqueId());
    }

    public void start(Player player, Mode mode, String plotId) {
        if (player == null || mode == null || plotId == null || plotId.isBlank()) {
            return;
        }
        clear(player);
        UUID token = UUID.randomUUID();
        BukkitTask task = scheduleTimeout(player, token);
        pendingInputs.put(player.getUniqueId(), new PendingInput(mode, plotId, token, task));
        messages.send(player, promptKey(mode), "");
    }

    public boolean handleChat(Player player, String message) {
        if (player == null) {
            return false;
        }
        PendingInput pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        if (message == null || message.isBlank()) {
            messages.send(player, "chatinput.invalid", "");
            clear(player);
            return true;
        }
        switch (pending.mode()) {
            case ADD_TRUSTED -> handleAddTrusted(player, pending.plotId(), message);
            case SET_OWNER_PLAYER -> handleSetOwner(player, pending.plotId(), message, PlotOwnerType.PLAYER);
            case SET_OWNER_GUILD -> handleSetOwner(player, pending.plotId(), message, PlotOwnerType.GUILD);
        }
        clear(player);
        return true;
    }

    public boolean handleCancel(Player player) {
        if (player == null) {
            return false;
        }
        PendingInput pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) {
            return false;
        }
        clear(player);
        messages.send(player, "chatinput.cancelled", "");
        return true;
    }

    public void clear(Player player) {
        if (player == null) {
            return;
        }
        PendingInput pending = pendingInputs.remove(player.getUniqueId());
        if (pending == null) {
            return;
        }
        if (pending.task() != null) {
            pending.task().cancel();
        }
    }

    private void handleAddTrusted(Player player, String plotId, String targetName) {
        PlotService.AddTrustedOutcome outcome = plotService.addTrusted(player, plotId, targetName);
        switch (outcome.result()) {
            case PLOT_NOT_FOUND -> messages.send(player, "city.plot.not-found", "", Map.of("id", plotId));
            case NOT_OWNED -> messages.send(player, "city.plot.not-owned", "", Map.of("id", plotId));
            case NOT_OWNER -> messages.send(player, "city.plot.not-owner", "");
            case SELF -> messages.send(player, "city.plot.member.self", "");
            case LIMIT_REACHED -> messages.send(player, "plot.trusted.limit", "",
                    Map.of("limit", String.valueOf(config.trustedMax())));
            case INVALID_TARGET -> messages.send(player, "chatinput.invalid", "");
            case SUCCESS -> messages.send(player, "city.plot.member.added", "",
                    Map.ofEntries(
                            Map.entry("player", outcome.targetDisplay()),
                            Map.entry("id", plotId)));
        }
    }

    private void handleSetOwner(Player player, String plotId, String ownerRef, PlotOwnerType type) {
        PlotService.SetOwnerOutcome outcome = plotService.setOwner(plotId, type, ownerRef);
        switch (outcome.result()) {
            case PLOT_NOT_FOUND -> messages.send(player, "city.plot.not-found", "", Map.of("id", plotId));
            case NO_GUILDS -> messages.send(player, "plot.setowner.guilds_not_installed", "");
            case GUILD_NOT_FOUND -> messages.send(player, "plot.setowner.guild.not_found", "",
                    Map.of("tag", ownerRef));
            case INVALID_INPUT -> messages.send(player, "chatinput.invalid", "");
            case SUCCESS -> messages.send(player, "plot.setowner.success", "",
                    Map.ofEntries(
                            Map.entry("id", plotId),
                            Map.entry("owner", outcome.ownerDisplay())));
        }
    }

    private BukkitTask scheduleTimeout(Player player, UUID token) {
        int timeoutSeconds = config.chatInputTimeoutSeconds();
        if (timeoutSeconds <= 0) {
            return null;
        }
        long delay = timeoutSeconds * 20L;
        return Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingInput pending = pendingInputs.get(player.getUniqueId());
            if (pending == null || !pending.token().equals(token)) {
                return;
            }
            pendingInputs.remove(player.getUniqueId());
            messages.send(player, "chatinput.timeout", "");
        }, delay);
    }

    private String promptKey(Mode mode) {
        return switch (mode) {
            case ADD_TRUSTED -> "chatinput.prompt.add_trusted";
            case SET_OWNER_PLAYER -> "chatinput.prompt.set_owner_player";
            case SET_OWNER_GUILD -> "chatinput.prompt.set_owner_guild";
        };
    }
}
