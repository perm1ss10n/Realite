package ru.realite.magic.spell;

import java.util.List;

public record ReagentCost(boolean consumeOnCast, List<ReagentItem> items) {

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }
}
