package ru.realite.familiars.service;

import ru.realite.familiars.model.FamiliarInstance;
import ru.realite.familiars.model.FamiliarState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FamiliarStore {

    private final Map<UUID, List<FamiliarInstance>> instances = new ConcurrentHashMap<>();

    public List<FamiliarInstance> getInstances(UUID owner) {
        return instances.getOrDefault(owner, Collections.emptyList());
    }

    public Map<UUID, List<FamiliarInstance>> snapshot() {
        Map<UUID, List<FamiliarInstance>> snapshot = new ConcurrentHashMap<>();
        for (Map.Entry<UUID, List<FamiliarInstance>> entry : instances.entrySet()) {
            snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return snapshot;
    }

    public void loadAll(Map<UUID, List<FamiliarInstance>> loaded) {
        instances.clear();
        if (loaded == null || loaded.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, List<FamiliarInstance>> entry : loaded.entrySet()) {
            instances.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    public int countActive(UUID owner) {
        return getInstances(owner).size();
    }

    public int countSummoned(UUID owner) {
        return (int) getInstances(owner).stream()
                .filter(instance -> instance.state() == FamiliarState.SUMMONED)
                .count();
    }

    public void upsert(FamiliarInstance instance) {
        instances.compute(instance.owner(), (key, list) -> {
            List<FamiliarInstance> updated = new ArrayList<>(list == null ? List.of() : list);
            updated.removeIf(existing -> existing.typeId().equals(instance.typeId()));
            updated.add(instance);
            return updated;
        });
    }

    public void clear() {
        instances.clear();
    }
}
