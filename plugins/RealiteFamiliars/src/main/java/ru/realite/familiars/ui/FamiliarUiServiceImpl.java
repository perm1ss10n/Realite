package ru.realite.familiars.ui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import ru.realite.core.api.familiars.FamiliarActionResult;
import ru.realite.core.api.familiars.FamiliarDetailsData;
import ru.realite.core.api.familiars.FamiliarHudActive;
import ru.realite.core.api.familiars.FamiliarHudData;
import ru.realite.core.api.familiars.FamiliarManagerData;
import ru.realite.core.api.familiars.FamiliarSummary;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.familiars.FamiliarUiState;
import ru.realite.core.api.ui.UiInvalidateEvent;
import ru.realite.familiars.core.CoreAccess;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarState;
import ru.realite.familiars.model.FamiliarType;
import ru.realite.familiars.service.CheckResult;
import ru.realite.familiars.service.FamiliarLimitInfo;
import ru.realite.familiars.service.FamiliarService;
import ru.realite.familiars.service.VirtualInventoryService;

public final class FamiliarUiServiceImpl implements FamiliarUiService {

    private static final int XP_MAX = 100;

    private final FamiliarService service;
    private final VirtualInventoryService inventoryService;
    private final Map<UUID, String> activeSelections = new ConcurrentHashMap<>();

    public FamiliarUiServiceImpl(FamiliarService service, VirtualInventoryService inventoryService) {
        this.service = service;
        this.inventoryService = inventoryService;
    }

    @Override
    public Optional<FamiliarHudData> hudData(Player player) {
        if (player == null || service == null) {
            return Optional.empty();
        }
        List<FamiliarInstance> familiars = service.getFamiliars(player.getUniqueId());
        FamiliarLimitInfo limitInfo = service.getLimitInfo(player);
        Optional<FamiliarInstance> active = resolveActive(player.getUniqueId(), familiars);
        Optional<FamiliarHudActive> activeData = active.flatMap(instance -> buildActive(player, instance));
        FamiliarHudData data = new FamiliarHudData(familiars.size(), limitInfo.limit(), activeData);
        return Optional.of(data);
    }

    @Override
    public Optional<FamiliarManagerData> managerData(Player player) {
        if (player == null || service == null) {
            return Optional.empty();
        }
        List<FamiliarInstance> familiars = service.getFamiliars(player.getUniqueId());
        List<FamiliarSummary> summaries = new ArrayList<>();
        for (FamiliarInstance instance : familiars) {
            FamiliarType type = service.getType(instance.typeId()).orElse(null);
            summaries.add(new FamiliarSummary(
                    instance.typeId(),
                    resolveName(instance, type),
                    resolveMobType(instance.typeId()),
                    instance.level(),
                    type != null ? type.role() : "-",
                    mapState(instance.state())));
        }
        summaries.sort(Comparator.comparing(FamiliarSummary::typeId, String.CASE_INSENSITIVE_ORDER));
        Optional<String> activeType = resolveActiveType(player.getUniqueId(), familiars);
        return Optional.of(new FamiliarManagerData(summaries, activeType));
    }

    @Override
    public Optional<FamiliarDetailsData> detailsData(Player player, String typeId) {
        if (player == null || service == null || typeId == null || typeId.isBlank()) {
            return Optional.empty();
        }
        FamiliarInstance instance = findInstance(player.getUniqueId(), typeId);
        if (instance == null) {
            return Optional.empty();
        }
        FamiliarType type = service.getType(instance.typeId()).orElse(null);
        Map<String, Integer> stats = type != null ? type.baseStats() : Map.of();
        boolean inventoryEnabled = inventoryService != null;
        List<String> inventory = inventoryService != null
                ? inventoryService.describe(instance.inventory())
                : List.of();
        FamiliarDetailsData data = new FamiliarDetailsData(
                instance.typeId(),
                resolveName(instance, type),
                resolveMobType(instance.typeId()),
                instance.level(),
                clampXp(instance.xp()),
                XP_MAX,
                type != null ? type.role() : "-",
                mapState(instance.state()),
                stats,
                List.of(),
                inventoryEnabled,
                inventory);
        return Optional.of(data);
    }

    @Override
    public boolean openInventory(Player player, String typeId) {
        if (player == null || typeId == null || typeId.isBlank()) {
            return false;
        }
        FamiliarInstance instance = findInstance(player.getUniqueId(), typeId);
        if (instance == null) {
            return false;
        }
        if (instance.state() == FamiliarState.SUMMONED && instance.summonedEntityId().isPresent()) {
            Entity entity = Bukkit.getEntity(instance.summonedEntityId().get());
            if (entity instanceof org.bukkit.inventory.InventoryHolder holder) {
                player.openInventory(holder.getInventory());
                return true;
            }
        }
        if (inventoryService == null) {
            return false;
        }
        FamiliarType type = service.getType(instance.typeId()).orElse(null);
        return inventoryService.open(player, instance, resolveName(instance, type));
    }

    @Override
    public FamiliarActionResult summon(Player player, String typeId) {
        return handleAction(player, typeId, service.summon(player, typeId), true);
    }

    @Override
    public FamiliarActionResult dismiss(Player player, String typeId) {
        return handleAction(player, typeId, service.dismiss(player, typeId), false);
    }

    @Override
    public FamiliarActionResult setActive(Player player, String typeId) {
        if (player == null || typeId == null || typeId.isBlank()) {
            return FamiliarActionResult.denied(List.of("Invalid familiar id."));
        }
        FamiliarInstance instance = findInstance(player.getUniqueId(), typeId);
        if (instance == null) {
            return FamiliarActionResult.denied(List.of("Familiar not found."));
        }
        if (instance.state() != FamiliarState.SUMMONED) {
            return FamiliarActionResult.denied(List.of("Familiar is not summoned."));
        }
        activeSelections.put(player.getUniqueId(), instance.typeId());
        publishInvalidate(player);
        return FamiliarActionResult.success();
    }

    @Override
    public FamiliarActionResult canRelease(Player player, String typeId) {
        if (player == null || typeId == null || typeId.isBlank()) {
            return FamiliarActionResult.denied(List.of("Invalid familiar id."));
        }
        CheckResult result = service.canRelease(player, typeId);
        if (result.allowed()) {
            return FamiliarActionResult.success();
        }
        return FamiliarActionResult.denied(result.reasons());
    }

    @Override
    public FamiliarActionResult release(Player player, String typeId) {
        if (player == null || typeId == null || typeId.isBlank()) {
            return FamiliarActionResult.denied(List.of("Invalid familiar id."));
        }
        CheckResult result = service.releaseFamiliar(player, typeId);
        if (result.allowed()) {
            activeSelections.computeIfPresent(player.getUniqueId(),
                    (key, value) -> value.equalsIgnoreCase(typeId) ? null : value);
            publishInvalidate(player);
            return FamiliarActionResult.success();
        }
        return FamiliarActionResult.denied(result.reasons());
    }

    @Override
    public FamiliarActionResult rename(Player player, String typeId, String name) {
        return FamiliarActionResult.denied(List.of("Rename not supported yet."));
    }

    private FamiliarActionResult handleAction(Player player, String typeId, CheckResult result, boolean setActive) {
        if (result.allowed()) {
            if (setActive && player != null && typeId != null) {
                activeSelections.put(player.getUniqueId(), typeId);
            } else if (!setActive && player != null && typeId != null) {
                activeSelections.computeIfPresent(player.getUniqueId(), (key, value) -> value.equalsIgnoreCase(typeId) ? null : value);
            }
            publishInvalidate(player);
            return FamiliarActionResult.success();
        }
        return FamiliarActionResult.denied(result.reasons());
    }

    private Optional<FamiliarInstance> resolveActive(UUID owner, List<FamiliarInstance> familiars) {
        Optional<String> active = resolveActiveType(owner, familiars);
        if (active.isPresent()) {
            String typeId = active.get();
            for (FamiliarInstance instance : familiars) {
                if (instance.typeId().equalsIgnoreCase(typeId)) {
                    return Optional.of(instance);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> resolveActiveType(UUID owner, List<FamiliarInstance> familiars) {
        if (owner == null) {
            return Optional.empty();
        }
        String selected = activeSelections.get(owner);
        if (selected != null) {
            boolean exists = familiars.stream().anyMatch(instance -> instance.typeId().equalsIgnoreCase(selected));
            if (!exists) {
                activeSelections.remove(owner);
            } else {
                return Optional.of(selected);
            }
        }
        return familiars.stream()
                .filter(instance -> instance.state() == FamiliarState.SUMMONED)
                .findFirst()
                .map(FamiliarInstance::typeId);
    }

    private FamiliarInstance findInstance(UUID owner, String typeId) {
        if (owner == null || typeId == null) {
            return null;
        }
        for (FamiliarInstance instance : service.getFamiliars(owner)) {
            if (instance.typeId().equalsIgnoreCase(typeId)) {
                return instance;
            }
        }
        return null;
    }

    private Optional<FamiliarHudActive> buildActive(Player player, FamiliarInstance instance) {
        FamiliarType type = service.getType(instance.typeId()).orElse(null);
        String name = resolveName(instance, type);
        String role = type != null ? type.role() : "-";
        OptionalInt hpCurrent = OptionalInt.empty();
        OptionalInt hpMax = OptionalInt.empty();
        OptionalDouble distance = OptionalDouble.empty();
        if (instance.state() == FamiliarState.SUMMONED && instance.summonedEntityId().isPresent()) {
            Entity entity = Bukkit.getEntity(instance.summonedEntityId().get());
            if (entity != null && player != null) {
                double dist = player.getLocation().distance(entity.getLocation());
                distance = OptionalDouble.of(dist);
                if (entity instanceof LivingEntity living) {
                    double current = Math.max(0.0, living.getHealth());
                    double max = current;
                    var attr = living.getAttribute(Attribute.MAX_HEALTH);
                    if (attr != null) {
                        max = attr.getValue();
                    }
                    hpCurrent = OptionalInt.of((int) Math.round(current));
                    hpMax = OptionalInt.of((int) Math.round(max));
                }
            }
        }
        return Optional.of(new FamiliarHudActive(
                instance.typeId(),
                name,
                instance.level(),
                role,
                hpCurrent,
                hpMax,
                distance));
    }

    private FamiliarUiState mapState(FamiliarState state) {
        return switch (state) {
            case SUMMONED -> FamiliarUiState.SUMMONED;
            case COOLDOWN -> FamiliarUiState.COOLDOWN;
            case IDLE -> FamiliarUiState.IDLE;
        };
    }

    private String resolveName(FamiliarInstance instance, FamiliarType type) {
        if (type != null && type.id() != null && !type.id().isBlank()) {
            return type.id();
        }
        return instance.typeId();
    }

    private String resolveMobType(String typeId) {
        EntityType type = switch (typeId == null ? "" : typeId.toLowerCase()) {
            case "wolf" -> EntityType.WOLF;
            case "fairy" -> EntityType.ALLAY;
            case "fox" -> EntityType.FOX;
            default -> EntityType.WOLF;
        };
        return type.name().toLowerCase();
    }

    private int clampXp(int xp) {
        if (xp < 0) {
            return 0;
        }
        return Math.min(xp, XP_MAX);
    }

    private void publishInvalidate(Player player) {
        if (player == null) {
            return;
        }
        CoreAccess.core().events().publish(new UiInvalidateEvent(player, HUD_PROVIDER_ID));
    }
}
