package ru.realite.ui.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiRegistry;
import ru.realite.core.api.ui.UiSlot;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.hud.UiHudService;
import ru.realite.ui.settings.UiSettings;
import ru.realite.ui.settings.UiSettingsStore;
import ru.realite.ui.util.UiText;

public final class UiSettingsMenu extends BaseMenu {

    private static final int SIZE = 27;

    private final MiniMessageMessages messages;
    private final UiSettingsStore settingsStore;
    private final UiHudService hudService;
    private final UiRegistry registry;

    public UiSettingsMenu(MiniMessageMessages messages,
                          UiSettingsStore settingsStore,
                          UiHudService hudService,
                          UiRegistry registry) {
        super(SIZE, messages.get("ui.settings.title"));
        this.messages = messages;
        this.settingsStore = settingsStore;
        this.hudService = hudService;
        this.registry = registry;
    }

    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        UiSettings settings = settingsStore.get(player.getUniqueId());
        setButton(11, Material.DRAGON_BREATH, messages.get("ui.settings.bossbar"),
                List.of(currentProviderLore(settings, UiSlot.BOSSBAR), messages.get("ui.settings.choose")),
                p -> new UiProviderSelectMenu(messages, settingsStore, hudService, registry, UiSlot.BOSSBAR)
                        .open(p));
        setButton(15, Material.PAPER, messages.get("ui.settings.actionbar"),
                List.of(currentProviderLore(settings, UiSlot.ACTION_BAR), messages.get("ui.settings.choose")),
                p -> new UiProviderSelectMenu(messages, settingsStore, hudService, registry, UiSlot.ACTION_BAR)
                        .open(p));
        setButton(22, Material.OAK_DOOR, messages.get("ui.common.close"), null, Player::closeInventory);
    }

    private Component currentProviderLore(UiSettings settings, UiSlot slot) {
        String providerName = settings.provider(slot)
                .map(providerId -> UiText.providerName(messages, providerId))
                .orElseGet(() -> UiText.noneName(messages));
        return messages.get("ui.settings.current", java.util.Map.of("provider", providerName));
    }
}
