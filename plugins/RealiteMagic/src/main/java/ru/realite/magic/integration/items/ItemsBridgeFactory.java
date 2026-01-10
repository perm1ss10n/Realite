package ru.realite.magic.integration.items;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.items.service.ItemService;

public final class ItemsBridgeFactory {

    private ItemsBridgeFactory() {
    }

    public static ItemsBridge create() {
        RegisteredServiceProvider<ItemService> provider =
                Bukkit.getServicesManager().getRegistration(ItemService.class);
        if (provider == null || provider.getProvider() == null) {
            return new NoopItemsBridge();
        }
        return new CoreItemsBridge(provider::getProvider);
    }
}
