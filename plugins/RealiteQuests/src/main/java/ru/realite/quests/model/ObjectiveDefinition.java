package ru.realite.quests.model;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class ObjectiveDefinition {

    private final String id;
    private final ObjectiveType type;
    private final String npcId;
    private final EntityType entityType;
    private final List<Material> materials;
    private final int amount;
    private final String world;
    private final double x;
    private final double y;
    private final double z;
    private final double radius;

    public ObjectiveDefinition(String id,
                               ObjectiveType type,
                               String npcId,
                               EntityType entityType,
                               List<Material> materials,
                               int amount,
                               String world,
                               double x,
                               double y,
                               double z,
                               double radius) {
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.npcId = npcId;
        this.entityType = entityType;
        this.materials = materials == null ? null : List.copyOf(materials);
        this.amount = amount;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
    }

    public String id() {
        return id;
    }

    public ObjectiveType type() {
        return type;
    }

    public String npcId() {
        return npcId;
    }

    public EntityType entityType() {
        return entityType;
    }

    public List<Material> materials() {
        return materials == null ? Collections.emptyList() : materials;
    }

    public int amount() {
        return amount;
    }

    public String world() {
        return world;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public double radius() {
        return radius;
    }
}
