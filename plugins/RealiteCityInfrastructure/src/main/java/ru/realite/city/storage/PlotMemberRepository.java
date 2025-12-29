package ru.realite.city.storage;

import ru.realite.city.model.PlotMemberRole;

import java.util.Optional;
import java.util.UUID;

public interface PlotMemberRepository {
    void upsert(String plotId, UUID memberUuid, PlotMemberRole role);

    boolean remove(String plotId, UUID memberUuid);

    Optional<PlotMemberRole> findRole(String plotId, UUID memberUuid);

    boolean isMember(String plotId, UUID memberUuid);
}
