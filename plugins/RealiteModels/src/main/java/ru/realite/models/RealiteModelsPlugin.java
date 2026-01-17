package ru.realite.models;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.models.ModelAssetRegistry;
import ru.realite.core.api.models.ModelsBridge;
import ru.realite.models.command.ModelsCommand;
import ru.realite.models.service.ModelsBridgeImpl;

public final class RealiteModelsPlugin extends JavaPlugin {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private BukkitTask corePollingTask;
    private boolean waitingLogged;
    private boolean bridgeRegistered;
    private boolean registrationConflictLogged;
    private ModelsBridgeImpl bridge;

    @Override
    public void onEnable() {
        bridge = new ModelsBridgeImpl(this::resolveModelRegistry);

        if (!attemptRegisterBridge()) {
            startCorePolling();
        }

        registerCommands();
    }

    @Override
    public void onDisable() {
        if (corePollingTask != null) {
            corePollingTask.cancel();
            corePollingTask = null;
        }
        unregisterBridge();
    }

    private boolean attemptRegisterBridge() {
        CoreApi core = resolveCore();
        if (core == null) {
            if (!waitingLogged) {
                waitingLogged = true;
                sendConsole(miniMessage.deserialize(
                        "<gray>[RealiteModels]</gray> <yellow>CoreApi not ready, waiting for registration.</yellow>"));
            }
            return false;
        }

        boolean registered = core.services().registerIfAbsent(ModelsBridge.class, bridge);
        if (registered) {
            bridgeRegistered = true;
            sendConsole(miniMessage.deserialize(
                    "<gray>[RealiteModels]</gray> <green>Models bridge registered in Core.</green>"));
            return true;
        }

        ModelsBridge current = core.services().get(ModelsBridge.class);
        if (current == bridge) {
            bridgeRegistered = true;
            sendConsole(miniMessage.deserialize(
                    "<gray>[RealiteModels]</gray> <green>Models bridge already registered.</green>"));
            return true;
        }

        if (!registrationConflictLogged) {
            registrationConflictLogged = true;
            sendConsole(miniMessage.deserialize(
                    "<gray>[RealiteModels]</gray> <red>Models bridge already registered by another module.</red>"));
        }
        return true;
    }

    private void startCorePolling() {
        if (corePollingTask != null) {
            return;
        }
        corePollingTask = getServer().getScheduler().runTaskTimer(
                this,
                () -> {
                    if (attemptRegisterBridge()) {
                        corePollingTask.cancel();
                        corePollingTask = null;
                    }
                },
                20L,
                40L
        );
    }

    private void unregisterBridge() {
        if (!bridgeRegistered) {
            return;
        }
        CoreApi core = resolveCore();
        if (core == null) {
            return;
        }
        ModelsBridge current = core.services().get(ModelsBridge.class);
        if (current == bridge) {
            core.services().unregister(ModelsBridge.class);
        }
        bridgeRegistered = false;
    }

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            return null;
        }
        return provider.getProvider();
    }

    private ModelAssetRegistry resolveModelRegistry() {
        CoreApi core = resolveCore();
        if (core == null) {
            return null;
        }
        return core.services().get(ModelAssetRegistry.class);
    }

    private void registerCommands() {
        var command = getCommand("models");
        if (command == null) {
            getLogger().warning("Command /models not found in plugin.yml; executor not registered.");
            return;
        }
        ModelsCommand executor = new ModelsCommand(this::resolveModelRegistry);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void sendConsole(Component message) {
        getServer().getConsoleSender().sendMessage(message);
    }
}
