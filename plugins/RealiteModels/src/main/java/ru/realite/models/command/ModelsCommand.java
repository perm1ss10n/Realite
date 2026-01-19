package ru.realite.models.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import ru.realite.core.api.models.ApplyResult;
import ru.realite.core.api.models.ModelAssetInfo;
import ru.realite.core.api.models.ModelAssetRegistry;
import ru.realite.core.api.models.ModelOffset;
import ru.realite.core.api.models.ModelVisualProfile;
import ru.realite.models.config.ModelsConfig;
import ru.realite.models.service.ModelWrapperService;

public final class ModelsCommand implements CommandExecutor, TabCompleter {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Supplier<ModelAssetRegistry> registrySupplier;
    private final ModelWrapperService wrapperService;
    private final Supplier<ModelsConfig> configSupplier;

    public ModelsCommand(Supplier<ModelAssetRegistry> registrySupplier,
                         ModelWrapperService wrapperService,
                         Supplier<ModelsConfig> configSupplier) {
        this.registrySupplier = Objects.requireNonNull(registrySupplier, "registrySupplier");
        this.wrapperService = Objects.requireNonNull(wrapperService, "wrapperService");
        this.configSupplier = Objects.requireNonNull(configSupplier, "configSupplier");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "apply" -> {
                if (args.length < 3) {
                    sender.sendMessage(miniMessage.deserialize("<red>Usage: /models apply <modelId> <target></red>"));
                    return true;
                }
                String modelId = args[1];
                List<Entity> targets = resolveTargets(sender, args[2]);
                if (targets.isEmpty()) {
                    sender.sendMessage(miniMessage.deserialize("<red>No targets found.</red>"));
                    return true;
                }
                int applied = 0;
                for (Entity target : targets) {
                    ApplyResult result = wrapperService.applyModel(target, modelId);
                    if (result.isApplied()) {
                        applied++;
                    } else {
                        sender.sendMessage(miniMessage.deserialize(
                                "<red>Failed to apply model to <target>: <reason></red>",
                                Placeholder.parsed("target", describeTarget(target)),
                                Placeholder.parsed("reason", result.message())));
                    }
                }
                sender.sendMessage(miniMessage.deserialize(
                        "<green>Applied model <modelId> to <count> target(s).</green>",
                        Placeholder.parsed("modelId", modelId),
                        Placeholder.parsed("count", Integer.toString(applied))));
                return true;
            }
            case "clear" -> {
                if (args.length < 2) {
                    sender.sendMessage(miniMessage.deserialize("<red>Usage: /models clear <target></red>"));
                    return true;
                }
                List<Entity> targets = resolveTargets(sender, args[1]);
                if (targets.isEmpty()) {
                    sender.sendMessage(miniMessage.deserialize("<red>No targets found.</red>"));
                    return true;
                }
                for (Entity target : targets) {
                    wrapperService.clearModel(target);
                }
                sender.sendMessage(miniMessage.deserialize(
                        "<green>Cleared models for <count> target(s).</green>",
                        Placeholder.parsed("count", Integer.toString(targets.size()))));
                return true;
            }
            case "info" -> {
                if (args.length < 2) {
                    sender.sendMessage(miniMessage.deserialize("<red>Usage: /models info <target></red>"));
                    return true;
                }
                List<Entity> targets = resolveTargets(sender, args[1]);
                if (targets.isEmpty()) {
                    sender.sendMessage(miniMessage.deserialize("<red>No targets found.</red>"));
                    return true;
                }
                for (Entity target : targets) {
                    Optional<ModelWrapperService.ModelInstanceInfo> info = wrapperService.getModelInfo(target);
                    if (info.isEmpty()) {
                        sender.sendMessage(miniMessage.deserialize(
                                "<gray>No model applied on <target>.</gray>",
                                Placeholder.parsed("target", describeTarget(target))));
                        continue;
                    }
                    sendModelInfo(sender, target, info.get());
                }
                return true;
            }
            case "debug" -> {
                if (args.length < 2 || !"asset".equalsIgnoreCase(args[1])) {
                    sendHelp(sender);
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(miniMessage.deserialize("<red>Usage: /models debug asset <modelId></red>"));
                    return true;
                }
                String modelId = args[2];
                ModelAssetRegistry registry = registrySupplier.get();
                if (registry == null) {
                    sender.sendMessage(miniMessage.deserialize("<red>Model assets registry is not available.</red>"));
                    return true;
                }
                Optional<ModelAssetInfo> assetInfo = registry.find(modelId);
                if (assetInfo.isEmpty()) {
                    sender.sendMessage(miniMessage.deserialize(
                            "<red>Model asset not found: <modelId></red>",
                            Placeholder.parsed("modelId", modelId)));
                    return true;
                }
                sendAssetInfo(sender, assetInfo.get());
                return true;
            }
            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matchPrefix(args[0], List.of("apply", "clear", "info", "debug"));
        }
        if (args.length == 2 && "apply".equalsIgnoreCase(args[0])) {
            ModelsConfig config = configSupplier.get();
            if (config == null) {
                return Collections.emptyList();
            }
            return matchPrefix(args[1], new ArrayList<>(config.all().keySet()));
        }
        if (args.length == 2 && ("clear".equalsIgnoreCase(args[0]) || "info".equalsIgnoreCase(args[0]))) {
            return matchPrefix(args[1], List.of("nearest"));
        }
        if (args.length == 2 && "debug".equalsIgnoreCase(args[0])) {
            return matchPrefix(args[1], List.of("asset"));
        }
        if (args.length == 3 && "debug".equalsIgnoreCase(args[0]) && "asset".equalsIgnoreCase(args[1])) {
            ModelAssetRegistry registry = registrySupplier.get();
            if (registry == null) {
                return Collections.emptyList();
            }
            return matchPrefix(args[2], new ArrayList<>(registry.all().keySet()));
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(miniMessage.deserialize("<gray>Models commands:</gray>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/models apply <modelId> <target></yellow>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/models clear <target></yellow>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/models info <target></yellow>"));
        sender.sendMessage(miniMessage.deserialize("<yellow>/models debug asset <modelId></yellow>"));
    }

    private void sendAssetInfo(CommandSender sender, ModelAssetInfo info) {
        ModelVisualProfile profile = info.asset().visualProfile();
        ModelOffset offset = profile.offset();
        Component header = miniMessage.deserialize(
                "<gray>Model asset <white><modelId></white></gray>",
                Placeholder.parsed("modelId", info.asset().modelId()));
        Component source = miniMessage.deserialize(
                "<gray>Source:</gray> <white><source></white>",
                Placeholder.parsed("source", info.source()));
        Component kind = miniMessage.deserialize(
                "<gray>Kind:</gray> <white><kind></white>",
                Placeholder.parsed("kind", info.asset().kind().name()));
        Component renderer = miniMessage.deserialize(
                "<gray>Renderer hint:</gray> <white><hint></white>",
                Placeholder.parsed("hint", info.asset().rendererHint().name()));
        Component visual = miniMessage.deserialize(
                "<gray>Visual:</gray> <white>scale=<scale> offset=(<x>, <y>, <z>) anchor=<anchor></white>",
                Placeholder.parsed("scale", formatDouble(profile.scale())),
                Placeholder.parsed("x", formatDouble(offset.x())),
                Placeholder.parsed("y", formatDouble(offset.y())),
                Placeholder.parsed("z", formatDouble(offset.z())),
                Placeholder.parsed("anchor", profile.anchor()));
        sender.sendMessage(header);
        sender.sendMessage(source);
        sender.sendMessage(kind);
        sender.sendMessage(renderer);
        sender.sendMessage(visual);
    }

    private void sendModelInfo(CommandSender sender, Entity target, ModelWrapperService.ModelInstanceInfo info) {
        Component header = miniMessage.deserialize(
                "<gray>Model info for <target>:</gray>",
                Placeholder.parsed("target", describeTarget(target)));
        Component modelId = miniMessage.deserialize(
                "<gray>Model ID:</gray> <white><modelId></white>",
                Placeholder.parsed("modelId", info.modelId()));
        String attachmentId = info.attachmentId() == null ? "-" : info.attachmentId().toString();
        Component attachment = miniMessage.deserialize(
                "<gray>Attachment UUID:</gray> <white><uuid></white>",
                Placeholder.parsed("uuid", attachmentId));
        Component version = miniMessage.deserialize(
                "<gray>Version:</gray> <white><version></white>",
                Placeholder.parsed("version", Integer.toString(info.version())));
        sender.sendMessage(header);
        sender.sendMessage(modelId);
        sender.sendMessage(attachment);
        sender.sendMessage(version);
    }

    private List<Entity> resolveTargets(CommandSender sender, String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        if ("nearest".equalsIgnoreCase(raw) && sender instanceof Player player) {
            return findNearestHorse(player)
                    .map(List::of)
                    .orElseGet(Collections::emptyList);
        }
        if (raw.startsWith("@")) {
            try {
                return Bukkit.selectEntities(sender, raw);
            } catch (IllegalArgumentException ex) {
                return Collections.emptyList();
            }
        }
        Optional<UUID> uuid = parseUuid(raw);
        if (uuid.isPresent()) {
            Entity entity = Bukkit.getEntity(uuid.get());
            if (entity != null) {
                return List.of(entity);
            }
        }
        return Collections.emptyList();
    }

    private Optional<Entity> findNearestHorse(Player player) {
        double closest = Double.MAX_VALUE;
        Horse nearest = null;
        for (Horse horse : player.getWorld().getEntitiesByClass(Horse.class)) {
            double dist = horse.getLocation().distanceSquared(player.getLocation());
            if (dist < closest) {
                closest = dist;
                nearest = horse;
            }
        }
        return Optional.ofNullable(nearest);
    }

    private Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private String describeTarget(Entity target) {
        String name = target.getCustomName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return target.getType().name().toLowerCase(Locale.ROOT) + ":" + target.getUniqueId();
    }

    private List<String> matchPrefix(String raw, List<String> candidates) {
        if (raw == null || raw.isBlank()) {
            return candidates;
        }
        String needle = raw.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String candidate : candidates) {
            if (candidate.toLowerCase(Locale.ROOT).startsWith(needle)) {
                matches.add(candidate);
            }
        }
        return matches;
    }

    private String formatDouble(double value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
