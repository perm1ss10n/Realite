package ru.realite.familiars.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public record FamiliarInstance(
        UUID owner,
        String typeId,
        int level,
        int xp,
        FamiliarState state,
        Optional<UUID> summonedEntityId,
        List<ItemStack> inventory
) {
    public FamiliarInstance {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(typeId, "typeId");
        Objects.requireNonNull(state, "state");
        summonedEntityId = summonedEntityId == null ? Optional.empty() : summonedEntityId;
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
    }
}
