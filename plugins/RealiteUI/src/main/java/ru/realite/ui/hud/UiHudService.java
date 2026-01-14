package ru.realite.ui.hud;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.ui.UiProvider;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiRegistry;
import ru.realite.core.api.ui.UiSlot;
import ru.realite.core.api.ui.UiSnapshot;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.settings.UiSettings;
import ru.realite.ui.settings.UiSettingsStore;
import ru.realite.ui.util.UiText;

public final class UiHudService implements Listener {

    private final JavaPlugin plugin;
    private final MiniMessageMessages messages;
    private final UiRegistry registry;
    private final UiSettingsStore settingsStore;
    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public UiHudService(JavaPlugin plugin,
                        MiniMessageMessages messages,
                        UiRegistry registry,
                        UiSettingsStore settingsStore) {
        this.plugin = plugin;
        this.messages = messages;
        this.registry = registry;
        this.settingsStore = settingsStore;
    }

    public void refresh(Player player) {
        refresh(player, UiSlot.BOSSBAR);
        refresh(player, UiSlot.ACTION_BAR);
    }

    public void refreshOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            refresh(player);
        }
    }

    public void refresh(Player player, UiSlot slot) {
        UiSettings settings = settingsStore.get(player.getUniqueId());
        Optional<UiProviderId> providerId = settings.provider(slot);
        if (providerId.isEmpty()) {
            clearSlot(player, slot);
            return;
        }
        updateSlot(player, slot, providerId.get());
    }

    public void refreshIfMatches(Player player, UiProviderId providerId) {
        UiSettings settings = settingsStore.get(player.getUniqueId());
        for (UiSlot slot : UiSlot.values()) {
            if (settings.provider(slot).map(providerId::equals).orElse(false)) {
                updateSlot(player, slot, providerId);
            }
        }
    }

    private void updateSlot(Player player, UiSlot slot, UiProviderId providerId) {
        UiProvider provider = registry.provider(providerId).orElse(null);
        if (provider == null) {
            clearSlot(player, slot);
            return;
        }
        Optional<UiSnapshot> snapshot = provider.snapshot(player);
        if (snapshot.isEmpty()) {
            clearSlot(player, slot);
            return;
        }
        UiSnapshot data = snapshot.get();
        String providerName = UiText.providerName(messages, providerId);
        switch (slot) {
            case BOSSBAR -> showBossBar(player, providerName, data);
            case ACTION_BAR -> showActionBar(player, providerName, data);
        }
    }

    private void showBossBar(Player player, String providerName, UiSnapshot snapshot) {
        double progress = snapshot.max() <= 0 ? 0.0 : (double) snapshot.current() / snapshot.max();
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(),
                id -> BossBar.bossBar(Component.empty(), 0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS));
        Component title = messages.get("ui.hud.bossbar", Map.of(
                "provider", providerName,
                "current", String.valueOf(snapshot.current()),
                "max", String.valueOf(snapshot.max())));
        bar.name(title);
        bar.progress((float) Math.max(0.0, Math.min(1.0, progress)));
        player.showBossBar(bar);
    }

    private void showActionBar(Player player, String providerName, UiSnapshot snapshot) {
        Component message = messages.get("ui.hud.actionbar", Map.of(
                "provider", providerName,
                "current", String.valueOf(snapshot.current()),
                "max", String.valueOf(snapshot.max())));
        player.sendActionBar(message);
    }

    private void clearSlot(Player player, UiSlot slot) {
        switch (slot) {
            case BOSSBAR -> clearBossBar(player);
            case ACTION_BAR -> player.sendActionBar(Component.empty());
        }
    }

    private void clearBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTask(plugin, () -> refresh(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearBossBar(event.getPlayer());
    }
}
