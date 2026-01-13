package ru.realite.ui.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiRegistry;
import ru.realite.core.api.ui.UiSlot;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.hud.UiHudService;
import ru.realite.ui.settings.UiSettings;
import ru.realite.ui.settings.UiSettingsStore;
import ru.realite.ui.util.UiText;

public final class UiProviderSelectMenu extends BaseMenu {

    private static final int SIZE = 54;
    private static final int LAST_ROW_START = 45;

    private final MiniMessageMessages messages;
    private final UiSettingsStore settingsStore;
    private final UiHudService hudService;
    private final UiRegistry registry;
    private final UiSlot slot;

    public UiProviderSelectMenu(MiniMessageMessages messages,
                                UiSettingsStore settingsStore,
                                UiHudService hudService,
                                UiRegistry registry,
                                UiSlot slot) {
        super(SIZE, messages.get("ui.settings.select_title"));
        this.messages = messages;
        this.settingsStore = settingsStore;
        this.hudService = hudService;
        this.registry = registry;
        this.slot = slot;
    }

    public void open(Player player) {
        build(player);
        super.open(player);
    }

    private void build(Player player) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        UiSettings settings = settingsStore.get(player.getUniqueId());
        List<UiProviderId> providers = new ArrayList<>(registry.providerIds());
        providers.sort(Comparator.comparing(UiProviderId::value));

        int slotIndex = 0;
        for (UiProviderId providerId : providers) {
            if (slotIndex >= LAST_ROW_START) {
                break;
            }
            String providerName = UiText.providerName(messages, providerId);
            List<Component> lore = new ArrayList<>();
            if (settings.provider(slot).map(providerId::equals).orElse(false)) {
                lore.add(messages.get("ui.settings.selected"));
            }
            setButton(slotIndex, Material.NAME_TAG, messages.get("ui.settings.provider_item",
                    Map.of("provider", providerName)), lore, p -> {
                settings.setProvider(slot, providerId);
                settingsStore.save(p.getUniqueId());
                hudService.refresh(p, slot);
                new UiSettingsMenu(messages, settingsStore, hudService, registry).open(p);
            });
            slotIndex++;
        }

        setButton(LAST_ROW_START, Material.BARRIER, messages.get("ui.settings.none"), null, p -> {
            settings.setProvider(slot, null);
            settingsStore.save(p.getUniqueId());
            hudService.refresh(p, slot);
            new UiSettingsMenu(messages, settingsStore, hudService, registry).open(p);
        });
        setButton(LAST_ROW_START + 4, Material.ARROW, messages.get("ui.menu.back"), null,
                p -> new UiSettingsMenu(messages, settingsStore, hudService, registry).open(p));
        setButton(LAST_ROW_START + 8, Material.OAK_DOOR, messages.get("ui.menu.close"), null, Player::closeInventory);
    }
}
