package ru.realite.quests;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;
import ru.realite.core.api.logging.Banners;
import ru.realite.core.api.quests.QuestService;
import ru.realite.quests.command.QuestCommand;
import ru.realite.quests.service.QuestObjectiveListener;

public final class RealiteQuestsPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private final RealiteQuestsEntrypoint entrypoint = new RealiteQuestsEntrypoint();

    private boolean initialized;
    private int initTaskId = -1;

    @Override
    public void onEnable() {
        Banners.REALITE_QUESTS_WAITING(this);

        tryInitialize();
        if (!initialized) {
            initTaskId = Bukkit.getScheduler()
                    .runTaskTimer(this, this::tryInitialize, 20L, 20L)
                    .getTaskId();
        }
    }

    @Override
    public void onDisable() {
        if (initTaskId != -1) {
            Bukkit.getScheduler().cancelTask(initTaskId);
            initTaskId = -1;
        }
        initialized = false;
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

        PluginCommand cmd = getCommand("quest");
        if (cmd != null) {
            cmd.setExecutor(new QuestCommand(() -> core.services().get(QuestService.class)));
        } else {
            getLogger().warning("Command /quest not found in plugin.yml; executor not registered.");
        }

        Bukkit.getPluginManager().registerEvents(
                new QuestObjectiveListener(() -> core.services().get(QuestService.class)),
                this);

        initialized = true;

        if (initTaskId != -1) {
            Bukkit.getScheduler().cancelTask(initTaskId);
            initTaskId = -1;
        }

        getLogger().info("Initialized (QuestService bound, listeners registered)");
    }

    private CoreApi resolveCore() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) {
            return null;
        }
        return provider.getProvider();
    }
}
