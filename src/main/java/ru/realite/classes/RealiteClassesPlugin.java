package ru.realite.classes;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.classes.command.ClassCommand;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.listener.MenuListener;
import ru.realite.classes.listener.PlayerJoinListener;
import ru.realite.classes.listener.PlayerQuitListener;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EffectService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.YamlProfileRepository;
import ru.realite.classes.util.Messages;

public final class RealiteClassesPlugin extends JavaPlugin {

    private ClassService classService; // чтобы сохранить в onDisable

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("classes.yml", false);
        saveResource("messages.yml", false);

        // репозитории
        YamlProfileRepository profileRepo = new YamlProfileRepository(getDataFolder());
        ClassConfigRepository classConfig = new ClassConfigRepository(getDataFolder());
        classConfig.reload();

        // permissions
        String changePerm = getConfig().getString("permissions.change-class", "realiteclass.change");

        // сервисы
        EvolutionService evolutionService = new EvolutionService(classConfig, changePerm);
        this.classService = new ClassService(profileRepo, evolutionService);

        // эффекты
        int tickSeconds = getConfig().getInt("effects.tick-seconds", 5);
        boolean clearManaged = getConfig().getBoolean("effects.clear-managed-effects", true);
        EffectService effectService = new EffectService(classService, classConfig, clearManaged);

        getServer().getScheduler().runTaskTimer(
                this,
                () -> getServer().getOnlinePlayers().forEach(effectService::applyFor),
                20L,
                tickSeconds * 20L
        );

        // сообщения
        Messages messages = new Messages(this);

        // GUI
        ClassSelectMenu menu = new ClassSelectMenu(this, classConfig);

        // команды
        if (getCommand("class") != null) {
            getCommand("class").setExecutor(new ClassCommand(classService, evolutionService, menu, messages));
        } else {
            getLogger().severe("Command /class is not defined in plugin.yml!");
        }

        // листенеры
        getServer().getPluginManager().registerEvents(new MenuListener(classService, evolutionService, messages), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, classService, menu), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(classService), this);

        // автосейв профилей (каждые N минут)
        int autosaveMinutes = getConfig().getInt("storage.autosave-minutes", 5);
        if (autosaveMinutes > 0) {
            long period = autosaveMinutes * 60L * 20L;
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    classService.saveAll();
                } catch (Exception e) {
                    getLogger().log(java.util.logging.Level.SEVERE, "AutoSave failed", e);
                }
            }, period, period);
        }

        getLogger().info("RealiteClasses enabled.");
    }

    @Override
    public void onDisable() {
        if (classService != null) {
            classService.saveAll();
        }
        getLogger().info("RealiteClasses disabled.");
    }
}
