package ru.realite.city.storage;

import ru.realite.city.model.PlotMemberRole;
import ru.realite.core.api.Storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SqlitePlotMemberRepository implements PlotMemberRepository {

    private final Storage storage;
    private final Map<String, Map<UUID, PlotMemberRole>> cache = new ConcurrentHashMap<>();

    public SqlitePlotMemberRepository(Storage storage) {
        this.storage = storage;
    }

    public int loadAll() throws SQLException {
        cache.clear();
        Connection connection = storage.connection();
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT plot_id, member_uuid, role FROM plot_members"
        )) {
            try (ResultSet rs = statement.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String plotId = rs.getString("plot_id");
                    UUID memberUuid = UUID.fromString(rs.getString("member_uuid"));
                    PlotMemberRole role = PlotMemberRole.valueOf(rs.getString("role"));
                    cache.computeIfAbsent(plotId, ignored -> new ConcurrentHashMap<>())
                            .put(memberUuid, role);
                    count++;
                }
                return count;
            }
        }
    }

    @Override
    public void upsert(String plotId, UUID memberUuid, PlotMemberRole role) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO plot_members(plot_id, member_uuid, role) "
                            + "VALUES(?, ?, ?) "
                            + "ON CONFLICT(plot_id, member_uuid) DO UPDATE SET "
                            + "role = excluded.role"
            )) {
                statement.setString(1, plotId);
                statement.setString(2, memberUuid.toString());
                statement.setString(3, role.name());
                statement.executeUpdate();
            }
            cache.computeIfAbsent(plotId, ignored -> new ConcurrentHashMap<>())
                    .put(memberUuid, role);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to upsert plot member: " + plotId, e);
        }
    }

    @Override
    public boolean remove(String plotId, UUID memberUuid) {
        try {
            Connection connection = storage.connection();
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM plot_members WHERE plot_id = ? AND member_uuid = ?"
            )) {
                statement.setString(1, plotId);
                statement.setString(2, memberUuid.toString());
                int updated = statement.executeUpdate();
                if (updated > 0) {
                    Map<UUID, PlotMemberRole> members = cache.get(plotId);
                    if (members != null) {
                        members.remove(memberUuid);
                    }
                    return true;
                }
                return false;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove plot member: " + plotId, e);
        }
    }

    @Override
    public Optional<PlotMemberRole> findRole(String plotId, UUID memberUuid) {
        if (plotId == null || memberUuid == null) {
            return Optional.empty();
        }
        Map<UUID, PlotMemberRole> members = cache.get(plotId);
        if (members == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(members.get(memberUuid));
    }

    @Override
    public boolean isMember(String plotId, UUID memberUuid) {
        return findRole(plotId, memberUuid).isPresent();
    }
}
