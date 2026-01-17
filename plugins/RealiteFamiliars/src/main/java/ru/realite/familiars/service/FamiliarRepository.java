package ru.realite.familiars.service;

import ru.realite.familiars.model.FamiliarInstance;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FamiliarRepository {
    Map<UUID, List<FamiliarInstance>> load();

    void save(Map<UUID, List<FamiliarInstance>> data);
}
