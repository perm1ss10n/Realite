package ru.realite.quests;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;
import ru.realite.core.api.quests.QuestService;
import ru.realite.quests.command.QuestCommand;
import ru.realite.quests.service.QuestObjectiveListener;

public final class RealiteQuestsPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private final RealiteQuestsEntrypoint entrypoint = new RealiteQuestsEntrypoint();
    private boolean initialized;
    private int initTaskId = -1;

    @Override
    public void onEnable() {
        getLogger().info("RealiteQuests loaded. Waiting for module enable.");
        tryInitialize();
        if (!initialized) {
            initTaskId = Bukkit.getScheduler().runTaskTimer(this, this::tryInitialize, 20L, 20L).getTaskId();
        }
    }

    @Override
    public Module module() {
        return entrypoint.module();
    }

    private void tryInitialize() {
        if (initialized) {
            return;
        }
        CoreApi core = resolveCore();
        if (core == null) {
            return;
        }
        getCommand("quest").setExecutor(
                new QuestCommand(() -> core.services().get(QuestService.class)));
        Bukkit.getPluginManager().registerEvents(
                new QuestObjectiveListener(() -> core.services().get(QuestService.class)),
                this);
        initialized = true;
        if (initTaskId != -1) {
            Bukkit.getScheduler().cancelTask(initTaskId);
        }
        getLogger().info("RealiteQuests initialized.");
    }

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            return null;
        }
        return provider.getProvider();
    }
}
