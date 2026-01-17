package ru.realite.familiars.integration.classes;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.realite.core.api.classes.ClassProfile;
import ru.realite.core.api.classes.ClassProfileProvider;

public final class CoreClassesBridge implements ClassesBridge {

    private final Supplier<ClassProfileProvider> profileProviderSupplier;

    public CoreClassesBridge(Supplier<ClassProfileProvider> profileProviderSupplier) {
        this.profileProviderSupplier = Objects.requireNonNull(profileProviderSupplier, "profileProviderSupplier");
    }

    @Override
    public boolean isAvailable() {
        return profileProviderSupplier.get() != null;
    }

    @Override
    public @Nullable String getActiveClassId(Player player) {
        ClassProfileProvider provider = profileProviderSupplier.get();
        if (provider == null || player == null) {
            return null;
        }
        Optional<ClassProfile> profile = provider.getProfile(player);
        return profile.map(ClassProfile::classId).orElse(null);
    }
}
