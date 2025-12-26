package ru.realite.classes.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.classes.RealiteClassesPlugin;
import ru.realite.classes.gui.ClassSettingsMenu;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EconomyService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.XpConfigRepository;
import ru.realite.classes.util.ChatTemplate;
import ru.realite.classes.util.ItemComponents;
import ru.realite.classes.util.Messages;

import java.util.Map;

public class ClassCommand implements CommandExecutor {

    private final RealiteClassesPlugin plugin;

    private final ClassService classService;
    private final EvolutionService evolutionService;

    private final ClassConfigRepository classConfig;
    private final EconomyService economy;
    private final Messages messages;

    @SuppressWarnings("unused")
    private final XpConfigRepository xpConfig;

    public ClassCommand(RealiteClassesPlugin plugin,
                        ClassService classService,
                        EvolutionService evolutionService,
                        ClassConfigRepository classConfig,
                        EconomyService economy,
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
        if (prof == null)
            return true;

        if (args.length == 0) {
            p.sendMessage(messages.get("class-help-header"));

            for (String line : messages.getList("class-help")) {
                p.sendMessage(line);
            }

            if (p.hasPermission("realiteclass.reload")) {
                for (String line : messages.getList("class-help-admin")) {
                    p.sendMessage(line);
                }
            }
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
                boolean isWanderer = prof.getClassId() == ru.realite.classes.model.ClassId.WANDERER;

                if (prof.hasClass() && !isWanderer) {
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

            case "settings" -> {
                new ClassSettingsMenu().open(p);
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

                String nextCostMoney = "-";
                if (next != null) {
                    nextCostMoney = String.valueOf((long) Math.max(0, next.costMoney));
                }

                boolean mastered = prof.hasMastered(prof.getClassId());
                String masteredText = mastered ? messages.get("mastered-yes") : messages.get("mastered-no");

                p.sendMessage(messages.format("info-header", Map.of(
                        "class", className
                )));

                var vars = Map.of(
                        "evolution", curTitle,
                        "level", String.valueOf(level),
                        "xp", String.valueOf(xp),
                        "xpToNext", String.valueOf(xpToNext),
                        "nextEvolution", nextTitle,
                        "nextRequired", nextReq,
                        "nextCostMoney", nextCostMoney,
                        "mastered", masteredText
                );

                var itemsComponent = (next != null)
                        ? ItemComponents.listOrDash(next.costItems)
                        : ItemComponents.listOrDash(null);

                for (String line : messages.getList("info-body")) {
                    if (line.contains("{nextCostItems}")) {
                        ChatTemplate.sendWithComponent(
                                p,
                                line,
                                vars,
                                "{nextCostItems}",
                                itemsComponent
                        );
                    } else {
                        p.sendMessage(messages.formatLine(line, vars));
                    }
                }
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
