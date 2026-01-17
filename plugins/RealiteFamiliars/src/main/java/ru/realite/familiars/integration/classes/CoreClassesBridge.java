package ru.realite.familiars.integration.classes;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.realite.core.api.classes.ClassProfile;
import ru.realite.core.api.classes.ClassProfileProvider;
import ru.realite.core.api.classes.ClassTag;
import ru.realite.core.api.classes.ClassTagProvider;

public final class CoreClassesBridge implements ClassesBridge {

    private final Supplier<ClassProfileProvider> profileProviderSupplier;
    private final Supplier<ClassTagProvider> tagProviderSupplier;

    public CoreClassesBridge(Supplier<ClassProfileProvider> profileProviderSupplier,
                             Supplier<ClassTagProvider> tagProviderSupplier) {
        this.profileProviderSupplier = Objects.requireNonNull(profileProviderSupplier, "profileProviderSupplier");
        this.tagProviderSupplier = Objects.requireNonNull(tagProviderSupplier, "tagProviderSupplier");
    }

    @Override
    public boolean isAvailable() {
        return profileProviderSupplier.get() != null;
    }

    @Override
    public @Nullable ClassTierInfo getActiveClassInfo(Player player) {
        ClassProfileProvider provider = profileProviderSupplier.get();
        if (provider == null || player == null) {
            return null;
        }
        Optional<ClassProfile> profile = provider.getProfile(player);
        String classId = profile.map(ClassProfile::classId).orElse(null);
        if (classId == null || classId.isBlank()) {
            return null;
        }
        int tier = resolveEvolutionTier(player);
        return new ClassTierInfo(classId, tier);
    }

    private int resolveEvolutionTier(Player player) {
        ClassTagProvider provider = tagProviderSupplier.get();
        if (provider == null) {
            return 1;
        }
        ClassTag tag = provider.getTag(player);
        if (tag == null) {
            return 1;
        }
        return Math.max(1, tag.evolutionStage());
    }
}
