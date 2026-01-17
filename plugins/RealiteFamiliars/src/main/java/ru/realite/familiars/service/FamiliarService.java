package ru.realite.familiars.service;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import ru.realite.familiars.model.FamiliarBehavior;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamiliarService {
    CheckResult canTame(Player player, String typeId);

    default CheckResult canTame(Player player, String typeId, EntityType entityType) {
        return canTame(player, typeId);
    }

    CheckResult canSummon(Player player, String typeId);

    TameResult tame(Player player, String typeId);

    default TameResult tame(Player player, String typeId, EntityType entityType) {
        return tame(player, typeId);
    }

    List<FamiliarInstance> getFamiliars(UUID owner);

    Optional<FamiliarType> getType(String typeId);

    Optional<FamiliarInstance> getSummoned(UUID owner);

    FamiliarLimitInfo getLimitInfo(Player player);

    void addExperience(UUID owner, String typeId, int amount, FamiliarXpSource source);

    CheckResult summon(Player player, String typeId);

    CheckResult dismiss(Player player, String typeId);

    CheckResult setBehavior(Player player, String typeId, FamiliarBehavior behavior);

    void handleLogout(UUID owner);

    void handleFamiliarDeath(UUID owner, String typeId);

    boolean isFamiliarEntity(Entity entity);

    Optional<FamiliarEntityData> getFamiliarEntityData(Entity entity);

    void recordOwnerCombat(UUID owner);
}
