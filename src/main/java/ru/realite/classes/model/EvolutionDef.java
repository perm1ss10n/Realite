package ru.realite.classes.model;

import java.util.List;

public class EvolutionDef {
    public final String id;
    public final String title;

    public final int requiredLevel;

    public final double costMoney;
    public final List<ItemAmount> costItems;

    public final double rewardMoney;
    public final List<ItemAmount> rewardItems;

    public EvolutionDef(String id, String title,
                        int requiredLevel,
                        double costMoney, List<ItemAmount> costItems,
                        double rewardMoney, List<ItemAmount> rewardItems) {
        this.id = id;
        this.title = title;
        this.requiredLevel = requiredLevel;
        this.costMoney = costMoney;
        this.costItems = costItems;
        this.rewardMoney = rewardMoney;
        this.rewardItems = rewardItems;
    }
}
