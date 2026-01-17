package ru.realite.familiars.ui;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiHudTextProvider;
import ru.realite.core.api.ui.UiProvider;
import ru.realite.core.api.ui.UiProviderId;
import ru.realite.core.api.ui.UiSlot;
import ru.realite.core.api.ui.UiSnapshot;
import ru.realite.familiars.config.Messages;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarState;
import ru.realite.familiars.model.FamiliarType;
import ru.realite.familiars.service.FamiliarService;

public final class FamiliarHudProvider implements UiProvider, UiHudTextProvider {

    public static final UiProviderId ID = new UiProviderId("familiars.status");

    private final FamiliarService service;
    private final Messages messages;

    public FamiliarHudProvider(FamiliarService service, Messages messages) {
        this.service = service;
        this.messages = messages;
    }

    @Override
    public UiProviderId id() {
        return ID;
    }

    @Override
    public Optional<UiSnapshot> snapshot(Player player) {
        FamiliarInstance instance = selectInstance(player);
        if (instance == null) {
            return Optional.empty();
        }
        int xpPercent = clampPercent(instance.xp());
        return Optional.of(new UiSnapshot(xpPercent, 100));
    }

    @Override
    public Optional<Component> text(Player player, UiSlot slot) {
        if (slot != UiSlot.ACTION_BAR) {
            return Optional.empty();
        }
        FamiliarInstance instance = selectInstance(player);
        if (instance == null) {
            return Optional.empty();
        }
        FamiliarType type = service.getType(instance.typeId()).orElse(null);
        String typeName = type != null ? type.id() : instance.typeId();
        String role = type != null ? type.role() : "-";
        String state = instance.state() == FamiliarState.SUMMONED
                ? safeRaw("hud.state.summoned", "<green>summoned</green>")
                : safeRaw("hud.state.tamed", "<yellow>tamed</yellow>");

        String distance = "-";
        String hp = "-";
        if (instance.state() == FamiliarState.SUMMONED && instance.summonedEntityId().isPresent()) {
            Entity entity = Bukkit.getEntity(instance.summonedEntityId().get());
            if (entity != null && player != null) {
                double dist = player.getLocation().distance(entity.getLocation());
                distance = String.format("%.1fm", dist);
                if (entity instanceof LivingEntity living) {
                    double current = Math.max(0.0, living.getHealth());

                    double max = 0.0;
                    var attr = living.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                    if (attr != null) {
                        max = attr.getValue(); // текущее максимальное (с баффами/дебаффами)
                    } else {
                        // на всякий случай фоллбек (если у сущности нет атрибута)
                        max = current;
                    }

                    hp = String.format("%.0f/%.0f", current, max);

                }
            }
        }

        int xpPercent = clampPercent(instance.xp());
        return Optional.of(messages.get("hud.actionbar", Map.of(
                "type", typeName,
                "role", role,
                "level", String.valueOf(instance.level()),
                "xp", String.valueOf(xpPercent),
                "state", state,
                "distance", distance,
                "hp", hp)));
    }

    @Override
    public boolean isAvailable(Player player) {
        return service != null && player != null;
    }

    private FamiliarInstance selectInstance(Player player) {
        if (player == null || service == null) {
            return null;
        }
        List<FamiliarInstance> familiars = service.getFamiliars(player.getUniqueId());
        if (familiars.isEmpty()) {
            return null;
        }
        return familiars.stream()
                .sorted(Comparator.comparing((FamiliarInstance inst) -> inst.state() == FamiliarState.SUMMONED)
                        .reversed())
                .findFirst()
                .orElse(null);
    }

    private int clampPercent(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 100);
    }

    private String safeRaw(String key, String fallback) {
        String raw = messages.raw(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return raw;
    }
}
