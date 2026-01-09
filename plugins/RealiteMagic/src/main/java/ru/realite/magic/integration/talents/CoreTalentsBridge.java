package ru.realite.magic.integration.talents;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.talents.TalentDefinition;
import ru.realite.core.api.talents.TalentProvider;

public final class CoreTalentsBridge implements TalentsBridge {

    private final Supplier<TalentProvider> talentProviderSupplier;
    private final Logger logger;
    private final AtomicBoolean warned = new AtomicBoolean(false);

    public CoreTalentsBridge(Logger logger) {
        this(logger, CoreTalentsBridge::resolveTalentProvider);
    }

    public CoreTalentsBridge(Logger logger, Supplier<TalentProvider> talentProviderSupplier) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.talentProviderSupplier = Objects.requireNonNull(talentProviderSupplier, "talentProviderSupplier");
    }

    @Override
    public boolean isAvailable() {
        return talentProvider() != null;
    }

    @Override
    public Set<String> activeTalents(Player player) {
        if (player == null) {
            return Set.of();
        }
        TalentProvider provider = talentProvider();
        if (provider == null) {
            return Set.of();
        }
        Set<String> talents = provider.activeTalents(player);
        return talents == null ? Set.of() : talents;
    }

    @Override
    public Optional<TalentDefinition> findTalent(String talentId) {
        if (talentId == null || talentId.isBlank()) {
            return Optional.empty();
        }
        TalentProvider provider = talentProvider();
        if (provider == null) {
            return Optional.empty();
        }
        return provider.findTalent(talentId);
    }

    private TalentProvider talentProvider() {
        TalentProvider provider;
        try {
            provider = talentProviderSupplier.get();
        } catch (Exception ex) {
            warnMissingService(ex);
            return null;
        }
        if (provider == null) {
            warnMissingService(null);
        }
        return provider;
    }

    private void warnMissingService(Throwable throwable) {
        if (!warned.compareAndSet(false, true)) {
            return;
        }
        String message = "TalentProvider not available (yet). Talent modifiers will be skipped.";
        if (throwable == null) {
            logger.warning(message);
            return;
        }
        logger.log(Level.WARNING, message, throwable);
    }

    private static TalentProvider resolveTalentProvider() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        CoreApi core = provider.getProvider();
        return core.services().get(TalentProvider.class);
    }
}
