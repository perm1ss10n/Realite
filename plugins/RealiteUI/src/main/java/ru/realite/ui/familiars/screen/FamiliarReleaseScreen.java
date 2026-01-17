package ru.realite.ui.familiars.screen;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.familiars.FamiliarDetailsData;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.ui.UiScreen;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.core.i18n.MiniMessageMessages;
import ru.realite.ui.familiars.menu.FamiliarReleaseConfirmMenu;

public final class FamiliarReleaseScreen implements UiScreen {

    private final CoreApi core;
    private final MiniMessageMessages messages;

    public FamiliarReleaseScreen(CoreApi core, MiniMessageMessages messages) {
        this.core = core;
        this.messages = messages;
    }

    @Override
    public String id() {
        return "familiars.release";
    }

    @Override
    public void open(Player player, @Nullable String payload) {
        FamiliarUiService service = resolveService();
        if (service == null) {
            player.sendMessage(messages.get("ui.familiars.unavailable"));
            return;
        }
        Map<String, String> params = parsePayload(payload);
        String typeId = params.get("type");
        if (typeId == null || typeId.isBlank()) {
            player.sendMessage(messages.get("ui.familiars.details.missing"));
            return;
        }
        String displayName = service.detailsData(player, typeId)
                .map(FamiliarDetailsData::name)
                .orElse(typeId);
        UiScreenRegistry screenRegistry = core.services().get(UiScreenRegistry.class);
        new FamiliarReleaseConfirmMenu(messages, service, screenRegistry, typeId, displayName,
                resolveBackTarget(typeId, params)).open(player);
    }

    private FamiliarUiService resolveService() {
        if (core == null) {
            return null;
        }
        return core.services().get(FamiliarUiService.class);
    }

    private Map<String, String> parsePayload(@Nullable String payload) {
        Map<String, String> params = new HashMap<>();
        if (payload == null || payload.isBlank()) {
            return params;
        }
        String[] parts = payload.split("[;,]");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                if (!params.containsKey("type")) {
                    params.put("type", trimmed);
                }
                continue;
            }
            String key = trimmed.substring(0, eq).trim().toLowerCase();
            String value = trimmed.substring(eq + 1).trim();
            if (!key.isBlank() && !value.isBlank()) {
                params.put(key, value);
            }
        }
        return params;
    }

    private String resolveBackTarget(String typeId, Map<String, String> params) {
        String back = params.getOrDefault("back", "manager");
        String selection = params.get("selection");
        if ("details".equalsIgnoreCase(back)) {
            return "familiars.details:" + typeId;
        }
        if ("manager".equalsIgnoreCase(back)) {
            return selection == null || selection.isBlank()
                    ? "familiars.manager"
                    : "familiars.manager:select=" + selection;
        }
        return null;
    }
}
