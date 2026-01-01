package ru.realite.classes.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.classes.RealiteClassesPlugin;
import ru.realite.classes.gui.ClassSettingsMenu;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EconomyService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.TesterPresetRepository;
import ru.realite.classes.storage.XpConfigRepository;
import ru.realite.classes.util.ChatTemplate;
import ru.realite.classes.util.ItemComponents;
import ru.realite.classes.util.Messages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClassCommand implements CommandExecutor {

    private final RealiteClassesPlugin plugin;

    private final ClassService classService;
    private final EvolutionService evolutionService;

    private final ClassConfigRepository classConfig;
    private final EconomyService economy;
    private final Messages messages;
    private final TesterPresetRepository testerPresets;

    @SuppressWarnings("unused")
    private final XpConfigRepository xpConfig;

    public ClassCommand(RealiteClassesPlugin plugin,
                        ClassService classService,
                        EvolutionService evolutionService,
                        ClassConfigRepository classConfig,
                        EconomyService economy,
                        Messages messages,
                        XpConfigRepository xpConfig,
                        TesterPresetRepository testerPresets) {
        this.plugin = plugin;
        this.classService = classService;
        this.evolutionService = evolutionService;
        this.classConfig = classConfig;
        this.economy = economy;
        this.messages = messages;
        this.xpConfig = xpConfig;
        this.testerPresets = testerPresets;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && "admin".equalsIgnoreCase(args[0])) {
            return handleAdmin(sender, args);
        }

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

    private boolean handleAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission("realite.classes.admin")) {
            sender.sendMessage(messages.get("no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(messages.get("admin-usage"));
            return true;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "evolution" -> {
                return handleAdminEvolution(sender, args);
            }
            case "level" -> {
                return handleAdminLevel(sender, args);
            }
            case "mastered" -> {
                return handleAdminMastered(sender, args);
            }
            case "grant" -> {
                return handleAdminGrant(sender, args);
            }
            case "preset" -> {
                return handleAdminPreset(sender, args);
            }
            case "unlock-ready" -> {
                return handleAdminUnlockReady(sender, args);
            }
            default -> {
                sender.sendMessage(messages.get("admin-usage"));
                return true;
            }
        }
    }

    private boolean handleAdminEvolution(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(messages.get("admin-usage-evolution"));
            return true;
        }

        String action = args[2].toLowerCase();
        Player target = resolvePlayer(sender, args[3]);
        if (target == null) {
            return true;
        }

        var profile = classService.getProfile(target);
        if (profile == null || !profile.hasClass()) {
            sender.sendMessage(messages.get("no-class-selected"));
            return true;
        }

        var def = classConfig.get(profile.getClassId());
        if (def == null) {
            sender.sendMessage(messages.get("unknown-class"));
            return true;
        }

        switch (action) {
            case "set" -> {
                if (args.length < 5) {
                    sender.sendMessage(messages.get("admin-usage-evolution"));
                    return true;
                }
                String evolutionId = args[4];
                if (!forceSetEvolution(profile, def, evolutionId)) {
                    sender.sendMessage(messages.get("unknown-evolution"));
                    return true;
                }
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            case "next" -> {
                var next = evolutionService.getNextEvolution(profile);
                if (next == null) {
                    sender.sendMessage(messages.get("evolve-already-max"));
                    return true;
                }
                forceSetEvolution(profile, def, next.id);
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            case "max" -> {
                var fin = def.finalEvolution();
                if (fin == null) {
                    sender.sendMessage(messages.get("unknown-evolution"));
                    return true;
                }
                forceSetEvolution(profile, def, fin.id);
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            case "reset" -> {
                var first = def.firstEvolution();
                if (first == null) {
                    sender.sendMessage(messages.get("unknown-evolution"));
                    return true;
                }
                forceSetEvolution(profile, def, first.id);
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            default -> {
                sender.sendMessage(messages.get("admin-usage-evolution"));
                return true;
            }
        }
    }

    private boolean handleAdminLevel(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(messages.get("admin-usage-level"));
            return true;
        }

        String action = args[2].toLowerCase();
        Player target = resolvePlayer(sender, args[3]);
        if (target == null) {
            return true;
        }

        var profile = classService.getProfile(target);
        if (profile == null || !profile.hasClass()) {
            sender.sendMessage(messages.get("no-class-selected"));
            return true;
        }

        var def = classConfig.get(profile.getClassId());
        if (def == null) {
            sender.sendMessage(messages.get("unknown-class"));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("admin-usage-level"));
            return true;
        }

        switch (action) {
            case "set" -> {
                applyLevel(profile, def, amount);
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            case "add" -> {
                applyLevel(profile, def, profile.getClassLevel() + amount);
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            default -> {
                sender.sendMessage(messages.get("admin-usage-level"));
                return true;
            }
        }
    }

    private boolean handleAdminMastered(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(messages.get("admin-usage-mastered"));
            return true;
        }

        String action = args[2].toLowerCase();
        Player target = resolvePlayer(sender, args[3]);
        if (target == null) {
            return true;
        }

        var profile = classService.getProfile(target);
        if (profile == null) {
            sender.sendMessage(messages.get("admin-success"));
            return true;
        }

        switch (action) {
            case "list" -> {
                List<String> names = new ArrayList<>();
                for (String id : profile.getMasteredClasses()) {
                    var classId = ru.realite.classes.model.ClassId.fromString(id);
                    if (classId == null) {
                        names.add(id);
                        continue;
                    }
                    var def = classConfig.get(classId);
                    names.add(def != null ? def.name : classId.name());
                }
                String list = names.isEmpty() ? "-" : String.join(", ", names);
                sender.sendMessage(messages.format("mastered-list", Map.of("list", list)));
                return true;
            }
            case "add", "remove" -> {
                if (args.length < 5) {
                    sender.sendMessage(messages.get("admin-usage-mastered"));
                    return true;
                }
                var classId = ru.realite.classes.model.ClassId.fromString(args[4]);
                if (classId == null) {
                    sender.sendMessage(messages.get("unknown-class"));
                    return true;
                }
                if ("add".equals(action)) {
                    profile.addMastered(classId);
                } else {
                    profile.getMasteredClasses().remove(classId.name());
                }
                classService.save(profile);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            default -> {
                sender.sendMessage(messages.get("admin-usage-mastered"));
                return true;
            }
        }
    }

    private boolean handleAdminGrant(CommandSender sender, String[] args) {
        if (!ensureTesterMode(sender)) {
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(messages.get("admin-usage-grant"));
            return true;
        }

        Player target = resolvePlayer(sender, args[2]);
        if (target == null) {
            return true;
        }

        var classId = ru.realite.classes.model.ClassId.fromString(args[3]);
        if (classId == null) {
            sender.sendMessage(messages.get("unknown-class"));
            return true;
        }

        var def = classConfig.get(classId);
        if (def == null) {
            sender.sendMessage(messages.get("unknown-class"));
            return true;
        }

        var profile = classService.getProfile(target);
        if (profile == null) {
            sender.sendMessage(messages.get("admin-success"));
            return true;
        }

        if (!profile.hasClass() || profile.getClassId() != classId) {
            classService.assignClass(target, classId);
            profile = classService.getProfile(target);
        }

        if (args.length >= 5) {
            String evolutionId = args[4];
            if (!forceSetEvolution(profile, def, evolutionId)) {
                sender.sendMessage(messages.get("unknown-evolution"));
                return true;
            }
        }

        if (args.length >= 6) {
            int level;
            try {
                level = Integer.parseInt(args[5]);
            } catch (NumberFormatException e) {
                sender.sendMessage(messages.get("admin-usage-grant"));
                return true;
            }
            applyLevel(profile, def, level);
        }

        classService.save(profile);
        sender.sendMessage(messages.get("admin-success"));
        return true;
    }

    private boolean handleAdminPreset(CommandSender sender, String[] args) {
        if (!ensureTesterMode(sender)) {
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(messages.get("admin-usage-preset"));
            return true;
        }

        String action = args[2].toLowerCase();
        switch (action) {
            case "list" -> {
                String list = String.join(", ", testerPresets.listIds());
                if (list.isBlank()) {
                    list = "-";
                }
                sender.sendMessage(messages.format("preset-list", Map.of("list", list)));
                return true;
            }
            case "apply" -> {
                if (args.length < 5) {
                    sender.sendMessage(messages.get("admin-usage-preset"));
                    return true;
                }
                String presetId = args[3];
                Player target = resolvePlayer(sender, args[4]);
                if (target == null) {
                    return true;
                }
                var preset = testerPresets.get(presetId);
                if (preset == null) {
                    sender.sendMessage(messages.get("preset-not-found"));
                    return true;
                }
                applyPreset(target, preset);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            case "save" -> {
                if (args.length < 5) {
                    sender.sendMessage(messages.get("admin-usage-preset"));
                    return true;
                }
                String presetId = args[3];
                Player target = resolvePlayer(sender, args[4]);
                if (target == null) {
                    return true;
                }
                savePreset(presetId, target);
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            case "delete" -> {
                if (args.length < 4) {
                    sender.sendMessage(messages.get("admin-usage-preset"));
                    return true;
                }
                testerPresets.remove(args[3]);
                testerPresets.save();
                sender.sendMessage(messages.get("admin-success"));
                return true;
            }
            default -> {
                sender.sendMessage(messages.get("admin-usage-preset"));
                return true;
            }
        }
    }

    private boolean handleAdminUnlockReady(CommandSender sender, String[] args) {
        if (!ensureTesterMode(sender)) {
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(messages.get("admin-usage-unlock-ready"));
            return true;
        }

        Player target = resolvePlayer(sender, args[2]);
        if (target == null) {
            return true;
        }

        var classId = ru.realite.classes.model.ClassId.fromString(args[3]);
        if (classId == null) {
            sender.sendMessage(messages.get("unknown-class"));
            return true;
        }

        var def = classConfig.get(classId);
        if (def == null) {
            sender.sendMessage(messages.get("unknown-class"));
            return true;
        }

        var profile = classService.getProfile(target);
        if (profile == null) {
            sender.sendMessage(messages.get("admin-success"));
            return true;
        }

        if (def.requiresMastered != null) {
            for (var req : def.requiresMastered) {
                profile.addMastered(req);
            }
        }

        classService.save(profile);
        sender.sendMessage(messages.get("admin-success"));
        return true;
    }

    private Player resolvePlayer(CommandSender sender, String name) {
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            sender.sendMessage(messages.get("unknown-player"));
        }
        return target;
    }

    private boolean ensureTesterMode(CommandSender sender) {
        if (!sender.hasPermission("realite.classes.admin.tester")) {
            sender.sendMessage(messages.get("no-permission"));
            return false;
        }
        boolean enabled = plugin.getConfig().getBoolean("testerMode.enabled", false);
        if (!enabled) {
            sender.sendMessage(messages.get("tester-disabled"));
            return false;
        }
        return true;
    }

    private boolean forceSetEvolution(ru.realite.classes.model.PlayerProfile profile,
                                      ClassConfigRepository.ClassDef def,
                                      String evolutionId) {
        if (def.findEvolution(evolutionId) == null) {
            return false;
        }

        profile.setEvolution(evolutionId);

        var fin = def.finalEvolution();
        if (fin != null && fin.id.equalsIgnoreCase(evolutionId)) {
            profile.addMastered(profile.getClassId());
        }
        return true;
    }

    private void applyLevel(ru.realite.classes.model.PlayerProfile profile,
                            ClassConfigRepository.ClassDef def,
                            int level) {
        int safeLevel = Math.max(0, level);
        profile.setClassLevel(safeLevel);
        int xpPerLevel = Math.max(1, def.xpPerLevel);
        profile.setClassXp((long) safeLevel * xpPerLevel);

        int prevMax = profile.getMaxLevelByClass().getOrDefault(profile.getClassId().name(), 0);
        if (safeLevel > prevMax) {
            profile.getMaxLevelByClass().put(profile.getClassId().name(), safeLevel);
        }
    }

    private void applyPreset(Player target, TesterPresetRepository.TesterPreset preset) {
        var profile = classService.getProfile(target);
        if (profile == null) {
            return;
        }

        if (preset.classId() != null) {
            classService.assignClass(target, preset.classId());
            profile = classService.getProfile(target);
        }

        if (profile != null && preset.evolutionId() != null && profile.hasClass()) {
            var def = classConfig.get(profile.getClassId());
            if (def != null) {
                forceSetEvolution(profile, def, preset.evolutionId());
            }
        }

        if (profile != null && preset.level() != null && profile.hasClass()) {
            var def = classConfig.get(profile.getClassId());
            if (def != null) {
                applyLevel(profile, def, preset.level());
            }
        }

        if (profile != null && preset.mastered() != null) {
            for (var id : preset.mastered()) {
                profile.addMastered(id);
            }
        }

        if (profile != null) {
            classService.save(profile);
        }
    }

    private void savePreset(String presetId, Player target) {
        var profile = classService.getProfile(target);
        if (profile == null) {
            return;
        }

        ru.realite.classes.model.ClassId classId = profile.getClassId();
        String evolutionId = profile.getEvolution();
        Integer level = profile.hasClass() ? profile.getClassLevel() : null;

        Set<ru.realite.classes.model.ClassId> mastered = new HashSet<>();
        for (String id : profile.getMasteredClasses()) {
            var cid = ru.realite.classes.model.ClassId.fromString(id);
            if (cid != null) {
                mastered.add(cid);
            }
        }

        testerPresets.set(presetId, new TesterPresetRepository.TesterPreset(
                classId,
                evolutionId,
                level,
                mastered));
        testerPresets.save();
    }
}
