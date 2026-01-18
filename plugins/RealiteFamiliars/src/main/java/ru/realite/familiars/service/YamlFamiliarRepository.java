package ru.realite.familiars.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarState;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class YamlFamiliarRepository implements FamiliarRepository {

    private final JavaPlugin plugin;
    private final File file;

    public YamlFamiliarRepository(JavaPlugin plugin, File file) {
        this.plugin = plugin;
        this.file = file;
    }

    @Override
    public Map<UUID, List<FamiliarInstance>> load() {
        Map<UUID, List<FamiliarInstance>> result = new HashMap<>();
        if (!file.exists()) {
            return result;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection owners = config.getConfigurationSection("owners");
        if (owners == null) {
            return result;
        }

        for (String ownerId : owners.getKeys(false)) {
            UUID owner;
            try {
                owner = UUID.fromString(ownerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid owner UUID in familiars store: " + ownerId);
                continue;
            }

            ConfigurationSection ownerSection = owners.getConfigurationSection(ownerId);
            if (ownerSection == null) {
                continue;
            }

            List<FamiliarInstance> instances = new ArrayList<>();

            // legacy: owners.<uuid>.typeId + owners.<uuid>.(fields)
            String legacyTypeId = ownerSection.getString("typeId");
            if (legacyTypeId != null && !legacyTypeId.isBlank()) {
                instances.add(readInstance(ownerId, owner, legacyTypeId, ownerSection));
            } else {
                for (String typeId : ownerSection.getKeys(false)) {
                    ConfigurationSection instanceSection = ownerSection.getConfigurationSection(typeId);
                    if (instanceSection == null) {
                        continue;
                    }
                    instances.add(readInstance(ownerId, owner, typeId, instanceSection));
                }
            }

            if (!instances.isEmpty()) {
                result.put(owner, instances);
            }
        }

        return result;
    }

    @Override
    public void save(Map<UUID, List<FamiliarInstance>> data) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection owners = config.createSection("owners");

        if (data != null) {
            for (Map.Entry<UUID, List<FamiliarInstance>> entry : data.entrySet()) {
                String ownerId = entry.getKey().toString();
                ConfigurationSection ownerSection = owners.createSection(ownerId);

                for (FamiliarInstance instance : entry.getValue()) {
                    ConfigurationSection instanceSection = ownerSection.createSection(instance.typeId());
                    instanceSection.set("level", instance.level());
                    instanceSection.set("xp", instance.xp());
                    instanceSection.set("state", instance.state().name());
                    instanceSection.set("summonedEntityId", instance.summonedEntityId().map(UUID::toString).orElse(""));

                    // ✅ inventory as YAML list of maps
                    instanceSection.set("inventory", serializeInventory(instance.inventory()));
                }
            }
        }

        try {
            file.getParentFile().mkdirs();
            config.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to save familiars store: " + e.getMessage());
        }
    }

    private FamiliarInstance readInstance(String ownerId, UUID owner, String typeId, ConfigurationSection section) {
        int level = section.getInt("level", 1);
        int xp = section.getInt("xp", 0);

        String stateRaw = section.getString("state", FamiliarState.IDLE.name());
        FamiliarState state;
        try {
            state = FamiliarState.valueOf(stateRaw.toUpperCase());
        } catch (IllegalArgumentException e) {
            state = FamiliarState.IDLE;
        }

        String summonedRaw = section.getString("summonedEntityId", "");
        Optional<UUID> summoned = Optional.empty();
        if (summonedRaw != null && !summonedRaw.isBlank()) {
            try {
                summoned = Optional.of(UUID.fromString(summonedRaw));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid summoned entity UUID for " + ownerId + ":" + typeId);
            }
        }

        // ✅ read inventory maps
        List<ItemStack> inventory = deserializeInventory(section.getList("inventory"));

        return new FamiliarInstance(owner, typeId, level, xp, state, summoned, inventory);
    }

    /**
     * Store inventory as List<Map<String, Object>>.
     * Each item uses Bukkit's ConfigurationSerializable map from
     * ItemStack#serialize().
     */
    private List<Map<String, Object>> serializeInventory(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> out = new ArrayList<>(items.size());
        for (ItemStack stack : items) {
            if (stack == null) {
                out.add(null);
                continue;
            }
            try {
                out.add(stack.serialize());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to serialize familiar item: " + e.getMessage());
                out.add(null);
            }
        }
        return out;
    }

    /**
     * Read inventory from YAML list. Expected elements:
     * - null
     * - Map<String, Object> produced by ItemStack#serialize()
     */
    @SuppressWarnings("unchecked")
    private List<ItemStack> deserializeInventory(Object rawList) {
        if (!(rawList instanceof List<?> list) || list.isEmpty()) {
            return List.of();
        }

        List<ItemStack> items = new ArrayList<>(list.size());
        for (Object el : list) {
            if (el == null) {
                items.add(null);
                continue;
            }
            if (!(el instanceof Map<?, ?> mapAny)) {
                items.add(null);
                continue;
            }

            try {
                Map<String, Object> map = (Map<String, Object>) mapAny;
                items.add(ItemStack.deserialize(map));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deserialize familiar item: " + e.getMessage());
                items.add(null);
            }
        }
        return items;
    }
}
