package ru.realite.city.command;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.realite.city.model.CityArea;
import ru.realite.city.service.CityAreaSelectionService;
import ru.realite.city.service.CityAreaSelectionService.Selection;
import ru.realite.city.storage.CityAreaRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class CityCommand implements CommandExecutor {

    private static final String ADMIN_PERMISSION = "realite.city.admin";

    private final CityAreaRepository repository;
    private final CityAreaSelectionService selectionService;

    public CityCommand(CityAreaRepository repository, CityAreaSelectionService selectionService) {
        this.repository = repository;
        this.selectionService = selectionService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }
        if (args.length < 1) {
            sendUsage(player);
            return true;
        }
        if (!"area".equalsIgnoreCase(args[0])) {
            sendUsage(player);
            return true;
        }
        if (args.length < 2) {
            sendUsage(player);
            return true;
        }
        String action = args[1].toLowerCase();
        switch (action) {
            case "wand" -> {
                selectionService.enableWand(player.getUniqueId());
                player.sendMessage("City area wand enabled.");
            }
            case "create" -> handleCreate(player, args);
            case "delete" -> handleDelete(player, args);
            case "list" -> handleList(player);
            default -> sendUsage(player);
        }
        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("Usage: /city area create <id>");
            return;
        }
        String id = args[2];
        Optional<CityArea> existing = repository.findById(id);
        if (existing.isPresent()) {
            player.sendMessage("CityArea " + id + " already exists.");
            return;
        }
        Optional<Selection> selection = selectionService.getSelection(player.getUniqueId());
        if (selection.isEmpty() || selection.get().pos1() == null || selection.get().pos2() == null) {
            player.sendMessage("Both pos1 and pos2 must be set.");
            return;
        }
        Location pos1 = selection.get().pos1();
        Location pos2 = selection.get().pos2();
        if (pos1.getWorld() == null || pos2.getWorld() == null) {
            player.sendMessage("Invalid selection world.");
            return;
        }
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            player.sendMessage("pos1 and pos2 must be in the same world.");
            return;
        }
        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
        CityArea area = new CityArea(
                id,
                pos1.getWorld().getName(),
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                System.currentTimeMillis()
        );
        repository.upsert(area);
        player.sendMessage("CityArea " + id + " created.");
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage("Usage: /city area delete <id>");
            return;
        }
        String id = args[2];
        if (repository.delete(id)) {
            player.sendMessage("CityArea " + id + " deleted.");
        } else {
            player.sendMessage("CityArea " + id + " not found.");
        }
    }

    private void handleList(Player player) {
        List<CityArea> areas = repository.findAll();
        if (areas.isEmpty()) {
            player.sendMessage("No city areas defined.");
            return;
        }
        areas.sort(Comparator.comparing(CityArea::id));
        player.sendMessage("City areas:");
        for (CityArea area : areas) {
            player.sendMessage("- " + area.id()
                    + " (" + area.world() + ") "
                    + "[" + area.minX() + "," + area.minY() + "," + area.minZ()
                    + "] -> [" + area.maxX() + "," + area.maxY() + "," + area.maxZ() + "]");
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage("Usage: /city area <wand|create|delete|list>");
    }
}
