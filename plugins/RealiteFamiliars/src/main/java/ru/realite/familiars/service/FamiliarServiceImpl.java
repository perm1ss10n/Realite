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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import ru.realite.core.api.familiars.FamiliarUiService;
import ru.realite.core.api.ui.UiInvalidateEvent;
import ru.realite.familiars.config.FamiliarLimitsRepository;
import ru.realite.familiars.config.FamiliarTypeRepository;
import ru.realite.familiars.config.TamePolicy;
import ru.realite.familiars.config.TamePolicyRepository;
import ru.realite.familiars.config.TamingRules;
import ru.realite.familiars.config.TamingRulesRepository;
import ru.realite.familiars.core.CoreAccess;
import ru.realite.familiars.event.FamiliarLeveledEvent;
import ru.realite.familiars.integration.classes.ClassesBridge;
import ru.realite.familiars.integration.limits.CityGuildBridge;
import ru.realite.familiars.integration.magic.MagicBridge;
import ru.realite.familiars.integration.quests.FamiliarQuestEvent;
import ru.realite.familiars.integration.quests.FamiliarQuestEventType;
import ru.realite.familiars.integration.quests.QuestsBridge;
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
import java.util.Locale;
import java.util.OptionalInt;
import java.util.logging.Logger;

public final class FamiliarServiceImpl implements FamiliarService {

    private final Plugin plugin;
    private final FamiliarStore store;
    private final FamiliarRepository repository;
    private final Logger logger;
    private final ClassesBridge classesBridge;
    private final QuestsBridge questsBridge;
    private final MagicBridge magicBridge;
    private final CityGuildBridge cityGuildBridge;
    private final FamiliarLimitService limitService;
    private FamiliarTypeRepository typeRepository;
    private TamingRulesRepository rulesRepository;
    private TamePolicyRepository policyRepository;
    private final Map<UUID, Instant> lastTame = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastSummon = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastCombat = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, FamiliarBehavior>> behaviors = new ConcurrentHashMap<>();
    private final NamespacedKey ownerKey = new NamespacedKey("realite", "familiar_owner");
    private final NamespacedKey typeKey = new NamespacedKey("realite", "familiar_type");
    private final double followDistance = 3.5;
    private final double teleportDistance = 12.0;
    private static final int XP_PER_LEVEL = 100;
    private static final int SUPPORT_BUFF_DURATION_TICKS = 60;
    private static final int UTILITY_BUFF_DURATION_TICKS = 60;
    private static final int COMBAT_BUFF_DURATION_TICKS = 60;
    private static final int OUT_OF_COMBAT_SECONDS = 10;

    public FamiliarServiceImpl(Plugin plugin, FamiliarStore store, FamiliarRepository repository,
            Logger logger,
            ClassesBridge classesBridge,
            QuestsBridge questsBridge,
            MagicBridge magicBridge,
            CityGuildBridge cityGuildBridge,
            FamiliarLimitService limitService) {
        this.plugin = plugin;
        this.store = store;
        this.repository = repository;
        this.logger = logger;
        this.classesBridge = classesBridge;
        this.questsBridge = questsBridge;
        this.magicBridge = magicBridge;
        this.cityGuildBridge = cityGuildBridge;
        this.limitService = limitService;
        startFollowTask();
    }

    public void updateRepositories(FamiliarTypeRepository typeRepository,
                                   TamingRulesRepository rulesRepository,
                                   FamiliarLimitsRepository limitsRepository,
                                   TamePolicyRepository policyRepository) {
        this.typeRepository = typeRepository;
        this.rulesRepository = rulesRepository;
        this.policyRepository = policyRepository;
        if (limitService != null) {
            limitService.updateRepository(limitsRepository);
        }
    }

    @Override
    public CheckResult canTame(Player player, String typeId) {
        return canTame(player, typeId, null);
    }

    @Override
    public CheckResult canTame(Player player, String typeId, EntityType entityType) {
        List<String> reasons = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        FamiliarType type = getType(typeId, reasons);
        TamingRules rules = getRules(reasons);

        FamiliarLimitInfo limitInfo = resolveLimit(player);
        if (store.countActive(player.getUniqueId()) >= limitInfo.limit()) {
            reasons.add("Limit reached: familiars=" + limitInfo.limit() + " (" + limitInfo.source() + ")");
        }

        if (type != null) {
            checkAllowedClasses(player, type, reasons, notes);
        }
        checkTamePolicy(player, typeId, entityType, reasons, notes);

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
        OptionalInt maxActive = cityGuildBridge.maxActive(player);
        if (maxActive.isPresent() && store.countActive(player.getUniqueId()) >= maxActive.getAsInt()) {
            reasons.add("Limit reached: city-guild-max-active=" + maxActive.getAsInt());
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
        OptionalInt maxSummoned = cityGuildBridge.maxSummoned(player);
        if (maxSummoned.isPresent() && store.countSummoned(player.getUniqueId()) >= maxSummoned.getAsInt()) {
            reasons.add("Limit reached: city-guild-max-summoned=" + maxSummoned.getAsInt());
        }

        if (!reasons.isEmpty()) {
            return CheckResult.denied(reasons);
        }
        return CheckResult.allowed(notes);
    }

    @Override
    public TameResult tame(Player player, String typeId) {
        return tame(player, typeId, null);
    }

    @Override
    public TameResult tame(Player player, String typeId, EntityType entityType) {
        CheckResult check = canTame(player, typeId, entityType);
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
        questsBridge.publish(new FamiliarQuestEvent(
                FamiliarQuestEventType.TAME,
                player.getUniqueId(),
                typeId,
                instance.level()));
        publishUiInvalidate(player);
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
    public Optional<FamiliarInstance> getSummoned(UUID owner) {
        for (FamiliarInstance instance : store.getInstances(owner)) {
            if (instance.state() == FamiliarState.SUMMONED) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

    @Override
    public FamiliarLimitInfo getLimitInfo(Player player) {
        return resolveLimit(player);
    }

    @Override
    public void addExperience(UUID owner, String typeId, int amount, FamiliarXpSource source) {
        if (owner == null || typeId == null || amount <= 0) {
            return;
        }
        FamiliarInstance instance = getInstance(owner, typeId);
        if (instance == null) {
            return;
        }
        int xp = instance.xp() + amount;
        int level = instance.level();
        Player player = Bukkit.getPlayer(owner);
        while (xp >= XP_PER_LEVEL) {
            xp -= XP_PER_LEVEL;
            int previous = level;
            level += 1;
            FamiliarInstance leveled = new FamiliarInstance(
                    instance.owner(),
                    instance.typeId(),
                    level,
                    xp,
                    instance.state(),
                    instance.summonedEntityId());
            if (player != null) {
                Bukkit.getPluginManager().callEvent(new FamiliarLeveledEvent(player, leveled, previous, level));
            }
            questsBridge.publish(new FamiliarQuestEvent(
                    FamiliarQuestEventType.LEVEL,
                    owner,
                    instance.typeId(),
                    level));
        }
        if (level == instance.level() && xp == instance.xp()) {
            return;
        }
        FamiliarInstance updated = new FamiliarInstance(
                instance.owner(),
                instance.typeId(),
                level,
                xp,
                instance.state(),
                instance.summonedEntityId());
        store.upsert(updated);
        save();
        publishUiInvalidate(player);
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
        questsBridge.publish(new FamiliarQuestEvent(
                FamiliarQuestEventType.SUMMON,
                player.getUniqueId(),
                typeId,
                summoned.level()));
        magicBridge.refresh(player, summoned);
        publishUiInvalidate(player);

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
        magicBridge.clear(player, instance);
        save();
        publishUiInvalidate(player);
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
        lastCombat.remove(owner);
        save();
        Player player = Bukkit.getPlayer(owner);
        publishUiInvalidate(player);
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
        Player player = Bukkit.getPlayer(owner);
        publishUiInvalidate(player);
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
        lastCombat.clear();
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

    private FamiliarLimitInfo resolveLimit(Player player) {
        if (limitService == null) {
            return new FamiliarLimitInfo(1, "default");
        }
        return limitService.resolveLimit(player);
    }

    private void checkAllowedClasses(Player player, FamiliarType type, List<String> reasons, List<String> notes) {
        if (type.allowedClasses().isEmpty()) {
            return;
        }
        String classId = classesBridge.getActiveClassId(player);
        if (classId == null || classId.isBlank()) {
            notes.add("Class bridge missing; skipping allowedClasses check");
            return;
        }
        boolean allowed = type.allowedClasses().stream()
                .anyMatch(entry -> entry.equalsIgnoreCase(classId));
        if (!allowed) {
            reasons.add("Class '" + classId + "' not allowed");
        }
    }

    private void checkTamePolicy(Player player, String typeId, EntityType entityType,
                                 List<String> reasons, List<String> notes) {
        if (policyRepository == null) {
            return;
        }
        TamePolicy policy = policyRepository.policy();
        if (policy == null) {
            return;
        }
        String classId = classesBridge.getActiveClassId(player);
        if (classId == null || classId.isBlank()) {
            notes.add("Class bridge missing; skipping tame policy check");
            return;
        }
        Optional<List<String>> allowedMobs = policy.allowedMobs(classId);
        if (allowedMobs.isEmpty()) {
            return;
        }
        boolean allowed = false;
        if (entityType != null) {
            allowed |= allowedMobs.get().contains(entityType.name().toLowerCase(Locale.ROOT));
        }
        if (typeId != null) {
            allowed |= allowedMobs.get().contains(typeId.toLowerCase(Locale.ROOT));
        }
        if (!allowed) {
            String mobLabel = entityType != null ? entityType.name() : typeId;
            reasons.add("Mob '" + mobLabel + "' not allowed for class '" + classId + "'");
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
            case "fox" -> EntityType.FOX;
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
                FamiliarType type = getType(instance.typeId()).orElse(null);
                applyRoleEffects(owner, entity, type);
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

    private void applyRoleEffects(Player owner, Entity entity, FamiliarType type) {
        if (owner == null || type == null) {
            return;
        }
        String role = type.role().toLowerCase(Locale.ROOT);
        switch (role) {
            case "combat" -> applyCombatRole(entity);
            case "support" -> applySupportRole(owner);
            case "utility" -> applyUtilityRole(owner);
            default -> {
            }
        }
    }

    private void applyCombatRole(Entity entity) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        living.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, COMBAT_BUFF_DURATION_TICKS, 0, true,
                false, false));
    }

    private void applySupportRole(Player owner) {
        applyPlayerEffect(owner, PotionEffectType.REGENERATION, SUPPORT_BUFF_DURATION_TICKS, 0);
    }

    private void applyUtilityRole(Player owner) {
        if (!isOutOfCombat(owner.getUniqueId())) {
            return;
        }
        applyPlayerEffect(owner, PotionEffectType.SPEED, UTILITY_BUFF_DURATION_TICKS, 0);
    }

    private void applyPlayerEffect(Player player, PotionEffectType type, int durationTicks, int amplifier) {
        if (player == null || type == null) {
            return;
        }
        PotionEffect existing = player.getPotionEffect(type);
        if (existing != null && existing.getDuration() > durationTicks / 2) {
            return;
        }
        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, true, false, false));
    }

    private boolean isOutOfCombat(UUID owner) {
        Instant last = lastCombat.get(owner);
        if (last == null) {
            return true;
        }
        return last.plusSeconds(OUT_OF_COMBAT_SECONDS).isBefore(Instant.now());
    }

    @Override
    public void recordOwnerCombat(UUID owner) {
        if (owner == null) {
            return;
        }
        lastCombat.put(owner, Instant.now());
    }

    private void publishUiInvalidate(Player player) {
        if (player == null) {
            return;
        }
        CoreAccess.core().events().publish(new UiInvalidateEvent(player, FamiliarUiService.HUD_PROVIDER_ID));
    }
}
