package ru.realite.classes.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.classes.RealiteClassesPlugin;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EconomyService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.XpConfigRepository;
import ru.realite.classes.util.Messages;

import java.util.Map;

public class ClassCommand implements CommandExecutor {

    private final RealiteClassesPlugin plugin;

    private final ClassService classService;
    private final EvolutionService evolutionService;

    private final ClassConfigRepository classConfig;
    private final EconomyService economy;
    private final Messages messages;

    // xpConfig сейчас не обязателен в команде, но пусть будет под рукой
    @SuppressWarnings("unused")
    private final XpConfigRepository xpConfig;

    public ClassCommand(RealiteClassesPlugin plugin,
                        ClassService classService,
                        EvolutionService evolutionService,
                        ClassConfigRepository classConfig,
                        EconomyService economy,
                        ClassSelectMenu menuIgnored,
                        Messages messages,
                        XpConfigRepository xpConfig) {
        this.plugin = plugin;
        this.classService = classService;
        this.evolutionService = evolutionService;
        this.classConfig = classConfig;
        this.economy = economy;
        this.messages = messages;
        this.xpConfig = xpConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        var prof = classService.getProfile(p);
        if (prof == null) return true;

        // /class
        if (args.length == 0) {
            if (!prof.hasClass()) {
                p.sendMessage(messages.get("no-class"));
                return true;
            }

            var def = classConfig.get(prof.getClassId());
            String className = (def != null ? def.name : prof.getClassId().name());

            var cur = evolutionService.getCurrentEvolution(prof);
            String evoTitle = (cur != null ? cur.title : "-");

            p.sendMessage(messages.format("status", Map.of(
                    "class", className,
                    "evolution", evoTitle
            )));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                if (!p.hasPermission("realiteclass.reload")) {
                    p.sendMessage(messages.get("no-permission"));
                    return true;
                }
                plugin.reloadAll();
                p.sendMessage(messages.get("reloaded"));
                return true;
            }

            case "choose" -> {
                if (prof.hasClass()) {
                    p.sendMessage(messages.get("already-chosen"));
                    return true;
                }
                p.openInventory(plugin.getMenu().create());
                return true;
            }

            case "change" -> {
                if (!evolutionService.canChangeClass(p, prof)) {
                    p.sendMessage(messages.get("cant-change"));
                    return true;
                }
                p.openInventory(plugin.getMenu().create());
                return true;
            }

            case "info" -> {
                if (!prof.hasClass()) {
                    p.sendMessage(messages.get("no-class"));
                    return true;
                }

                var def = classConfig.get(prof.getClassId());
                String className = (def != null ? def.name : prof.getClassId().name());

                long xp = prof.getClassXp();
                int level = prof.getClassLevel();

                int xpPerLevel = (def != null ? Math.max(1, def.xpPerLevel) : 100);
                long xpToNext = xpPerLevel - (xp % xpPerLevel);
                if (xpToNext == xpPerLevel) xpToNext = 0;

                var cur = evolutionService.getCurrentEvolution(prof);
                String curTitle = (cur != null ? cur.title : "-");

                var next = evolutionService.getNextEvolution(prof);
                String nextTitle = (next != null ? next.title : "-");
                String nextReq = (next != null ? String.valueOf(next.requiredLevel) : "-");

                boolean mastered = prof.hasMastered(prof.getClassId());
                String masteredText = mastered ? messages.get("mastered-yes") : messages.get("mastered-no");

                p.sendMessage("§6§lКласс: §e" + className);
                p.sendMessage("§7Эволюция: §a" + curTitle);
                p.sendMessage("§7Уровень: §b" + level);
                p.sendMessage("§7XP: §b" + xp + " §7/ до след. ур: §b" + xpToNext);
                p.sendMessage("§7След. эволюция: §e" + nextTitle + " §7(ур. " + nextReq + ")");
                p.sendMessage("§7Мастерство: " + masteredText);
                return true;
            }

            case "evolve" -> {
                if (!prof.hasClass()) {
                    p.sendMessage(messages.get("no-class"));
                    return true;
                }

                String res = evolutionService.evolve(p, prof, economy);
                if ("ok".equals(res)) {
                    classService.save(prof);

                    var def = classConfig.get(prof.getClassId());
                    String className = (def != null ? def.name : prof.getClassId().name());

                    var cur = evolutionService.getCurrentEvolution(prof);
                    String evoTitle = (cur != null ? cur.title : prof.getEvolution());

                    p.sendMessage(messages.format("evolved", Map.of(
                            "class", className,
                            "evolution", evoTitle
                    )));
                } else {
                    p.sendMessage(messages.get("evolve-" + res));
                }
                return true;
            }

            default -> {
                p.sendMessage(messages.get("usage"));
                return true;
            }
        }
    }
}
