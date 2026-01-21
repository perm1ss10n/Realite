package ru.realite.core.boss.impl;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.items.ItemService;
import ru.realite.core.api.models.ModelContext;
import ru.realite.core.api.models.ModelsBridge;
import ru.realite.core.boss.api.BossAbility;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.core.AbstractRealiteBoss;
import ru.realite.core.boss.core.BossAbilityRegistry;
import ru.realite.core.boss.core.context.DeathContext;
import ru.realite.core.boss.core.context.SpawnContext;
import ru.realite.core.boss.data.BossDefinition;
import ru.realite.core.boss.data.BossEquipmentDefinition;
import ru.realite.core.boss.data.BossGuaranteedDrop;
import ru.realite.core.boss.data.BossLootDefinition;
import ru.realite.core.boss.data.BossLootEntry;
import ru.realite.core.boss.data.BossPhaseDefinition;
import ru.realite.core.boss.data.BossStatsDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class BossFirst extends AbstractRealiteBoss {
    public static final String ID = "boss_first";

    private final BossDefinition definition;

    public BossFirst(BossDefinition definition, BossAbilityRegistry abilityRegistry) {
        super(
                definition.id(),
                definition.stats().maxHp(),
                toPhases(definition.phases()),
                toAbilities(definition.abilityIds(), abilityRegistry));
        this.definition = definition;
    }

    @Override
    protected LivingEntity spawnEntity(SpawnContext ctx) {
        Location location = ctx.location();
        World world = location.getWorld();
        if (world == null) {
            throw new IllegalStateException("Spawn location has no world");
        }

        Entity spawned = world.spawnEntity(location, definition.entityType());
        if (!(spawned instanceof LivingEntity living)) {
            spawned.remove();
            throw new IllegalStateException("Configured entityType is not a LivingEntity: " + definition.entityType());
        }

        String title = Optional.ofNullable(definition.name()).orElse(definition.id());
        living.customName(Component.text(title));
        living.setCustomNameVisible(true);

        return living;
    }

    @Override
    protected void onSpawned(SpawnContext ctx) {
        applyStats(definition.stats());
        applyEquipment(definition.equipment());
        applyModel(definition.modelId());
    }

    @Override
    public void onDeath(DeathContext ctx) {
        dropLoot(ctx);
    }

    private void applyStats(BossStatsDefinition stats) {
        LivingEntity entity = getEntity();
        if (entity == null || stats == null) {
            return;
        }
        AttributeInstance damage = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damage != null && stats.baseDamage() > 0.0) {
            damage.setBaseValue(stats.baseDamage());
        }
        AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null && stats.movementSpeed() > 0.0) {
            speed.setBaseValue(stats.movementSpeed());
        }
        AttributeInstance knockback = entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockback != null) {
            knockback.setBaseValue(Math.max(knockback.getBaseValue(), 0.7));
        }
    }

    private void applyEquipment(BossEquipmentDefinition equipment) {
        if (equipment == null || equipment.isEmpty()) {
            return;
        }
        ItemService itemService = Bukkit.getServicesManager().load(ItemService.class);
        if (itemService == null) {
            return;
        }
        LivingEntity entity = getEntity();
        if (entity == null) {
            return;
        }
        EntityEquipment entityEquipment = entity.getEquipment();
        if (entityEquipment == null) {
            return;
        }

        setEquipmentItem(entityEquipment, equipment.mainHand(), itemService, Slot.MAIN_HAND);
        setEquipmentItem(entityEquipment, equipment.offHand(), itemService, Slot.OFF_HAND);
        setEquipmentItem(entityEquipment, equipment.helmet(), itemService, Slot.HELMET);
        setEquipmentItem(entityEquipment, equipment.chestplate(), itemService, Slot.CHESTPLATE);
        setEquipmentItem(entityEquipment, equipment.leggings(), itemService, Slot.LEGGINGS);
        setEquipmentItem(entityEquipment, equipment.boots(), itemService, Slot.BOOTS);
    }

    private void setEquipmentItem(EntityEquipment equipment, String itemId, ItemService itemService, Slot slot) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        ItemStack item;
        try {
            item = itemService.create(itemId, 1);
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().warning("[Boss] Failed to create item " + itemId + " for " + definition.id());
            return;
        }
        switch (slot) {
            case MAIN_HAND -> equipment.setItemInMainHand(item);
            case OFF_HAND -> equipment.setItemInOffHand(item);
            case HELMET -> equipment.setHelmet(item);
            case CHESTPLATE -> equipment.setChestplate(item);
            case LEGGINGS -> equipment.setLeggings(item);
            case BOOTS -> equipment.setBoots(item);
        }
    }

    private void applyModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return;
        }
        LivingEntity entity = getEntity();
        if (entity == null) {
            return;
        }
        CoreApi core = Bukkit.getServicesManager().load(CoreApi.class);
        if (core == null) {
            return;
        }
        ModelsBridge bridge = core.services().get(ModelsBridge.class);
        if (bridge == null) {
            return;
        }
        bridge.apply(entity, modelId, new ModelContext("boss", java.util.Map.of("bossId", definition.id())));
    }

    private void dropLoot(DeathContext ctx) {
        BossLootDefinition loot = definition.loot();
        if (loot == null) {
            return;
        }
        ItemService itemService = Bukkit.getServicesManager().load(ItemService.class);
        List<ItemStack> drops = ctx.event().getDrops();

        for (BossGuaranteedDrop drop : loot.guaranteed()) {
            ItemStack stack = createDrop(itemService, drop.itemId(), drop.amount());
            if (stack != null) {
                drops.add(stack);
            }
        }

        if (loot.rolls() > 0 && !loot.table().isEmpty()) {
            for (int roll = 0; roll < loot.rolls(); roll++) {
                BossLootEntry entry = rollEntry(loot.table());
                if (entry == null) {
                    continue;
                }
                int amount = ThreadLocalRandom.current().nextInt(entry.min(), entry.max() + 1);
                ItemStack stack = createDrop(itemService, entry.itemId(), amount);
                if (stack != null) {
                    drops.add(stack);
                }
            }
        }
    }

    private ItemStack createDrop(ItemService itemService, String itemId, int amount) {
        if (itemService != null) {
            try {
                return itemService.create(itemId, amount);
            } catch (IllegalArgumentException e) {
                Bukkit.getLogger().warning("[Boss] Unknown drop item " + itemId + " for " + definition.id());
            }
        }
        Material fallback = fallbackMaterial(itemId);
        return fallback == null ? null : new ItemStack(fallback, Math.max(1, amount));
    }

    private Material fallbackMaterial(String itemId) {
        if (itemId == null) {
            return null;
        }
        if ("realite:boss_trophy_t1".equalsIgnoreCase(itemId)) {
            return Material.NETHER_STAR;
        }
        return Material.IRON_INGOT;
    }

    private BossLootEntry rollEntry(List<BossLootEntry> table) {
        int totalWeight = 0;
        for (BossLootEntry entry : table) {
            totalWeight += entry.weight();
        }
        if (totalWeight <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight) + 1;
        int current = 0;
        for (BossLootEntry entry : table) {
            current += entry.weight();
            if (roll <= current) {
                return entry;
            }
        }
        return null;
    }

    private static List<BossPhase> toPhases(List<BossPhaseDefinition> phases) {
        return phases.stream()
                .map(phase -> new BossPhase(phase.id(), phase.enterAt()))
                .toList();
    }

    private static List<BossAbility> toAbilities(List<String> abilityIds, BossAbilityRegistry registry) {
        List<BossAbility> abilities = new ArrayList<>();
        for (String abilityId : abilityIds) {
            abilities.add(registry.create(abilityId));
        }
        return abilities;
    }

    private enum Slot {
        MAIN_HAND,
        OFF_HAND,
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }
}
