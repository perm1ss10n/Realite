package ru.realite.quests.service;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import ru.realite.core.api.Platform;
import ru.realite.quests.model.ObjectiveDefinition;
import ru.realite.quests.model.ObjectiveType;
import ru.realite.quests.model.QuestDefinition;
import ru.realite.quests.model.QuestType;
import ru.realite.quests.model.RewardDefinition;
import ru.realite.quests.model.RewardType;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class QuestLoader {

    private final File questsDir;
    private final Platform logger;

    public QuestLoader(Path questsDir, Platform logger) {
        this.questsDir = questsDir.toFile();
        this.logger = logger;
        if (!this.questsDir.exists()) {
            // noinspection ResultOfMethodCallIgnored
            this.questsDir.mkdirs();
        }
    }

    public QuestRepository load() {
        Map<String, QuestDefinition> quests = new HashMap<>();
        File[] files = questsDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return new QuestRepository(quests);
        }
        for (File file : files) {
            QuestDefinition definition = loadFile(file);
            if (definition == null) {
                continue;
            }
            String key = normalize(definition.id());
            if (quests.containsKey(key)) {
                logger.warn("[Quests] Duplicate quest id " + definition.id() + " in " + file.getName());
                continue;
            }
            quests.put(key, definition);
        }
        logger.info("[Quests] Loaded quests: " + quests.size());
        return new QuestRepository(quests);
    }

    private QuestDefinition loadFile(File file) {
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        String id = yml.getString("id");
        if (id == null || id.isBlank()) {
            logger.warn("[Quests] Quest " + file.getName() + " missing id");
            return null;
        }

        List<ObjectiveDefinition> objectives = loadObjectives(file.getName(), yml);
        if (objectives.isEmpty()) {
            logger.warn("[Quests] Quest " + id + " has no valid objectives");
            return null;
        }

        List<RewardDefinition> rewards = loadRewards(file.getName(), yml);
        RewardDefinition unlockReward = loadGrantQuestUnlock(file.getName(), yml);
        if (unlockReward != null) {
            rewards.add(unlockReward);
        }

        // FIX: читать из yml, а не из config
        QuestType type = parseType(yml.getString("type", null));

        return new QuestDefinition(id.trim(), type, objectives, rewards);
    }

    private QuestType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return QuestType.STANDARD;
        }
        try {
            return QuestType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return QuestType.STANDARD;
        }
    }

    private List<ObjectiveDefinition> loadObjectives(String fileName, YamlConfiguration yml) {
        List<Map<?, ?>> entries = yml.getMapList("objectives");
        List<ObjectiveDefinition> objectives = new ArrayList<>();
        int index = 1;
        for (Map<?, ?> map : entries) {
            String typeRaw = asString(map.get("type"));
            ObjectiveType type = parseObjectiveType(typeRaw);
            if (type == null) {
                logger.warn("[Quests] Invalid objective type in " + fileName + ": " + typeRaw);
                index++;
                continue;
            }
            String id = asString(map.get("id"));
            if (id == null || id.isBlank()) {
                id = type.name().toLowerCase(Locale.ROOT) + "_" + index;
            }
            ObjectiveDefinition definition = switch (type) {
                case INTERACT_NPC -> parseInteractNpc(fileName, id, map);
                case KILL -> parseKill(fileName, id, map);
                case GO_TO_LOCATION -> parseLocation(fileName, id, map);
                case PLACE_BLOCK -> parsePlaceBlock(fileName, id, map);
                case BREAK_BLOCK -> parseBreakBlock(fileName, id, map);
                case HOLD_ITEM -> parseHoldItem(fileName, id, map);
            };
            if (definition != null) {
                objectives.add(definition);
            }
            index++;
        }
        return objectives;
    }

    private ObjectiveDefinition parseInteractNpc(String fileName, String id, Map<?, ?> map) {
        String npcId = asString(map.get("npcId"));
        if (npcId == null || npcId.isBlank()) {
            logger.warn("[Quests] INTERACT_NPC objective missing npcId in " + fileName);
            return null;
        }
        return new ObjectiveDefinition(id, ObjectiveType.INTERACT_NPC, npcId.trim(), null, null, 1,
                null, 0, 0, 0, 0);
    }

    private ObjectiveDefinition parseKill(String fileName, String id, Map<?, ?> map) {
        String entityRaw = asString(map.get("entity"));
        if (entityRaw == null) {
            entityRaw = asString(map.get("entityType"));
        }
        EntityType type = parseEntityType(entityRaw);
        if (type == null) {
            logger.warn("[Quests] KILL objective missing entity type in " + fileName);
            return null;
        }
        int amount = asInt(map.get("amount"), 1);
        if (amount <= 0) {
            amount = 1;
        }
        return new ObjectiveDefinition(id, ObjectiveType.KILL, null, type, null, amount,
                null, 0, 0, 0, 0);
    }

    private ObjectiveDefinition parseLocation(String fileName, String id, Map<?, ?> map) {
        String world = asString(map.get("world"));
        Double x = asDouble(map.get("x"));
        Double y = asDouble(map.get("y"));
        Double z = asDouble(map.get("z"));
        if (world == null || world.isBlank() || x == null || y == null || z == null) {
            logger.warn("[Quests] GO_TO_LOCATION objective missing coordinates in " + fileName);
            return null;
        }
        double radius = asDouble(map.get("radius"), 2.0);
        if (radius <= 0) {
            radius = 2.0;
        }
        return new ObjectiveDefinition(id, ObjectiveType.GO_TO_LOCATION, null, null, null, 1,
                world.trim(), x, y, z, radius);
    }

    private ObjectiveDefinition parsePlaceBlock(String fileName, String id, Map<?, ?> map) {
        List<Material> materials = parseMaterials(map);
        if (materials.isEmpty()) {
            logger.warn("[Quests] PLACE_BLOCK objective missing material in " + fileName);
            return null;
        }
        int amount = asInt(map.get("amount"), 1);
        if (amount <= 0) {
            amount = 1;
        }
        return new ObjectiveDefinition(id, ObjectiveType.PLACE_BLOCK, null, null, materials, amount,
                null, 0, 0, 0, 0);
    }

    private ObjectiveDefinition parseBreakBlock(String fileName, String id, Map<?, ?> map) {
        List<Material> materials = parseMaterials(map);
        if (materials.isEmpty()) {
            logger.warn("[Quests] BREAK_BLOCK objective missing material in " + fileName);
            return null;
        }
        int amount = asInt(map.get("amount"), 1);
        if (amount <= 0) {
            amount = 1;
        }
        return new ObjectiveDefinition(id, ObjectiveType.BREAK_BLOCK, null, null, materials, amount,
                null, 0, 0, 0, 0);
    }

    private ObjectiveDefinition parseHoldItem(String fileName, String id, Map<?, ?> map) {
        List<Material> materials = parseMaterials(map);
        if (materials.isEmpty()) {
            logger.warn("[Quests] HOLD_ITEM objective missing material in " + fileName);
            return null;
        }
        int amount = asInt(map.get("amount"), 1);
        if (amount <= 0) {
            amount = 1;
        }
        return new ObjectiveDefinition(id, ObjectiveType.HOLD_ITEM, null, null, materials, amount,
                null, 0, 0, 0, 0);
    }

    private List<RewardDefinition> loadRewards(String fileName, YamlConfiguration yml) {
        List<Map<?, ?>> entries = yml.getMapList("rewards");
        List<RewardDefinition> rewards = new ArrayList<>();
        for (Map<?, ?> map : entries) {
            String typeRaw = asString(map.get("type"));
            RewardType type = parseRewardType(typeRaw);
            if (type == null) {
                logger.warn("[Quests] Invalid reward type in " + fileName + ": " + typeRaw);
                continue;
            }
            RewardDefinition definition = switch (type) {
                case XP -> parseXpReward(fileName, map);
                case ITEM -> parseItemReward(fileName, map);
                case QUEST_UNLOCK -> parseQuestUnlockReward(fileName, map);
            };
            if (definition != null) {
                rewards.add(definition);
            }
        }
        return rewards;
    }

    private RewardDefinition parseXpReward(String fileName, Map<?, ?> map) {
        int amount = asInt(map.get("amount"), 0);
        if (amount <= 0) {
            logger.warn("[Quests] XP reward missing amount in " + fileName);
            return null;
        }
        return new RewardDefinition(RewardType.XP, amount, null, null);
    }

    private RewardDefinition parseItemReward(String fileName, Map<?, ?> map) {
        String materialRaw = asString(map.get("material"));
        Material material = parseMaterial(materialRaw);
        if (material == null) {
            logger.warn("[Quests] ITEM reward missing material in " + fileName);
            return null;
        }
        int amount = asInt(map.get("amount"), 1);
        if (amount <= 0) {
            amount = 1;
        }
        return new RewardDefinition(RewardType.ITEM, amount, material, null);
    }

    private RewardDefinition parseQuestUnlockReward(String fileName, Map<?, ?> map) {
        String unlockId = asString(map.get("unlock"));
        if (unlockId == null) {
            unlockId = asString(map.get("questUnlock"));
        }
        if (unlockId == null) {
            unlockId = asString(map.get("id"));
        }
        if (unlockId == null || unlockId.isBlank()) {
            logger.warn("[Quests] QUEST_UNLOCK reward missing unlock id in " + fileName);
            return null;
        }
        return new RewardDefinition(RewardType.QUEST_UNLOCK, 0, null, unlockId.trim());
    }

    private ObjectiveType parseObjectiveType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ObjectiveType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RewardType parseRewardType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return RewardType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private Material parseMaterial(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RewardDefinition loadGrantQuestUnlock(String fileName, YamlConfiguration yml) {
        String unlockId = yml.getString("grantQuestUnlock");
        if (unlockId == null || unlockId.isBlank()) {
            return null;
        }
        return new RewardDefinition(RewardType.QUEST_UNLOCK, 0, null, unlockId.trim());
    }

    private List<Material> parseMaterials(Map<?, ?> map) {
        Object raw = map.get("materials");
        if (raw instanceof List<?> list) {
            List<Material> materials = new ArrayList<>();
            for (Object entry : list) {
                Material material = parseMaterial(asString(entry));
                if (material != null) {
                    materials.add(material);
                }
            }
            return materials;
        }
        Material single = parseMaterial(asString(map.get("material")));
        if (single != null) {
            return List.of(single);
        }
        return List.of();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private int asInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }
        return fallback;
    }

    private Double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private double asDouble(Object value, double fallback) {
        Double parsed = asDouble(value);
        return parsed == null ? fallback : parsed;
    }

    private String normalize(String questId) {
        return questId.trim().toLowerCase(Locale.ROOT);
    }
}
