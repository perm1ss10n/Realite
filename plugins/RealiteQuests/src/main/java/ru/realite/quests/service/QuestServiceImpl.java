package ru.realite.quests.service;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.realite.core.api.Platform;
import ru.realite.core.api.quests.QuestProgress;
import ru.realite.core.api.quests.QuestService;
import ru.realite.core.api.quests.QuestState;
import ru.realite.quests.model.ObjectiveDefinition;
import ru.realite.quests.model.ObjectiveType;
import ru.realite.quests.model.QuestDefinition;
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
    private final QuestRepository repository;
    private final QuestProgressRepository progressRepository;

    public QuestServiceImpl(Platform logger, QuestRepository repository, QuestProgressRepository progressRepository) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.progressRepository = Objects.requireNonNull(progressRepository, "progressRepository");
    }

    @Override
    public void start(Player player, String questId) {
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
        if (progress != null && progress.state() == QuestState.COMPLETED) {
            return;
        }
        QuestProgressData fresh = new QuestProgressData(QuestState.ACTIVE, Set.of(), Map.of());
        progressRepository.save(player.getUniqueId(), quest.id(), fresh);
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
        for (QuestDefinition quest : repository.all()) {
            if (isActive(player, quest.id())) {
                result.add(quest.id());
            }
        }
        return result;
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

    public void handleKill(Player player, EntityType entityType) {
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

    public void handleBlockPlace(Player player, Material material) {
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

    public void handleBlockBreak(Player player, Material material) {
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
        progressRepository.save(player.getUniqueId(), quest.id(), progress);
        grantRewards(player, quest);
        notifyQuestCompleted(player, quest);
        logger.info("[Quests] Quest completed: " + quest.id() + " for " + player.getName());
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
}
