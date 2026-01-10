package ru.realite.magic;

import java.io.File;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ShapedRecipe;
import ru.realite.core.api.CoreApi;
import ru.realite.items.service.ItemService;
import ru.realite.magic.api.MagicApi;
import ru.realite.magic.api.impl.MagicApiImpl;
import ru.realite.magic.debug.DebugService;
import ru.realite.magic.effect.CleanseEffectExecutor;
import ru.realite.magic.effect.DamageEffectExecutor;
import ru.realite.magic.effect.EffectExecutorRegistry;
import ru.realite.magic.effect.HealEffectExecutor;
import ru.realite.magic.effect.KnockbackEffectExecutor;
import ru.realite.magic.effect.ParticlesEffectExecutor;
import ru.realite.magic.effect.PotionEffectExecutor;
import ru.realite.magic.effect.SoundEffectExecutor;
import ru.realite.magic.effect.TeleportEffectExecutor;
import ru.realite.magic.hud.MagicHudService;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.classes.CoreClassesBridge;
import ru.realite.magic.integration.classes.NoopClassesBridge;
import ru.realite.magic.integration.city.CityBridge;
import ru.realite.magic.integration.city.CoreCityBridge;
import ru.realite.magic.integration.city.NoopCityBridge;
import ru.realite.magic.integration.economy.CoreEconomyBridge;
import ru.realite.magic.integration.economy.EconomyBridge;
import ru.realite.magic.integration.economy.NoopEconomyBridge;
import ru.realite.magic.integration.economy.VaultEconomyBridge;
import ru.realite.magic.integration.events.BukkitEventPublisher;
import ru.realite.magic.integration.events.CoreEventPublisher;
import ru.realite.magic.integration.events.MagicEventPublisher;
import ru.realite.magic.integration.guilds.CoreGuildBridge;
import ru.realite.magic.integration.guilds.GuildBridge;
import ru.realite.magic.integration.guilds.NoopGuildBridge;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.ItemsBridgeFactory;
import ru.realite.magic.integration.talents.CoreTalentsBridge;
import ru.realite.magic.integration.talents.NoopTalentsBridge;
import ru.realite.magic.integration.talents.TalentsBridge;
import ru.realite.magic.listener.CombatListener;
import ru.realite.magic.listener.MagicInteractListener;
import ru.realite.magic.listener.MagicMenuListener;
import ru.realite.magic.listener.MasteryListener;
import ru.realite.magic.listener.PlayerCleanupListener;
import ru.realite.magic.listener.SpellBarListener;
import ru.realite.magic.listener.SpellUnlockListener;
import ru.realite.magic.listener.StaffRechargeListener;
import ru.realite.magic.mastery.MasteryService;
import ru.realite.magic.region.RegionRuleService;
import ru.realite.magic.command.MagicCommand;
import ru.realite.magic.service.GuildBonusService;
import ru.realite.magic.service.MagicService;
import ru.realite.magic.service.PlayerSpellService;
import ru.realite.magic.service.PlayerSpellServiceImpl;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.storage.PlayerSpellStorage;
import ru.realite.magic.storage.YamlPlayerSpellStorage;
import ru.realite.core.api.logging.Banners;

public final class RealiteMagicPlugin extends JavaPlugin {

    private MagicMessages messages;
    private MagicService magicService;
    private SpellRegistry spellRegistry;
    private PlayerSpellService playerSpellService;
    private DebugService debugService;
    private MagicHudService hudService;
    private MagicApi magicApi;
    private MasteryService masteryService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");
        saveIfNotExists("spells/warlock.yml");
        saveIfNotExists("spells/fire.yml");
        saveIfNotExists("spells/frost.yml");
        saveIfNotExists("spells/holy.yml");
        messages = new MagicMessages(this);
        EffectExecutorRegistry effectRegistry = buildEffectRegistry();
        spellRegistry = new SpellRegistry(this, effectRegistry);
        boolean strictValidation = getConfig().getBoolean("release.strictValidation", true);
        boolean logConfigWarnings = getConfig().getBoolean("release.logConfigWarnings", true);
        var report = spellRegistry.load();
        if (report.hasErrors() && logConfigWarnings) {
            getLogger().warning(messages.raw("magic.cmd.spells.errors.header"));
            for (var error : report.errors()) {
                if ("magic.cmd.spells.errors.schema".equals(error.messageKey())) {
                    getLogger().warning(messages.raw(error.messageKey(), error.placeholders()));
                    continue;
                }
                if (error.placeholders().containsKey("path")) {
                    String errorMessage = error.messageKey() == null
                            ? (error.message() == null ? messages.raw("magic.cmd.spells.errors.unknown")
                                    : error.message())
                            : messages.raw(error.messageKey(), error.placeholders());
                    getLogger().warning(messages.raw("magic.cmd.spells.errors.schema",
                            Map.of("file", error.fileName(),
                                    "path", error.placeholders().get("path"),
                                    "error", errorMessage)));
                    continue;
                }
                getLogger().warning(messages.raw("magic.cmd.spells.errors.entry",
                        "file", error.fileName(),
                        "error", error.messageKey() == null
                                ? (error.message() == null ? messages.raw("magic.cmd.spells.errors.unknown")
                                        : error.message())
                                : messages.raw(error.messageKey(), error.placeholders())));
            }
            if (!strictValidation) {
                getLogger().warning("[Magic] Spell validation is not strict; invalid entries were skipped.");
            }
        }
        PlayerSpellStorage storage = new YamlPlayerSpellStorage(this, messages);
        MagicEventPublisher eventPublisher = resolveEventPublisher();
        PlayerSpellServiceImpl playerSpellServiceImpl = new PlayerSpellServiceImpl(storage, spellRegistry, messages,
                eventPublisher);
        playerSpellService = playerSpellServiceImpl;
        ItemsBridge itemsBridge = ItemsBridgeFactory.create();
        ClassesBridge classesBridge = resolveClassesBridge();
        EconomyBridge economyBridge = resolveEconomyBridge();
        TalentsBridge talentsBridge = resolveTalentsBridge();
        CityBridge cityBridge = resolveCityBridge();
        GuildBridge guildBridge = resolveGuildBridge();
        debugService = new DebugService(messages);
        hudService = new MagicHudService(this, messages, spellRegistry);
        masteryService = new MasteryService(this, messages, spellRegistry, playerSpellServiceImpl);
        RegionRuleService regionRuleService = new RegionRuleService(this, cityBridge);
        GuildBonusService guildBonusService = new GuildBonusService(this, guildBridge);
        magicService = new MagicService(this,
                messages,
                spellRegistry,
                playerSpellService,
                itemsBridge,
                classesBridge,
                economyBridge,
                talentsBridge,
                eventPublisher,
                effectRegistry,
                debugService,
                hudService,
                masteryService,
                regionRuleService,
                guildBonusService);
        magicService.start();
        magicApi = new MagicApiImpl(spellRegistry, playerSpellService, magicService);
        Bukkit.getServicesManager().register(MagicApi.class, magicApi, this, ServicePriority.Normal);
        registerCommand();
        registerListeners();
        registerCraftingRecipes();
        
        // === Startup log (after full init) ===
        Banners.REALITE_MAGIC(this);
    }

    public MagicMessages getMessages() {
        return messages;
    }

    public MagicService getMagicService() {
        return magicService;
    }

    public SpellRegistry getSpellRegistry() {
        return spellRegistry;
    }

    public PlayerSpellService getPlayerSpellService() {
        return playerSpellService;
    }

    @Override
    public void onDisable() {
        if (magicService != null) {
            magicService.stop();
        }
        if (magicApi != null) {
            Bukkit.getServicesManager().unregister(MagicApi.class, magicApi);
            magicApi = null;
        }
        if (playerSpellService != null) {
            playerSpellService.flushAll();
        }
    }

    private void registerCommand() {
        var command = getCommand("rmagic");
        if (command == null) {
            getLogger().warning("Command /rmagic missing in plugin.yml");
            return;
        }
        MagicCommand magicCommand = new MagicCommand(this, magicService, playerSpellService, messages,
                debugService, hudService);
        command.setExecutor(magicCommand);
        command.setTabCompleter(magicCommand);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new CombatListener(magicService), this);
        MagicInteractListener interactListener = new MagicInteractListener(magicService);
        Bukkit.getPluginManager().registerEvents(
                new SpellUnlockListener(magicService, playerSpellService, messages), this);
        Bukkit.getPluginManager().registerEvents(
                new PlayerCleanupListener(magicService, playerSpellService), this);
        Bukkit.getPluginManager().registerEvents(
                new MagicMenuListener(magicService.spellSelectMenu(), spellRegistry, playerSpellService, messages),
                this);
        Bukkit.getPluginManager().registerEvents(interactListener, this);
        Bukkit.getPluginManager().registerEvents(
                new SpellBarListener(playerSpellService, hudService), this);
        Bukkit.getPluginManager().registerEvents(new MasteryListener(masteryService), this);
        Bukkit.getPluginManager().registerEvents(
                new StaffRechargeListener(this, messages, magicService.itemsBridge(),
                        magicService.staffChargeService()),
                this);
    }

    private void saveIfNotExists(String resourcePath) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + getDataFolder());
            return;
        }

        if (getResource(resourcePath) == null) {
            return;
        }

        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    private ClassesBridge resolveClassesBridge() {
        CoreClassesBridge coreBridge = new CoreClassesBridge(getLogger());
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        return new NoopClassesBridge();
    }

    private TalentsBridge resolveTalentsBridge() {
        CoreTalentsBridge coreBridge = new CoreTalentsBridge(getLogger());
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        return new NoopTalentsBridge();
    }

    private CityBridge resolveCityBridge() {
        CoreCityBridge coreBridge = new CoreCityBridge();
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        return new NoopCityBridge();
    }

    private GuildBridge resolveGuildBridge() {
        CoreGuildBridge coreBridge = new CoreGuildBridge();
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        return new NoopGuildBridge();
    }

    private EconomyBridge resolveEconomyBridge() {
        CoreEconomyBridge coreBridge = new CoreEconomyBridge(getLogger());
        if (coreBridge.isAvailable()) {
            return coreBridge;
        }
        VaultEconomyBridge vaultBridge = new VaultEconomyBridge(getLogger());
        if (vaultBridge.isAvailable()) {
            return vaultBridge;
        }
        return new NoopEconomyBridge();
    }

    private void registerCraftingRecipes() {
        if (!getConfig().getBoolean("crafting.enabled", true)) {
            return;
        }
        ItemService itemService = resolveItemService();
        if (itemService == null) {
            getLogger().warning("[Magic] Crafting recipes are enabled, but RealiteItems is unavailable.");
            return;
        }
        ConfigurationSection recipesSection = getConfig().getConfigurationSection("crafting.recipes");
        if (recipesSection == null) {
            return;
        }
        for (String recipeId : recipesSection.getKeys(false)) {
            ConfigurationSection recipeSection = recipesSection.getConfigurationSection(recipeId);
            if (recipeSection == null || !recipeSection.getBoolean("enabled", true)) {
                continue;
            }
            String resultItemId = recipeSection.getString("resultItemId");
            if (resultItemId == null || resultItemId.isBlank()) {
                getLogger().warning("[Magic] Crafting recipe " + recipeId + " missing resultItemId.");
                continue;
            }
            var shape = recipeSection.getStringList("shape");
            if (shape == null || shape.isEmpty()) {
                getLogger().warning("[Magic] Crafting recipe " + recipeId + " missing shape.");
                continue;
            }
            NamespacedKey key = new NamespacedKey(this, "magic_" + recipeId.toLowerCase());
            ShapedRecipe recipe;
            try {
                recipe = new ShapedRecipe(key, itemService.create(resultItemId, 1));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("[Magic] Crafting recipe " + recipeId + " failed: " + ex.getMessage());
                continue;
            }
            recipe.shape(shape.toArray(new String[0]));
            ConfigurationSection ingredientsSection = recipeSection.getConfigurationSection("ingredients");
            if (ingredientsSection == null) {
                getLogger().warning("[Magic] Crafting recipe " + recipeId + " missing ingredients.");
                continue;
            }
            for (String ingredientKey : ingredientsSection.getKeys(false)) {
                if (ingredientKey.length() != 1) {
                    continue;
                }
                String materialName = ingredientsSection.getString(ingredientKey);
                if (materialName == null || materialName.isBlank()) {
                    continue;
                }
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    getLogger().warning("[Magic] Crafting recipe " + recipeId + " has unknown material: "
                            + materialName);
                    continue;
                }
                recipe.setIngredient(ingredientKey.charAt(0), material);
            }
            Bukkit.addRecipe(recipe);
        }
    }

    private ItemService resolveItemService() {
        RegisteredServiceProvider<ItemService> provider = Bukkit.getServicesManager()
                .getRegistration(ItemService.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        return provider.getProvider();
    }

    private MagicEventPublisher resolveEventPublisher() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider != null && provider.getProvider() != null) {
            return new CoreEventPublisher(provider.getProvider().events());
        }
        return new BukkitEventPublisher();
    }

    private EffectExecutorRegistry buildEffectRegistry() {
        EffectExecutorRegistry registry = new EffectExecutorRegistry();
        registry.register(new DamageEffectExecutor());
        registry.register(new PotionEffectExecutor());
        registry.register(new ParticlesEffectExecutor());
        registry.register(new SoundEffectExecutor());
        registry.register(new HealEffectExecutor());
        registry.register(new TeleportEffectExecutor());
        registry.register(new KnockbackEffectExecutor());
        registry.register(new CleanseEffectExecutor());
        return registry;
    }
}
