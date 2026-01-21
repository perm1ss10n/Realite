package ru.realite.core.boss.data;

import java.util.List;

public record BossLootDefinition(
        List<BossGuaranteedDrop> guaranteed,
        List<BossLootEntry> table,
        int rolls
) {
    public BossLootDefinition {
        guaranteed = guaranteed == null ? List.of() : List.copyOf(guaranteed);
        table = table == null ? List.of() : List.copyOf(table);
        if (rolls < 0) {
            throw new IllegalArgumentException("rolls must be >= 0");
        }
    }

    public static BossLootDefinition empty() {
        return new BossLootDefinition(List.of(), List.of(), 0);
    }
}
