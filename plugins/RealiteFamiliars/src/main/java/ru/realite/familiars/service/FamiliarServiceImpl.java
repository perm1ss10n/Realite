package ru.realite.familiars.service;

import org.bukkit.entity.Player;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassTagProvider;
import ru.realite.familiars.config.FamiliarTypeRepository;
import ru.realite.familiars.config.TamingRules;
import ru.realite.familiars.config.TamingRulesRepository;
import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarType;
import ru.realite.familiars.model.FamiliarState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class FamiliarServiceImpl implements FamiliarService {

    private final CoreApi core;
    private final FamiliarStore store;
    private final FamiliarRepository repository;
    private final Logger logger;
    private FamiliarTypeRepository typeRepository;
    private TamingRulesRepository rulesRepository;
    private final Map<UUID, Instant> lastTame = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> lastSummon = new ConcurrentHashMap<>();

    public FamiliarServiceImpl(CoreApi core, FamiliarStore store, FamiliarRepository repository, Logger logger) {
        this.core = core;
        this.store = store;
        this.repository = repository;
        this.logger = logger;
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
                Optional.empty()
        );
        store.upsert(instance);
        lastTame.put(player.getUniqueId(), Instant.now());
        save();
        return new TameResult(check, instance);
    }

    public void shutdown() {
        save();
        store.clear();
        lastTame.clear();
        lastSummon.clear();
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
}
