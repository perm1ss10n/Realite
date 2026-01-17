package ru.realite.familiars.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.entity.Tameable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassTagProvider;
import ru.realite.familiars.config.FamiliarTypeRepository;
import ru.realite.familiars.config.TamingRules;
import ru.realite.familiars.config.TamingRulesRepository;
import ru.realite.familiars.model.FamiliarBehavior;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarState;
import ru.realite.familiars.model.FamiliarType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class FamiliarServiceImpl implements FamiliarService {

    private final CoreApi core;
    private final Plugin plugin;
    private final FamiliarStore store;
    private final FamiliarRepository repository;
    private final Logger logger;
    private FamiliarTypeRepository typeRepository;
    private TamingRulesRepository rulesRepository;
    private final Map<UUID, Instant> lastTame = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastSummon = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, FamiliarBehavior>> behaviors = new ConcurrentHashMap<>();
    private final NamespacedKey ownerKey = new NamespacedKey("realite", "familiar_owner");
    private final NamespacedKey typeKey = new NamespacedKey("realite", "familiar_type");
    private final double followDistance = 3.5;
    private final double teleportDistance = 12.0;

    public FamiliarServiceImpl(CoreApi core, Plugin plugin, FamiliarStore store, FamiliarRepository repository,
            Logger logger) {
        this.core = core;
        this.plugin = plugin;
        this.store = store;
        this.repository = repository;
        this.logger = logger;
        startFollowTask();
    }

    public void updateRepositories(FamiliarTypeRepository typeRepository, TamingRulesRepository rulesRepository) {
        this.typeRepository = typeRepository;
        this.rulesRepository = rulesRepository;
    }

    @Override
    public CheckResult canTame(Player player, String typeId) {
        List<String> reasons = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        FamiliarType type = getType(typeId, reasons);
        TamingRules rules = getRules(reasons);

        if (store.countActive(player.getUniqueId()) > 0) {
            reasons.add("Player already has a familiar");
        }

        if (type != null) {
            checkAllowedClasses(player, type, reasons, notes);
        }

        if (rules != null) {
            int active = store.countActive(player.getUniqueId());
            if (active >= rules.maxActive()) {
                reasons.add("Limit reached: max-active=" + rules.maxActive());
            }
            Instant last = lastTame.get(player.getUniqueId());
            if (last != null && last.plus(rules.tameCooldown()).isAfter(Instant.now())) {
                reasons.add("Tame cooldown not finished");
            }
        }

        if (!reasons.isEmpty()) {
            return CheckResult.denied(reasons);
        }
        return CheckResult.allowed(notes);
    }

    @Override
    public CheckResult canSummon(Player player, String typeId) {
        List<String> reasons = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        getType(typeId, reasons);
        TamingRules rules = getRules(reasons);

        if (rules != null) {
            int summoned = store.countSummoned(player.getUniqueId());
            if (summoned >= rules.maxSummoned()) {
                reasons.add("Limit reached: max-summoned=" + rules.maxSummoned());
            }
            Instant last = lastSummon.get(player.getUniqueId());
            if (last != null && last.plus(rules.summonCooldown()).isAfter(Instant.now())) {
                reasons.add("Summon cooldown not finished");
            }
        }

        if (!reasons.isEmpty()) {
            return CheckResult.denied(reasons);
        }
        return CheckResult.allowed(notes);
    }

    @Override
    public TameResult tame(Player player, String typeId) {
        CheckResult check = canTame(player, typeId);
        if (!check.allowed()) {
            return new TameResult(check, null);
        }
        FamiliarInstance instance = new FamiliarInstance(
                player.getUniqueId(),
                typeId,
                1,
                0,
                FamiliarState.IDLE,
                Optional.empty());
        store.upsert(instance);
        lastTame.put(player.getUniqueId(), Instant.now());
        save();
        return new TameResult(check, instance);
    }

    @Override
    public List<FamiliarInstance> getFamiliars(UUID owner) {
        return Collections.unmodifiableList(store.getInstances(owner));
    }

    @Override
    public Optional<FamiliarType> getType(String typeId) {
        if (typeRepository == null || typeId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(typeRepository.get(typeId));
    }

    @Override
    public CheckResult summon(Player player, String typeId) {
        List<String> reasons = new ArrayList<>();

        FamiliarType type = getType(typeId, reasons);
        if (!reasons.isEmpty()) {
            return CheckResult.denied(reasons);
        }

        FamiliarInstance instance = getInstance(player.getUniqueId(), typeId);
        if (instance == null) {
            return CheckResult.denied(List.of("Familiar not tamed: " + typeId));
        }

        if (instance.state() == FamiliarState.SUMMONED) {
            if (instance.summonedEntityId().isPresent()
                    && Bukkit.getEntity(instance.summonedEntityId().get()) != null) {
                return CheckResult.denied(List.of("Familiar already summoned: " + typeId));
            }

            // сущность пропала — сбрасываем состояние и продолжаем призыв
            instance = new FamiliarInstance(
                    instance.owner(),
                    instance.typeId(),
                    instance.level(),
                    instance.xp(),
                    FamiliarState.IDLE,
                    Optional.empty());
            store.upsert(instance);
            save();
        }

        CheckResult rulesCheck = canSummon(player, typeId);
        if (!rulesCheck.allowed()) {
            return CheckResult.denied(rulesCheck.reasons());
        }

        Entity spawned = spawnEntity(player, type);
        if (spawned == null) {
            return CheckResult.denied(List.of("Failed to spawn familiar entity"));
        }

        FamiliarInstance summoned = new FamiliarInstance(
                player.getUniqueId(),
                typeId,
                instance.level(),
                instance.xp(),
                FamiliarState.SUMMONED,
                Optional.of(spawned.getUniqueId()));

        store.upsert(summoned);
        registerBehavior(player.getUniqueId(), typeId, FamiliarBehavior.FOLLOW, spawned);
        lastSummon.put(player.getUniqueId(), Instant.now());
        save();

        return CheckResult.allowed(List.of());
    }

    @Override
    public CheckResult dismiss(Player player, String typeId) {
        FamiliarInstance instance = getInstance(player.getUniqueId(), typeId);
        if (instance == null) {
            return CheckResult.denied(List.of("Familiar not tamed: " + typeId));
        }
        if (instance.state() != FamiliarState.SUMMONED) {
            return CheckResult.denied(List.of("Familiar not summoned: " + typeId));
        }
        instance.summonedEntityId()
                .map(Bukkit::getEntity)
                .ifPresent(Entity::remove);
        FamiliarInstance updated = new FamiliarInstance(
                instance.owner(),
                instance.typeId(),
                instance.level(),
                instance.xp(),
                FamiliarState.IDLE,
                Optional.empty());
        store.upsert(updated);
        removeBehavior(instance.owner(), instance.typeId());
        save();
        return CheckResult.allowed(List.of());
    }

    @Override
    public CheckResult setBehavior(Player player, String typeId, FamiliarBehavior behavior) {
        FamiliarInstance instance = getInstance(player.getUniqueId(), typeId);
        if (instance == null) {
            return CheckResult.denied(List.of("Familiar not tamed: " + typeId));
        }
        if (instance.state() != FamiliarState.SUMMONED) {
            return CheckResult.denied(List.of("Familiar not summoned: " + typeId));
        }
        Entity entity = instance.summonedEntityId().map(Bukkit::getEntity).orElse(null);
        if (entity == null) {
            return CheckResult.denied(List.of("Familiar entity missing: " + typeId));
        }
        registerBehavior(instance.owner(), instance.typeId(), behavior, entity);
        return CheckResult.allowed(List.of());
    }

    @Override
    public void handleLogout(UUID owner) {
        List<FamiliarInstance> instances = List.copyOf(store.getInstances(owner));
        for (FamiliarInstance instance : instances) {
            if (instance.state() != FamiliarState.SUMMONED) {
                continue;
            }
            instance.summonedEntityId()
                    .map(Bukkit::getEntity)
                    .ifPresent(Entity::remove);
            FamiliarInstance updated = new FamiliarInstance(
                    instance.owner(),
                    instance.typeId(),
                    instance.level(),
                    instance.xp(),
                    FamiliarState.IDLE,
                    Optional.empty());
            store.upsert(updated);
            removeBehavior(instance.owner(), instance.typeId());
        }
        save();
    }

    @Override
    public void handleFamiliarDeath(UUID owner, String typeId) {
        FamiliarInstance instance = getInstance(owner, typeId);
        if (instance == null) {
            return;
        }
        FamiliarInstance updated = new FamiliarInstance(
                instance.owner(),
                instance.typeId(),
                instance.level(),
                instance.xp(),
                FamiliarState.IDLE,
                Optional.empty());
        store.upsert(updated);
        removeBehavior(instance.owner(), instance.typeId());
        save();
    }

    @Override
    public boolean isFamiliarEntity(Entity entity) {
        return getFamiliarEntityData(entity).isPresent();
    }

    @Override
    public Optional<FamiliarEntityData> getFamiliarEntityData(Entity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        String ownerRaw = container.get(ownerKey, PersistentDataType.STRING);
        String typeId = container.get(typeKey, PersistentDataType.STRING);
        if (ownerRaw == null || typeId == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(new FamiliarEntityData(UUID.fromString(ownerRaw), typeId));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public void shutdown() {
        save();
        store.clear();
        lastTame.clear();
        lastSummon.clear();
        behaviors.clear();
    }

    public void resetSummonedStates() {
        for (Map.Entry<UUID, List<FamiliarInstance>> entry : store.snapshot().entrySet()) {
            for (FamiliarInstance instance : entry.getValue()) {
                if (instance.state() != FamiliarState.SUMMONED) {
                    continue;
                }
                FamiliarInstance updated = new FamiliarInstance(
                        instance.owner(),
                        instance.typeId(),
                        instance.level(),
                        instance.xp(),
                        FamiliarState.IDLE,
                        Optional.empty());
                store.upsert(updated);
            }
        }
        save();
    }

    private FamiliarType getType(String typeId, List<String> reasons) {
        if (typeRepository == null) {
            reasons.add("Familiars config not loaded");
            return null;
        }
        FamiliarType type = typeRepository.get(typeId);
        if (type == null) {
            reasons.add("Unknown familiar type: " + typeId);
        }
        return type;
    }

    private TamingRules getRules(List<String> reasons) {
        if (rulesRepository == null) {
            reasons.add("Taming rules not loaded");
            return null;
        }
        return rulesRepository.rules();
    }

    private FamiliarInstance getInstance(UUID owner, String typeId) {
        for (FamiliarInstance instance : store.getInstances(owner)) {
            if (instance.typeId().equalsIgnoreCase(typeId)) {
                return instance;
            }
        }
        return null;
    }

    private void checkAllowedClasses(Player player, FamiliarType type, List<String> reasons, List<String> notes) {
        if (type.allowedClasses().isEmpty()) {
            return;
        }
        ClassTagProvider provider = core.services().get(ClassTagProvider.class);
        if (provider == null) {
            notes.add("Class provider missing; skipping allowedClasses check");
            return;
        }
        String className = provider.getTag(player).displayName();
        boolean allowed = type.allowedClasses().stream()
                .anyMatch(entry -> entry.equalsIgnoreCase(className));
        if (!allowed) {
            reasons.add("Class '" + className + "' not allowed");
        }
    }

    private void save() {
        if (repository == null) {
            return;
        }
        repository.save(store.snapshot());
        if (logger != null) {
            logger.fine("Saved familiars store.");
        }
    }

    private void registerBehavior(UUID owner, String typeId, FamiliarBehavior behavior, Entity entity) {
        behaviors.computeIfAbsent(owner, key -> new ConcurrentHashMap<>()).put(typeId, behavior);
        applyBehavior(entity, behavior, owner, typeId);
    }

    private void removeBehavior(UUID owner, String typeId) {
        Map<String, FamiliarBehavior> ownerBehaviors = behaviors.get(owner);
        if (ownerBehaviors == null) {
            return;
        }
        ownerBehaviors.remove(typeId);
        if (ownerBehaviors.isEmpty()) {
            behaviors.remove(owner);
        }
    }

    private Entity spawnEntity(Player player, FamiliarType type) {
        if (player == null || type == null) {
            return null;
        }
        EntityType entityType = resolveEntityType(type);
        Location location = player.getLocation();
        Entity entity = player.getWorld().spawnEntity(location, entityType);
        if (entity instanceof LivingEntity living) {
            living.setRemoveWhenFarAway(false);
            living.setPersistent(true);
            living.setCollidable(false);
            living.setAI(true);
        }
        if (entity instanceof Tameable tameable) {
            tameable.setOwner(player);
        }
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        container.set(typeKey, PersistentDataType.STRING, type.id());
        return entity;
    }

    private EntityType resolveEntityType(FamiliarType type) {
        String id = type.id().toLowerCase();
        return switch (id) {
            case "wolf" -> EntityType.WOLF;
            case "fairy" -> EntityType.ALLAY;
            default -> EntityType.WOLF;
        };
    }

    private void applyBehavior(Entity entity, FamiliarBehavior behavior, UUID owner, String typeId) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }

        boolean stay = behavior == FamiliarBehavior.STAY;

        // AI выключаем при STAY
        living.setAI(!stay);

        // Садим ТОЛЬКО если моб это поддерживает
        if (entity instanceof Sittable sittable) {
            sittable.setSitting(stay);
        }

        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(ownerKey, PersistentDataType.STRING, owner.toString());
        container.set(typeKey, PersistentDataType.STRING, typeId);

        living.setCollidable(false);
    }

    private void startFollowTask() {
        if (plugin == null) {
            return;
        }
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickFollow, 20L, 20L);
    }

    private void tickFollow() {
        for (Map.Entry<UUID, Map<String, FamiliarBehavior>> ownerEntry : behaviors.entrySet()) {
            Player owner = Bukkit.getPlayer(ownerEntry.getKey());
            if (owner == null || !owner.isOnline()) {
                handleLogout(ownerEntry.getKey());
                continue;
            }
            for (Map.Entry<String, FamiliarBehavior> behaviorEntry : ownerEntry.getValue().entrySet()) {
                FamiliarInstance instance = getInstance(owner.getUniqueId(), behaviorEntry.getKey());
                if (instance == null || instance.state() != FamiliarState.SUMMONED) {
                    continue;
                }
                Entity entity = instance.summonedEntityId().map(Bukkit::getEntity).orElse(null);
                if (entity == null) {
                    FamiliarInstance updated = new FamiliarInstance(
                            instance.owner(),
                            instance.typeId(),
                            instance.level(),
                            instance.xp(),
                            FamiliarState.IDLE,
                            Optional.empty());
                    store.upsert(updated);
                    removeBehavior(instance.owner(), instance.typeId());
                    save();
                    continue;
                }
                FamiliarBehavior behavior = behaviorEntry.getValue();
                applyBehavior(entity, behavior, owner.getUniqueId(), instance.typeId());
                if (behavior == FamiliarBehavior.STAY) {
                    continue;
                }
                followOwner(entity, owner);
            }
        }
    }

    private void followOwner(Entity entity, Player owner) {
        Location ownerLocation = owner.getLocation();
        Location petLocation = entity.getLocation();
        double distanceSquared = petLocation.distanceSquared(ownerLocation);
        double teleportSquared = teleportDistance * teleportDistance;
        double followSquared = followDistance * followDistance;
        if (distanceSquared > teleportSquared) {
            entity.teleport(ownerLocation);
            return;
        }
        if (distanceSquared > followSquared) {
            Vector direction = ownerLocation.toVector().subtract(petLocation.toVector());
            if (direction.lengthSquared() > 0.01) {
                Vector velocity = direction.normalize().multiply(0.35);
                entity.setVelocity(velocity);
            }
        }
    }
}
