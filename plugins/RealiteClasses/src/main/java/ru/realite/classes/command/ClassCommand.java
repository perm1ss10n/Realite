package ru.realite.classes.command;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.classes.RealiteClassesPlugin;
import ru.realite.classes.core.CoreAccess;
import ru.realite.classes.event.EvolutionUnlockedEvent;
import ru.realite.classes.gui.ClassSettingsMenu;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EconomyService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.XpConfigRepository;
import ru.realite.classes.util.ChatTemplate;
import ru.realite.classes.util.ItemComponents;
import ru.realite.classes.util.Messages;

import java.util.Map;

import javax.annotation.Nullable;

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

    @Nullable
    private Player resolveTarget(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(messages.get("admin.player-not-found"));
        }
        return target;
    }

    @Nullable
    private ClassId resolveClassId(CommandSender sender, String raw) {
        ClassId classId = ClassId.fromString(raw);
        if (classId == null) {
            sender.sendMessage(messages.format(
                    "admin.unknown-class",
                    Map.of("class", raw)));
            return null;
        }
        return classId;
    }

    @Nullable
    private String resolveEvolutionId(
            CommandSender sender,
            ClassId classId,
            String rawId) {
        var def = classConfig.get(classId);
        if (def == null || def.evolutions == null || def.evolutions.isEmpty()) {
            sender.sendMessage(messages.get("admin.no-evolutions"));
            return null;
        }

        for (var evo : def.evolutions) {
            if (evo.id.equalsIgnoreCase(rawId)) {
                return evo.id; // нормализованный id
            }
        }

        sender.sendMessage(messages.format(
                "admin.unknown-evolution",
                Map.of("evolution", rawId)));
        return null;
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

            // админские подсказки показываем тем, у кого есть админ-права (а не reload)
            if (p.hasPermission("realiteclass.admin") || p.hasPermission("realiteclass.reload")) {
                for (String line : messages.getList("class-help-admin")) {
                    p.sendMessage(line);
                }
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            case "admin" -> {

                if (!p.hasPermission("realiteclass.admin")) {
                    p.sendMessage(messages.get("no-permission"));
                    return true;
                }

                if (args.length < 2) {
                    // usage-admin: "&eИспользование: &f/class admin
                    // <evolution|level|mastered|grant|preset|unlock-ready>"
                    p.sendMessage(messages.get("admin.usage"));
                    return true;
                }

                String adminSub = args[1].toLowerCase();
                switch (adminSub) {
                    case "grant" -> {
                        // usage-grant: "&eИспользование: &f/class admin grant <player> <classId>
                        // [evolutionId] [level]
                        if (args.length < 4) {
                            p.sendMessage(messages.get("admin-usage-grant"));
                            return true;
                        }

                        Player target = resolveTarget(p, args[2]);
                        if (target == null)
                            return true;

                        ClassId classId = resolveClassId(p, args[3]);
                        if (classId == null)
                            return true;

                        String evolutionId = null;
                        Integer level = null;

                        if (args.length >= 5) {
                            evolutionId = resolveEvolutionId(p, classId, args[4]);
                            if (evolutionId == null)
                                return true;
                        }

                        if (args.length >= 6) {
                            try {
                                level = Integer.parseInt(args[5]);
                                if (level < 0) {
                                    p.sendMessage(messages.get("admin.invalid-level"));
                                    return true;
                                }
                            } catch (NumberFormatException ex) {
                                p.sendMessage(messages.get("admin.invalid-level"));
                                return true;
                            }
                        }

                        // применяем через сервис
                        classService.assignClass(target, classId);

                        // применяем optional поля (если заданы)
                        PlayerProfile profTarget = classService.getProfile(target);

                        if (evolutionId != null) {
                            profTarget.setEvolution(evolutionId);
                            profTarget.setEvolutionRewardTaken(false);
                            profTarget.setEvolutionNotified(false);
                        }

                        if (level != null) {
                            profTarget.setClassLevel(level);
                        }

                        classService.save(profTarget);

                        // обновляем HUD у цели
                        var hud = plugin.getHudService();
                        if (hud != null)
                            hud.refreshNow(target);

                        String extra = "";
                        if (evolutionId != null && level != null)
                            extra = " &7" + messages.get("evolution") + ": &e" + evolutionId + "&7, "
                                    + messages.get("level") + ": &b" + level + "";
                        else if (evolutionId != null)
                            extra = " &7" + messages.get("evolution") + ": &e" + evolutionId + "";
                        else if (level != null)
                            extra = " &7" + messages.get("level") + ": &b" + level + "";

                        p.sendMessage(messages.format("admin.set.grant", Map.of(
                                "player", target.getName(),
                                "class", classId.name(),
                                "extra", extra)));

                        target.sendMessage(messages.format("admin.set-self.grant", Map.of(
                                "class", classId.name(),
                                "extra", extra)));

                        return true;
                    }

                    case "evolution" -> {
                        // admin-usage-evolution: "&eИспользование: &f/class admin evolution
                        // <set|next|max|reset> <player> [evolutionId]"

                        ClassId classId = resolveClassId(p, args[3]);
                        if (classId == null)
                            return true;

                        if (args.length < 4) {
                            p.sendMessage(messages.get("admin-usage-evolution"));
                            return true;
                        }

                        String action = args[2].toLowerCase();
                        Player target = resolveTarget(p, args[3]);
                        if (target == null)
                            return true;

                        PlayerProfile profTarget = classService.getProfile(target);
                        if (profTarget == null || !profTarget.hasClass()) {
                            p.sendMessage(messages.get("admin.no-class-selected"));
                            return true;
                        }

                        switch (action) {
                            case "set" -> {
                                if (args.length < 5) {
                                    p.sendMessage(messages.get("admin-usage-evolution"));
                                    return true;
                                }

                                String evolutionId = resolveEvolutionId(p, profTarget.getClassId(), args[4]);
                                if (evolutionId == null)
                                    return true;

                                profTarget.setEvolution(evolutionId);
                                profTarget.setEvolutionRewardTaken(false);
                                profTarget.setEvolutionNotified(false);
                                classService.save(profTarget);
                                plugin.getHudService().refreshNow(target);

                                p.sendMessage(messages.format("admin.set.evolution", Map.of(
                                        "player", target.getName(),
                                        "evolution", evolutionId)));

                                target.sendMessage(messages.format("admin.set-self.evolution", Map.of(
                                        "evolution", evolutionId)));
                                return true;
                            }

                            case "next" -> {
                                var nextEvo = evolutionService.getNextEvolution(profTarget);
                                if (nextEvo == null) {
                                    p.sendMessage(messages.get("admin.already-max-evolution"));
                                    return true;
                                }
                                profTarget.setEvolution(nextEvo.id);
                                profTarget.setEvolutionRewardTaken(false);
                                profTarget.setEvolutionNotified(false);
                                classService.save(profTarget);
                                plugin.getHudService().refreshNow(target);
                                p.sendMessage(messages.format("admin.next-evolution", Map.of(
                                        "player", target.getName(),
                                        "evolution", nextEvo.title)));

                                target.sendMessage(messages.format("admin.set-self.evolution", Map.of(
                                        "evolution", nextEvo.title)));
                                return true;
                            }

                            case "max" -> {
                                var def = classConfig.get(profTarget.getClassId());
                                if (def == null || def.evolutions == null || def.evolutions.isEmpty()) {
                                    p.sendMessage(messages.get("admin.no-evolutions"));
                                    return true;
                                }
                                String lastEvoId = evolutionService.getFinalEvolutionId(profTarget.getClassId());
                                profTarget.setEvolution(lastEvoId);
                                profTarget.setEvolutionRewardTaken(false);
                                profTarget.setEvolutionNotified(false);
                                classService.save(profTarget);
                                plugin.getHudService().refreshNow(target);

                                p.sendMessage(messages.format("admin.set.evolution", Map.of(
                                        "player", target.getName(),
                                        "evolution", lastEvoId)));

                                target.sendMessage(messages.format("admin.set-self.evolution", Map.of(
                                        "evolution", lastEvoId)));
                                return true;
                            }

                            case "reset" -> {
                                String firstEvoId = evolutionService.getFirstEvolutionId(profTarget.getClassId());
                                if (firstEvoId == null) {
                                    p.sendMessage(messages.get("admin.no-evolutions"));
                                    return true;
                                }
                                profTarget.setEvolution(firstEvoId);
                                profTarget.setEvolutionRewardTaken(false);
                                profTarget.setEvolutionNotified(false);

                                classService.save(profTarget);
                                plugin.getHudService().refreshNow(target);

                                p.sendMessage(messages.format("admin.evolution-reset", Map.of(
                                        "player", target.getName())));
                                target.sendMessage(messages.format("admin.set-self.evolution", Map.of(
                                        "evolution", firstEvoId)));
                                return true;
                            }

                            default -> {
                                p.sendMessage(messages.get("admin-usage-evolution"));
                                return true;
                            }
                        }
                    }

                    case "level" -> {
                        // admin-usage-level: "&eИспользование: &f/class admin level <set|add> <player>
                        // <level>"
                        if (args.length < 5) {
                            p.sendMessage(messages.get("admin-usage-level"));
                            return true;
                        }

                        Player target = resolveTarget(p, args[3]);
                        if (target == null)
                            return true;

                        String action = args[2].toLowerCase();
                        PlayerProfile profTarget = classService.getProfile(target);
                        if (profTarget == null || !profTarget.hasClass()) {
                            p.sendMessage(messages.get("admin.no-class-selected"));
                            return true;
                        }
                        int value;
                        try {
                            value = Integer.parseInt(args[4]);
                        } catch (NumberFormatException ex) {
                            p.sendMessage(messages.format("admin.invalid-level", Map.of(
                                    "level", args[4])));
                            return true;
                        }

                        if (value < 0) {
                            p.sendMessage(messages.format("admin.invalid-level", Map.of(
                                    "level", args[4])));
                            return true;
                        }

                        int oldLevel = profTarget.getClassLevel();
                        int newLevel;

                        switch (action) {
                            case "set" -> {
                                newLevel = value;
                            }

                            case "add" -> {
                                newLevel = oldLevel + value;
                            }
                            default -> {
                                p.sendMessage(messages.get("admin-usage-level"));
                                return true;
                            }
                        }
                        profTarget.setClassLevel(newLevel);

                        // ===== уведомление о доступной эволюции (как в ProgressionService) =====
                        var next = evolutionService.getNextEvolution(profTarget);
                        if (next != null && !profTarget.isEvolutionNotified()) {
                            if (oldLevel < next.requiredLevel && newLevel >= next.requiredLevel) {

                                // событие (если нужно для внешней логики)
                                CoreAccess.core().events().publish(
                                        new EvolutionUnlockedEvent(target.getUniqueId(), profTarget.getClassId(),
                                                next.id));

                                // сообщение игроку (можно тем же ключом, что и в ProgressionService, чтобы не
                                // плодить)
                                var def = classConfig.get(profTarget.getClassId());
                                String className = (def != null ? def.name : profTarget.getClassId().name());

                                String moneyText = (next.costMoney > 0) ? ("$" + (long) next.costMoney) : "0$";

                                ChatTemplate.sendWithComponent(
                                        target,
                                        messages.get("evolution-available"),
                                        Map.of(
                                                "class", className,
                                                "evolution", next.title,
                                                "required", String.valueOf(next.requiredLevel),
                                                "money", moneyText),
                                        "{items}",
                                        ItemComponents.listOrDash(next.costItems));

                                profTarget.setEvolutionNotified(true);
                            }
                        }

                        classService.save(profTarget);

                        plugin.getHudService().refreshNow(target);
                        p.sendMessage(messages.format("admin.set.level", Map.of(
                                "player", target.getName(),
                                "level", String.valueOf(newLevel))));
                        target.sendMessage(messages.format("admin.set-self.level", Map.of(
                                "level", String.valueOf(newLevel))));
                        return true;
                    }

                    case "mastered" -> {
                        // admin-usage-mastered: "&eИспользование: &f/class admin mastered
                        // <add|remove|list> <player> [classId]"
                        if (args.length < 4) {
                            p.sendMessage(messages.get("admin.usage-mastered"));
                            return true;
                        }

                        String action = args[2].toLowerCase();
                        Player target = resolveTarget(p, args[3]);
                        if (target == null)
                            return true;

                        PlayerProfile profTarget = classService.getProfile(target);
                        if (profTarget == null)
                            return true;

                        switch (action) {
                            case "add" -> {
                                if (args.length < 5) {
                                    p.sendMessage(messages.get("admin.usage-mastered"));
                                    return true;
                                }

                                ClassId classId = resolveClassId(p, args[4]);
                                if (classId == null)
                                    return true;

                                profTarget.addMastered(classId);
                                classService.save(profTarget);

                                p.sendMessage(messages.format("admin.set.mastery", Map.of(
                                        "player", target.getName(),
                                        "class", classId.name(),
                                        "mastered", messages.get("mastered-yes"))));
                                target.sendMessage(messages.format("admin.set-self.mastery", Map.of(
                                        "class", classId.name(),
                                        "mastered", messages.get("mastered-yes"))));
                                return true;
                            }
                            case "remove" -> {
                                if (args.length < 5) {
                                    p.sendMessage(messages.get("admin.usage-mastered"));
                                    return true;
                                }
                                ClassId classId = resolveClassId(p, args[4]);
                                if (classId == null)
                                    return true;

                                profTarget.removeMastered(classId);
                                classService.save(profTarget);

                                p.sendMessage(messages.format("admin.set.mastery", Map.of(
                                        "player", target.getName(),
                                        "class", classId.name(),
                                        "mastered", messages.get("mastered-no"))));
                                target.sendMessage(messages.format("admin.set-self.mastery", Map.of(
                                        "class", classId.name(),
                                        "mastered", messages.get("mastered-no"))));
                                return true;
                            }
                            case "list" -> {

                                var mastered = profTarget.getMasteredClasses();
                                if (mastered == null || mastered.isEmpty()) {
                                    p.sendMessage(messages.format("admin.mastered-empty", Map.of(
                                            "player", target.getName())));
                                    return true;
                                }

                                // Выводятся как displayname + classId
                                for (String classIdStr : mastered) {
                                    String display = classIdStr;

                                    ClassId cid = ClassId.fromString(classIdStr);
                                    if (cid != null) {
                                        var def = classConfig.get(cid);
                                        if (def != null && def.name != null) {
                                            display = def.name + " &7(" + classIdStr + ")";
                                        }
                                    }

                                    p.sendMessage(messages.format("admin.mastered-list-line", Map.of(
                                            "class", display)));
                                }
                                return true;
                            }
                            default -> {
                                p.sendMessage(messages.get("admin.usage-mastered"));
                                return true;
                            }
                        }
                    }
                    // TODO: Доделать preset, unlock-ready
                    case "preset" -> {
                        // admin-usage-preset: "&eИспользование: &f/class admin preset
                        // <list|apply|save|delete> [presetId] [player]"
                        p.sendMessage(messages.get("admin.usage-preset"));
                        return true;
                    }
                    case "unlock-ready" -> {
                        // admin-usage-unlock-ready: "&eИспользование: &f/class admin unlock-ready
                        // <player> <hiddenClassId>"
                        p.sendMessage(messages.get("admin.usage-unlock-ready"));
                        return true;
                    }
                    default -> {
                        // usage-admin: "&eИспользование: &f/class admin
                        // <evolution|level|mastered|grant|preset|unlock-ready>"
                        p.sendMessage(messages.get("admin.usage"));
                        return true;
                    }
                }
            }

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
                if (xpToNext == xpPerLevel)
                    xpToNext = 0;

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
                        "class", className)));

                var vars = Map.of(
                        "evolution", curTitle,
                        "level", String.valueOf(level),
                        "xp", String.valueOf(xp),
                        "xpToNext", String.valueOf(xpToNext),
                        "nextEvolution", nextTitle,
                        "nextRequired", nextReq,
                        "nextCostMoney", nextCostMoney,
                        "mastered", masteredText);

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
                                itemsComponent);
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
                            "evolution", evoTitle)));
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
