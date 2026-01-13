package ru.realite.guilds.menu;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.guilds.i18n.GuildMessages;

public final class GuildChatInputService implements Listener {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final JavaPlugin plugin;
    private final GuildMessages messages;
    private final Map<UUID, PendingInput> pendingInputs = new ConcurrentHashMap<>();
    private final long timeoutTicks;

    public GuildChatInputService(JavaPlugin plugin, GuildMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
        this.timeoutTicks = 20L * 60L;
    }

    public void requestInput(Player player, InputType type) {
        clearInput(player);
        player.closeInventory();
        UUID uuid = player.getUniqueId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> pendingInputs.remove(uuid), timeoutTicks);
        pendingInputs.put(uuid, new PendingInput(type, task));
        messages.send(player, "ui.guild.input.cancel_hint");
        messages.send(player, type.promptKey());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = pendingInputs.get(player.getUniqueId());
        if (pending == null) {
            return;
        }
        event.setCancelled(true);
        String message = PLAIN.serialize(event.message()).trim();
        if (message.isEmpty()) {
            return;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        if ("cancel".equals(normalized) || "отмена".equals(normalized)) {
            clearInput(player);
            return;
        }
        clearInput(player);
        String command = pending.type.command(message);
        Bukkit.getScheduler().runTask(plugin, () -> player.performCommand(command));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearInput(event.getPlayer());
    }

    private void clearInput(Player player) {
        UUID uuid = player.getUniqueId();
        PendingInput pending = pendingInputs.remove(uuid);
        if (pending != null && pending.timeoutTask() != null) {
            pending.timeoutTask().cancel();
        }
    }

    private record PendingInput(InputType type, BukkitTask timeoutTask) {
    }

    public enum InputType {
        CREATE("ui.guild.input.create", "g create "),
        INVITE("ui.guild.input.invite", "g invite "),
        SETRANK("ui.guild.input.setrank", "g setrank "),
        TP("ui.guild.input.tp", "g tp "),
        UPGRADE_BUY("ui.guild.input.upgrade_buy", "g upgrade buy ");

        private final String promptKey;
        private final String commandPrefix;

        InputType(String promptKey, String commandPrefix) {
            this.promptKey = promptKey;
            this.commandPrefix = commandPrefix;
        }

        public String promptKey() {
            return promptKey;
        }

        public String command(String input) {
            return commandPrefix + input.trim();
        }
    }
}
