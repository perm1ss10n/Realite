package ru.realite.magic.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.ui.UiPaginationService;
import ru.realite.core.api.ui.UiScreenRegistry;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.ui.screen.MagicLoadoutScreen;
import ru.realite.magic.ui.screen.MagicSpellDetailsScreen;
import ru.realite.magic.ui.screen.MagicSpellsScreen;

public final class MagicUiScreensRegistrar {

    private final JavaPlugin plugin;
    private final MagicService magicService;
    private final SpellRegistry spellRegistry;
    private final PlayerSpellService playerSpellService;
    private final MagicMessages messages;

    public MagicUiScreensRegistrar(JavaPlugin plugin,
                                   MagicService magicService,
                                   SpellRegistry spellRegistry,
                                   PlayerSpellService playerSpellService,
                                   MagicMessages messages) {
        this.plugin = plugin;
        this.magicService = magicService;
        this.spellRegistry = spellRegistry;
        this.playerSpellService = playerSpellService;
        this.messages = messages;
    }

    public void register() {
        CoreApi core = resolveCore();
        if (core == null) {
            return;
        }
        UiScreenRegistry registry = core.services().get(UiScreenRegistry.class);
        UiPaginationService paginationService = core.services().get(UiPaginationService.class);
        if (registry == null || paginationService == null) {
            plugin.getLogger().warning("[Magic] UI screen registry or pagination service missing.");
            return;
        }
        registry.register(new MagicSpellsScreen(magicService, spellRegistry, playerSpellService,
                messages, paginationService, registry));
        registry.register(new MagicSpellDetailsScreen(magicService, spellRegistry, playerSpellService,
                messages, registry));
        registry.register(new MagicLoadoutScreen(magicService, spellRegistry, playerSpellService,
                messages, registry));
    }

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        return provider.getProvider();
    }
}
