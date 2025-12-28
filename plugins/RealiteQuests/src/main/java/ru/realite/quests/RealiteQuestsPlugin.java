package ru.realite.quests;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreModuleEntrypoint;
import ru.realite.core.api.Module;

public final class RealiteQuestsPlugin extends JavaPlugin implements CoreModuleEntrypoint {

    private final RealiteQuestsEntrypoint entrypoint = new RealiteQuestsEntrypoint();

    @Override
    public void onEnable() {
        getLogger().info("RealiteQuests loaded. Waiting for module enable.");
    }

    @Override
    public Module module() {
        return entrypoint.module();
    }
}
