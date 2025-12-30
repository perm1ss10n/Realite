package ru.realite.guilds.listener;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.service.GuildService;

public final class GuildAccessProtectionListener implements Listener {

    private static final long MESSAGE_COOLDOWN_MS = 1500L;

    private final GuildService service;
    private final GuildMessages messages;
    private final FileConfiguration config;
    private final Set<Material> protectedBlocks = new HashSet<>();
    private final ConcurrentMap<UUID, Long> lastMessageAt = new ConcurrentHashMap<>();

    public GuildAccessProtectionListener(GuildService service, GuildMessages messages, FileConfiguration config) {
        this.service = service;
        this.messages = messages;
        this.config = config;
        loadProtectedBlocks();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!config.getBoolean("access.protect.enabled", true)) {
            return;
        }
        if (!config.getBoolean("claim.enabled", true)) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!isProtected(block.getType())) {
            return;
        }
        Guild guild = service.findGuildByClaim(block.getLocation());
        if (guild == null) {
            return;
        }
        Player player = event.getPlayer();
        if (service.canAccessClaim(player, guild)) {
            return;
        }
        event.setCancelled(true);
        sendDenied(player);
    }

    private void loadProtectedBlocks() {
        for (String raw : config.getStringList("access.protect.blocks")) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String key = raw.trim().toUpperCase(Locale.ROOT);
            try {
                protectedBlocks.add(Material.valueOf(key));
            } catch (IllegalArgumentException ex) {
                // ignore invalid material
            }
        }
    }

    private boolean isProtected(Material type) {
        if (type == null) {
            return false;
        }
        if (protectedBlocks.contains(type)) {
            return true;
        }
        if (config.getBoolean("access.protect.doors", true)) {
            return Tag.DOORS.isTagged(type) || Tag.TRAPDOORS.isTagged(type);
        }
        return false;
    }

    private void sendDenied(Player player) {
        long now = System.currentTimeMillis();
        long last = lastMessageAt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < MESSAGE_COOLDOWN_MS) {
            return;
        }
        lastMessageAt.put(player.getUniqueId(), now);
        messages.send(player, "access.denied");
    }
}
