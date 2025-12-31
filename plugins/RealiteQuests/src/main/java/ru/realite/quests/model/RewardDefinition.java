package ru.realite.quests.model;

import org.bukkit.Material;

import java.util.Objects;

public final class RewardDefinition {

    private final RewardType type;
    private final int amount;
    private final Material material;
    private final String unlockId;

    public RewardDefinition(RewardType type, int amount, Material material, String unlockId) {
        this.type = Objects.requireNonNull(type, "type");
        this.amount = amount;
        this.material = material;
        this.unlockId = unlockId;
    }

    public RewardType type() {
        return type;
    }

    public int amount() {
        return amount;
    }

    public Material material() {
        return material;
    }

    public String unlockId() {
        return unlockId;
    }
}
