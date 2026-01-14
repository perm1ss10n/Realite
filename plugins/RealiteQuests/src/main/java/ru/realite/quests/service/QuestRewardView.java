package ru.realite.quests.service;

import org.bukkit.Material;
import ru.realite.quests.model.RewardType;

public record QuestRewardView(
        RewardType type,
        int amount,
        Material material,
        String unlockId
) {
}
