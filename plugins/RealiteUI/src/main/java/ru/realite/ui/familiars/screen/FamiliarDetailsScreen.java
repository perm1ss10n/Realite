package ru.realite.ui.familiars.screen;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.familiars.menu.FamiliarDetailsMenu;

public final class FamiliarDetailsScreen implements UiScreen {

    private final CoreApi core;
    private final MiniMessageMessages messages;

    public FamiliarDetailsScreen(CoreApi core, MiniMessageMessages messages) {
        this.core = core;
        this.messages = messages;
    }

    @Override
    public String id() {
        return "familiars.details";
    }

    @Override
    public void open(Player player, @Nullable String payload) {
        FamiliarUiService service = resolveService();
        if (service == null) {
            player.sendMessage(messages.get("ui.familiars.unavailable"));
            return;
        }
        String typeId = parseTypeId(payload);
        if (typeId == null) {
            player.sendMessage(messages.get("ui.familiars.details.missing"));
            return;
        }
        String displayName = service.detailsData(player, typeId)
                .map(data -> data.name())
                .orElse(typeId);
        UiScreenRegistry screenRegistry = core.services().get(UiScreenRegistry.class);
        new FamiliarDetailsMenu(messages, service, screenRegistry, typeId, displayName).open(player);
    }

    private FamiliarUiService resolveService() {
        if (core == null) {
            return null;
        }
        return core.services().get(FamiliarUiService.class);
    }

    private String parseTypeId(@Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String raw = payload.trim();
        if (raw.startsWith("type=")) {
            raw = raw.substring("type=".length()).trim();
        }
        if (raw.startsWith("type:")) {
            raw = raw.substring("type:".length()).trim();
        }
        return raw.isBlank() ? null : raw;
    }
}
