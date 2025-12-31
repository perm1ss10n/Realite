package ru.realite.quests.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.realite.core.api.EventBus;
import ru.realite.core.api.Platform;
import ru.realite.core.api.events.QuestCompletedEvent;
import ru.realite.core.api.events.QuestStartedEvent;
import ru.realite.core.api.quests.QuestProgress;
import ru.realite.core.api.quests.QuestService;
import ru.realite.core.api.quests.QuestState;
import ru.realite.core.api.quests.QuestStartTrigger;
import ru.realite.core.api.quests.QuestUnlockService;
import ru.realite.core.api.quests.CityAdapter;
import ru.realite.core.api.quests.GuildAdapter;
import ru.realite.quests.model.ObjectiveDefinition;
import ru.realite.quests.model.ObjectiveType;
import ru.realite.quests.model.QuestConditions;
import ru.realite.quests.model.QuestDefinition;
import ru.realite.quests.model.QuestType;
import ru.realite.quests.model.RewardDefinition;
import ru.realite.quests.model.RewardType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class QuestServiceImpl implements QuestService {

    private final Platform logger;
    private final EventBus eventBus;
    private final java.nio.file.Path questsDir;
    private QuestRepository repository;
    private final QuestProgressRepository progressRepository;
    private final QuestUnlockService questUnlockService;
    private final CityAdapter cityAdapter;
    private final GuildAdapter guildAdapter;

    private static final String FEATURE_UNAVAILABLE_REASON = "feature unavailable";

    public QuestServiceImpl(Platform logger,
            EventBus eventBus,
            java.nio.file.Path questsDir,
            QuestRepository repository,
            QuestProgressRepository progressRepository,
            QuestUnlockService questUnlockService,
            CityAdapter cityAdapter,
            GuildAdapter guildAdapter) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus");
        this.questsDir = Objects.requireNonNull(questsDir, "questsDir");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.progressRepository = Objects.requireNonNull(progressRepository, "progressRepository");
        this.questUnlockService = questUnlockService;
        this.cityAdapter = cityAdapter;
        this.guildAdapter = guildAdapter;
    }

    @Override
    public void start(Player player, String questId) {
        start(player, questId, QuestStartTrigger.COMMAND, false);
    }

    @Override
    public void start(Player player, String questId, QuestStartTrigger trigger, boolean force) {
        if (player == null || questId == null || questId.isBlank()) {
            return;
        }
        QuestDefinition quest = repository.get(questId);
        if (quest == null) {
            logger.warn("[Quests] Quest not found: " + questId);
            return;
        }
        QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
        if (progress != null && progress.state() == QuestState.ACTIVE) {
            return;
        }
        if (progress != null && progress.state() == QuestState.COMPLETED
                && quest.type() == QuestType.INTRO && !force) {
            return;
        }
        QuestProgressData fresh = new QuestProgressData(QuestState.ACTIVE, false, Set.of(), Map.of());
        progressRepository.save(player.getUniqueId(), quest.id(), fresh);
        eventBus.publish(new QuestStartedEvent(player.getUniqueId(), quest.id(), trigger));
        logger.info("[Quests] Started quest " + quest.id() + " for " + player.getName());
    }

    @Override
    public boolean isActive(Player player, String questId) {
        if (player == null || questId == null || questId.isBlank()) {
            return false;
        }
        QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), questId);
        return progress != null && progress.state() == QuestState.ACTIVE;
    }

    @Override
    public QuestProgress getProgress(Player player, String questId) {
        if (player == null || questId == null || questId.isBlank()) {
            return null;
        }
        return progressRepository.getProgress(player.getUniqueId(), questId);
    }

    public List<String> getActiveQuestIds(Player player) {
        List<String> result = new ArrayList<>();
        if (player == null) {
            return result;
        }
        Set<String> active = new java.util.HashSet<>(progressRepository.getActiveQuestIds(player.getUniqueId()));
        for (QuestDefinition quest : repository.all()) {
            if (isActive(player, quest.id())) {
                active.add(quest.id());
            }
        }
        result.addAll(active);
        return result;
    }

    public boolean reloadQuests() {
        QuestRepository loaded = new QuestLoader(questsDir, logger).load();
        this.repository = loaded;
        return true;
    }

    public void handleNpcInteract(Player player, String npcId, String npcName) {
        if (player == null || npcId == null) {
            return;
        }
        for (QuestDefinition quest : repository.all()) {
            QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            boolean updated = false;
            for (ObjectiveDefinition objective : quest.objectives()) {
                if (objective.type() != ObjectiveType.INTERACT_NPC) {
                    continue;
                }
                if (progress.completedObjectives().contains(objective.id())) {
                    continue;
                }
                if (!canProgressObjective(player, player.getLocation(), quest, objective)) {
                    continue;
                }
                String target = objective.npcId();
                if (matchesNpc(target, npcId, npcName)) {
                    progress.completedObjectivesMutable().add(objective.id());
                    notifyObjectiveCompleted(player, objective);
                    updated = true;
                }
            }
            if (updated) {
                progressRepository.save(player.getUniqueId(), quest.id(), progress);
                tryCompleteQuest(player, quest, progress);
            }
        }
    }

    public void handleKill(Player player, EntityType entityType, Location location) {
        if (player == null || entityType == null) {
            return;
        }
        for (QuestDefinition quest : repository.all()) {
            QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            boolean updated = false;
            for (ObjectiveDefinition objective : quest.objectives()) {
                if (objective.type() != ObjectiveType.KILL) {
                    continue;
                }
                if (progress.completedObjectives().contains(objective.id())) {
                    continue;
                }
                if (!canProgressObjective(player, location, quest, objective)) {
                    continue;
                }
                if (objective.entityType() != entityType) {
                    continue;
                }
                int current = progress.objectiveCountsMutable().getOrDefault(objective.id(), 0) + 1;
                if (current >= objective.amount()) {
                    progress.completedObjectivesMutable().add(objective.id());
                    progress.objectiveCountsMutable().remove(objective.id());
                    notifyObjectiveCompleted(player, objective);
                } else {
                    progress.objectiveCountsMutable().put(objective.id(), current);
                }
                updated = true;
            }
            if (updated) {
                progressRepository.save(player.getUniqueId(), quest.id(), progress);
                tryCompleteQuest(player, quest, progress);
            }
        }
    }

    public void handleLocation(Player player, Location location) {
        if (player == null || location == null) {
            return;
        }
        for (QuestDefinition quest : repository.all()) {
            QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            boolean updated = false;
            for (ObjectiveDefinition objective : quest.objectives()) {
                if (objective.type() != ObjectiveType.GO_TO_LOCATION) {
                    continue;
                }
                if (progress.completedObjectives().contains(objective.id())) {
                    continue;
                }
                if (!canProgressObjective(player, location, quest, objective)) {
                    continue;
                }
                if (!matchesLocation(objective, location)) {
                    continue;
                }
                progress.completedObjectivesMutable().add(objective.id());
                notifyObjectiveCompleted(player, objective);
                updated = true;
            }
            if (updated) {
                progressRepository.save(player.getUniqueId(), quest.id(), progress);
                tryCompleteQuest(player, quest, progress);
            }
        }
    }

    public void handleBlockPlace(Player player, Material material, Location location) {
        if (player == null || material == null) {
            return;
        }
        for (QuestDefinition quest : repository.all()) {
            QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            boolean updated = false;
            for (ObjectiveDefinition objective : quest.objectives()) {
                if (objective.type() != ObjectiveType.PLACE_BLOCK) {
                    continue;
                }
                if (progress.completedObjectives().contains(objective.id())) {
                    continue;
                }
                if (!canProgressObjective(player, location, quest, objective)) {
                    continue;
                }
                if (!matchesMaterial(objective, material)) {
                    continue;
                }
                int current = progress.objectiveCountsMutable().getOrDefault(objective.id(), 0) + 1;
                if (current >= objective.amount()) {
                    progress.completedObjectivesMutable().add(objective.id());
                    progress.objectiveCountsMutable().remove(objective.id());
                    notifyObjectiveCompleted(player, objective);
                } else {
                    progress.objectiveCountsMutable().put(objective.id(), current);
                }
                updated = true;
            }
            if (updated) {
                progressRepository.save(player.getUniqueId(), quest.id(), progress);
                tryCompleteQuest(player, quest, progress);
            }
        }
    }

    public void handleBlockBreak(Player player, Material material, Location location) {
        if (player == null || material == null) {
            return;
        }
        for (QuestDefinition quest : repository.all()) {
            QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            boolean updated = false;
            for (ObjectiveDefinition objective : quest.objectives()) {
                if (objective.type() != ObjectiveType.BREAK_BLOCK) {
                    continue;
                }
                if (progress.completedObjectives().contains(objective.id())) {
                    continue;
                }
                if (!canProgressObjective(player, location, quest, objective)) {
                    continue;
                }
                if (!matchesMaterial(objective, material)) {
                    continue;
                }
                int current = progress.objectiveCountsMutable().getOrDefault(objective.id(), 0) + 1;
                if (current >= objective.amount()) {
                    progress.completedObjectivesMutable().add(objective.id());
                    progress.objectiveCountsMutable().remove(objective.id());
                    notifyObjectiveCompleted(player, objective);
                } else {
                    progress.objectiveCountsMutable().put(objective.id(), current);
                }
                updated = true;
            }
            if (updated) {
                progressRepository.save(player.getUniqueId(), quest.id(), progress);
                tryCompleteQuest(player, quest, progress);
            }
        }
    }

    public void handleHoldItem(Player player) {
        if (player == null) {
            return;
        }
        for (QuestDefinition quest : repository.all()) {
            QuestProgressData progress = progressRepository.getProgress(player.getUniqueId(), quest.id());
            if (progress == null || progress.state() != QuestState.ACTIVE) {
                continue;
            }
            boolean updated = false;
            for (ObjectiveDefinition objective : quest.objectives()) {
                if (objective.type() != ObjectiveType.HOLD_ITEM) {
                    continue;
                }
                if (progress.completedObjectives().contains(objective.id())) {
                    continue;
                }
                if (!canProgressObjective(player, player.getLocation(), quest, objective)) {
                    continue;
                }
                int count = countInventory(player, objective);
                if (count >= objective.amount()) {
                    progress.completedObjectivesMutable().add(objective.id());
                    notifyObjectiveCompleted(player, objective);
                    updated = true;
                }
            }
            if (updated) {
                progressRepository.save(player.getUniqueId(), quest.id(), progress);
                tryCompleteQuest(player, quest, progress);
            }
        }
    }

    private void tryCompleteQuest(Player player, QuestDefinition quest, QuestProgressData progress) {
        if (progress.state() != QuestState.ACTIVE) {
            return;
        }
        for (ObjectiveDefinition objective : quest.objectives()) {
            if (!progress.completedObjectives().contains(objective.id())) {
                return;
            }
        }
        progress.state(QuestState.COMPLETED);
        boolean grantRewards = !progress.rewardGranted();
        if (grantRewards) {
            progress.rewardGranted(true);
        }
        progressRepository.save(player.getUniqueId(), quest.id(), progress);
        if (grantRewards) {
            grantRewards(player, quest);
            eventBus.publish(new QuestCompletedEvent(player.getUniqueId(), quest.id()));
            notifyQuestCompleted(player, quest);
            logger.info("[Quests] Quest completed: " + quest.id() + " for " + player.getName());
        }
    }

    private void grantRewards(Player player, QuestDefinition quest) {
        for (RewardDefinition reward : quest.rewards()) {
            if (reward.type() == RewardType.XP) {
                player.giveExp(reward.amount());
            } else if (reward.type() == RewardType.ITEM) {
                Material material = reward.material();
                if (material == null) {
                    continue;
                }
                ItemStack stack = new ItemStack(material, reward.amount());
                Map<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
                for (ItemStack item : leftovers.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), item);
                }
            } else if (reward.type() == RewardType.QUEST_UNLOCK) {
                if (questUnlockService == null) {
                    continue;
                }
                String unlockId = reward.unlockId();
                if (unlockId == null || unlockId.isBlank()) {
                    continue;
                }
                questUnlockService.grantUnlock(player, unlockId);
            }
        }
    }

    private boolean matchesNpc(String target, String npcId, String npcName) {
        if (target == null) {
            return false;
        }
        String normalizedTarget = target.trim().toLowerCase(Locale.ROOT);
        if (normalizedTarget.equals(npcId.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return npcName != null && normalizedTarget.equals(npcName.toLowerCase(Locale.ROOT));
    }

    private boolean matchesLocation(ObjectiveDefinition objective, Location location) {
        if (objective.world() == null || location.getWorld() == null) {
            return false;
        }
        if (!objective.world().equalsIgnoreCase(location.getWorld().getName())) {
            return false;
        }
        double dx = location.getX() - objective.x();
        double dy = location.getY() - objective.y();
        double dz = location.getZ() - objective.z();
        double distanceSquared = dx * dx + dy * dy + dz * dz;
        return distanceSquared <= objective.radius() * objective.radius();
    }

    private boolean matchesMaterial(ObjectiveDefinition objective, Material material) {
        for (Material allowed : objective.materials()) {
            if (allowed == material) {
                return true;
            }
        }
        return false;
    }

    private int countInventory(Player player, ObjectiveDefinition objective) {
        int total = 0;
        for (Material material : objective.materials()) {
            for (ItemStack stack : player.getInventory().all(material).values()) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean canProgressObjective(Player player, Location location, QuestDefinition quest,
                                         ObjectiveDefinition objective) {
        return checkConditions(player, location, quest.conditions()).allowed()
                && checkConditions(player, location, objective.conditions()).allowed();
    }

    private ConditionCheckResult checkConditions(Player player, Location location, QuestConditions conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return ConditionCheckResult.allow();
        }
        if (player == null) {
            return ConditionCheckResult.deny(null);
        }
        Location target = location != null ? location : player.getLocation();
        if (conditions.requireGuild()) {
            if (guildAdapter == null) {
                return ConditionCheckResult.deny(FEATURE_UNAVAILABLE_REASON);
            }
            if (!guildAdapter.isInGuild(player)) {
                return ConditionCheckResult.deny(null);
            }
        }
        boolean needsCity = conditions.requireCity()
                || conditions.requireOutsideCity()
                || conditions.requirePlot()
                || !conditions.allowedCityIds().isEmpty();
        if (needsCity) {
            if (cityAdapter == null) {
                return ConditionCheckResult.deny(FEATURE_UNAVAILABLE_REASON);
            }
            boolean inCity = cityAdapter.isInsideCityRegion(target);
            if (conditions.requireCity() && !inCity) {
                return ConditionCheckResult.deny(null);
            }
            if (conditions.requireOutsideCity() && inCity) {
                return ConditionCheckResult.deny(null);
            }
            if (conditions.requirePlot() && !cityAdapter.isInsideCityPlot(target)) {
                return ConditionCheckResult.deny(null);
            }
            if (!conditions.allowedCityIds().isEmpty()) {
                String cityId = cityAdapter.getCityId(target).orElse(null);
                if (cityId == null) {
                    return ConditionCheckResult.deny(null);
                }
                boolean allowed = false;
                for (String allowedCityId : conditions.allowedCityIds()) {
                    if (allowedCityId.equalsIgnoreCase(cityId)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    return ConditionCheckResult.deny(null);
                }
            }
        }
        return ConditionCheckResult.allow();
    }

    public int getObjectiveProgressCount(Player player, ObjectiveDefinition objective, QuestProgressData progress) {
        if (objective == null || player == null) {
            return 0;
        }
        if (progress == null) {
            return 0;
        }
        return switch (objective.type()) {
            case KILL, PLACE_BLOCK, BREAK_BLOCK -> progress.objectiveCounts().getOrDefault(objective.id(), 0);
            case HOLD_ITEM -> countInventory(player, objective);
            default -> 0;
        };
    }

    public String describeObjective(ObjectiveDefinition objective) {
        if (objective == null) {
            return "";
        }
        return switch (objective.type()) {
            case INTERACT_NPC -> "Talk to " + objective.npcId();
            case KILL -> "Kill " + objective.amount() + " " + formatEntity(objective.entityType());
            case GO_TO_LOCATION -> "Go to " + objective.world() + " (" + formatLocation(objective) + ")";
            case PLACE_BLOCK -> "Place " + objective.amount() + " " + formatMaterials(objective.materials());
            case BREAK_BLOCK -> "Break " + objective.amount() + " " + formatMaterials(objective.materials());
            case HOLD_ITEM -> "Hold " + objective.amount() + " " + formatMaterials(objective.materials());
        };
    }

    private void notifyObjectiveCompleted(Player player, ObjectiveDefinition objective) {
        String description = describeObjective(objective);
        Component message = Component.text("Objective completed: ", NamedTextColor.GREEN)
                .append(Component.text(description, NamedTextColor.YELLOW));
        player.sendMessage(message);
        player.sendActionBar(message);
    }

    private void notifyQuestCompleted(Player player, QuestDefinition quest) {
        Component message = Component.text("Quest completed: ", NamedTextColor.GOLD)
                .append(Component.text(quest.id(), NamedTextColor.YELLOW));
        player.sendMessage(message);
        player.sendActionBar(message);
    }

    private String formatMaterials(List<Material> materials) {
        if (materials == null || materials.isEmpty()) {
            return "material";
        }
        List<String> names = new ArrayList<>();
        for (Material material : materials) {
            names.add(formatEnum(material.name()));
        }
        return String.join("/", names);
    }

    private String formatEntity(EntityType type) {
        if (type == null) {
            return "entity";
        }
        return formatEnum(type.name());
    }

    private String formatLocation(ObjectiveDefinition objective) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", objective.x(), objective.y(), objective.z());
    }

    private String formatEnum(String raw) {
        return raw.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public QuestDefinition getQuestDefinition(String questId) {
        if (questId == null || questId.isBlank()) {
            return null;
        }
        return repository.get(questId);
    }

    public ConditionCheckResult getObjectiveConditionResult(Player player, QuestDefinition quest,
                                                            ObjectiveDefinition objective) {
        if (player == null || quest == null || objective == null) {
            return ConditionCheckResult.deny(null);
        }
        ConditionCheckResult questResult = checkConditions(player, player.getLocation(), quest.conditions());
        if (!questResult.allowed()) {
            return questResult;
        }
        return checkConditions(player, player.getLocation(), objective.conditions());
    }
}
