package ru.realite.magic.integration.guilds;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.quests.GuildAdapter;

public final class CoreGuildBridge implements GuildBridge {

    private final Supplier<GuildAdapter> guildAdapterSupplier;

    public CoreGuildBridge() {
        this(CoreGuildBridge::resolveGuildAdapter);
    }

    public CoreGuildBridge(Supplier<GuildAdapter> guildAdapterSupplier) {
        this.guildAdapterSupplier = Objects.requireNonNull(guildAdapterSupplier, "guildAdapterSupplier");
    }

    @Override
    public boolean isAvailable() {
        return guildAdapterSupplier.get() != null;
    }

    @Override
    public Optional<String> guildId(UUID playerId) {
        Player player = resolvePlayer(playerId);
        if (player == null) {
            return Optional.empty();
        }
        GuildAdapter adapter = guildAdapterSupplier.get();
        if (adapter == null) {
            return Optional.empty();
        }
        return adapter.getGuildTag(player);
    }

    @Override
    public Optional<String> guildRank(UUID playerId) {
        Player player = resolvePlayer(playerId);
        if (player == null) {
            return Optional.empty();
        }
        GuildAdapter adapter = guildAdapterSupplier.get();
        if (adapter == null) {
            return Optional.empty();
        }
        return adapter.getGuildRankId(player);
    }

    private Player resolvePlayer(UUID playerId) {
        if (playerId == null) {
            return null;
        }
        return Bukkit.getPlayer(playerId);
    }

    private static GuildAdapter resolveGuildAdapter() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        return provider.getProvider().services().get(GuildAdapter.class);
    }
}
