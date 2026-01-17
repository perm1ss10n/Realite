package ru.realite.ui.familiars;

import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.familiars.FamiliarHudActive;
import ru.realite.core.api.familiars.FamiliarHudData;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.ui.UiHudTextProvider;
import ru.realite.core.api.ui.UiProvider;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiSlot;
import ru.realite.core.api.ui.UiSnapshot;
import ru.realite.core.i18n.MiniMessageMessages;

public final class FamiliarHudProvider implements UiProvider, UiHudTextProvider {

    private final CoreApi core;
    private final MiniMessageMessages messages;

    public FamiliarHudProvider(CoreApi core, MiniMessageMessages messages) {
        this.core = core;
        this.messages = messages;
    }

    @Override
    public UiProviderId id() {
        return FamiliarUiService.HUD_PROVIDER_ID;
    }

    @Override
    public Optional<UiSnapshot> snapshot(Player player) {
        FamiliarUiService uiService = resolveService();
        if (uiService == null) {
            return Optional.empty();
        }
        return uiService.hudData(player)
                .map(data -> new UiSnapshot(data.count(), data.max()));
    }

    @Override
    public Optional<Component> text(Player player, UiSlot slot) {
        if (slot != UiSlot.ACTION_BAR) {
            return Optional.empty();
        }
        FamiliarUiService uiService = resolveService();
        if (uiService == null) {
            return Optional.empty();
        }
        Optional<FamiliarHudData> data = uiService.hudData(player);
        if (data.isEmpty()) {
            return Optional.empty();
        }
        FamiliarHudData hud = data.get();
        String hint = messages.rawOr("ui.familiars.hud.hint", "Open control");
        if (hud.active().isEmpty()) {
            return Optional.of(messages.get("ui.familiars.hud.actionbar_empty", Map.of(
                    "count", String.valueOf(hud.count()),
                    "max", String.valueOf(hud.max()),
                    "hint", hint)));
        }
        FamiliarHudActive active = hud.active().get();
        String hp = active.hpCurrent().isPresent() && active.hpMax().isPresent()
                ? active.hpCurrent().getAsInt() + "/" + active.hpMax().getAsInt()
                : "-";
        String distance = active.distanceMeters().isPresent()
                ? String.format("%.1fm", active.distanceMeters().getAsDouble())
                : "-";
        String model = active.modelId().orElse("-");
        return Optional.of(messages.get("ui.familiars.hud.actionbar", Map.of(
                "count", String.valueOf(hud.count()),
                "max", String.valueOf(hud.max()),
                "name", active.name(),
                "level", String.valueOf(active.level()),
                "role", active.role(),
                "hp", hp,
                "distance", distance,
                "model", model,
                "hint", hint)));
    }

    @Override
    public boolean isAvailable(Player player) {
        return resolveService() != null;
    }

    private FamiliarUiService resolveService() {
        if (core == null) {
            return null;
        }
        return core.services().get(FamiliarUiService.class);
    }
}
