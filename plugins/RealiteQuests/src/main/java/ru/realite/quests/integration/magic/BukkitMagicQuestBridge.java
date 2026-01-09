package ru.realite.quests.integration.magic;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.magic.api.MagicApi;

public final class BukkitMagicQuestBridge implements MagicQuestBridge {

    private MagicApi magicApi;

    @Override
    public boolean isAvailable() {
        return api().isPresent();
    }

    @Override
    public Optional<MagicApi> api() {
        refresh();
        return Optional.ofNullable(magicApi);
    }

    @Override
    public void refresh() {
        RegisteredServiceProvider<MagicApi> provider = Bukkit.getServicesManager()
                .getRegistration(MagicApi.class);
        magicApi = provider != null ? provider.getProvider() : null;
    }
}
