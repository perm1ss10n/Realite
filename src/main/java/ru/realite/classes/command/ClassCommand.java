package ru.realite.classes.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.classes.gui.ClassSelectMenu;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.util.Messages;

import java.util.Map;

public class ClassCommand implements CommandExecutor {

    private final ClassService classService;
    private final EvolutionService evolutionService;
    private final ClassSelectMenu menu;
    private final Messages messages;

    public ClassCommand(ClassService classService, EvolutionService evolutionService, ClassSelectMenu menu, Messages messages) {
        this.classService = classService;
        this.evolutionService = evolutionService;
        this.menu = menu;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(messages.get("only-players"));
            return true;
        }

        var prof = classService.getProfile(p);

        if (args.length == 0) {
            if (!prof.hasClass()) {
                p.sendMessage(messages.get("no-class"));
                return true;
            }

            String evo = (prof.getEvolution() == null || prof.getEvolution().isEmpty()) ? "-" : prof.getEvolution();
            p.sendMessage(messages.format("status", Map.of(
                    "class", prof.getClassId().name(),
                    "evolution", evo
            )));
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "choose" -> {
                if (prof.hasClass()) {
                    p.sendMessage(messages.get("already-chosen"));
                    return true;
                }
                p.openInventory(menu.create());
                return true;
            }
            case "change" -> {
                if (!evolutionService.canChangeClass(p, prof)) {
                    p.sendMessage(messages.get("cant-change"));
                    return true;
                }
                p.openInventory(menu.create());
                return true;
            }
            default -> {
                p.sendMessage(messages.get("usage"));
                return true;
            }
        }
    }
}
