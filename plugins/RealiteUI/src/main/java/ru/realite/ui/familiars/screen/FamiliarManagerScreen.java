package ru.realite.ui.familiars.screen;

import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.familiars.menu.FamiliarManagerMenu;

public final class FamiliarManagerScreen implements UiScreen {

    private final CoreApi core;
    private final MiniMessageMessages messages;

    public FamiliarManagerScreen(CoreApi core, MiniMessageMessages messages) {
        this.core = core;
        this.messages = messages;
    }

    @Override
    public String id() {
        return "familiars.manager";
    }

    @Override
    public void open(Player player, @Nullable String payload) {
        FamiliarUiService service = resolveService();
        if (service == null) {
            player.sendMessage(messages.get("ui.familiars.unavailable"));
            return;
        }
        String selection = parseSelection(payload);
        UiScreenRegistry screenRegistry = core.services().get(UiScreenRegistry.class);
        new FamiliarManagerMenu(messages, service, screenRegistry, selection).open(player);
    }

    private FamiliarUiService resolveService() {
        if (core == null) {
            return null;
        }
        return core.services().get(FamiliarUiService.class);
    }

    private String parseSelection(@Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        String raw = payload.trim();
        if (raw.startsWith("select=")) {
            return raw.substring("select=".length()).trim();
        }
        if (raw.startsWith("select:")) {
            return raw.substring("select:".length()).trim();
        }
        return raw;
    }
}
