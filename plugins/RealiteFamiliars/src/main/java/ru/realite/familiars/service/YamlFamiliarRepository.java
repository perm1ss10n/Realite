package ru.realite.familiars.service;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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
        return new FamiliarInstance(owner, typeId, level, xp, state, summoned);
    }
}
