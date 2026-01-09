package ru.realite.magic.integration.classes;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.Nullable;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassProfile;
import ru.realite.core.api.classes.ClassProfileProvider;

public final class CoreClassesBridge implements ClassesBridge {

    private final Supplier<ClassProfileProvider> classProfileProviderSupplier;
    private final Logger logger;
    private final AtomicBoolean warned = new AtomicBoolean(false);

    public CoreClassesBridge(Logger logger) {
        this(logger, CoreClassesBridge::resolveClassProfileProvider);
    }

    public CoreClassesBridge(Logger logger, Supplier<ClassProfileProvider> classProfileProviderSupplier) {
        this.logger = Objects.requireNonNull(logger, "logger");
        this.classProfileProviderSupplier = Objects.requireNonNull(classProfileProviderSupplier, "classProfileProviderSupplier");
    }

    public boolean isAvailable() {
        return classProfileProvider() != null;
    }

    @Override
    public @Nullable String getActiveClassId(Player player) {
        if (player == null) {
            return null;
        }
        ClassProfileProvider provider = classProfileProvider();
        if (provider == null) {
            return null;
        }
        Optional<ClassProfile> profile = provider.getProfile(player);
        return profile.map(ClassProfile::classId).orElse(null);
    }

    @Override
    public @Nullable String getActiveEvolutionId(Player player) {
        if (player == null) {
            return null;
        }
        ClassProfileProvider provider = classProfileProvider();
        if (provider == null) {
            return null;
        }
        Optional<ClassProfile> profile = provider.getProfile(player);
        return profile.map(ClassProfile::evolutionId).orElse(null);
    }

    @Override
    public Component displayClassName(String classId) {
        if (classId == null || classId.isBlank()) {
            return Component.empty();
        }
        return Component.text(classId);
    }

    @Override
    public Component displayEvolutionName(String evolutionId) {
        if (evolutionId == null || evolutionId.isBlank()) {
            return Component.empty();
        }
        return Component.text(evolutionId);
    }

    private ClassProfileProvider classProfileProvider() {
        ClassProfileProvider provider = classProfileProviderSupplier.get();
        if (provider == null) {
            warnMissingService();
        }
        return provider;
    }

    private void warnMissingService() {
        if (!warned.compareAndSet(false, true)) {
            return;
        }
        logger.warning("Classes module service not available; falling back to no-op bridge.");
    }

    private static ClassProfileProvider resolveClassProfileProvider() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        CoreApi core = provider.getProvider();
        return core.services().get(ClassProfileProvider.class);
    }
}
