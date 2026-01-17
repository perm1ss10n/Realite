package ru.realite.models.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import ru.realite.core.api.models.ModelAssetInfo;
import ru.realite.core.api.models.ModelAssetRegistry;
import ru.realite.core.api.models.ModelOffset;
import ru.realite.core.api.models.ModelVisualProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class ModelsCommand implements CommandExecutor, TabCompleter {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Supplier<ModelAssetRegistry> registrySupplier;

    public ModelsCommand(Supplier<ModelAssetRegistry> registrySupplier) {
        this.registrySupplier = Objects.requireNonNull(registrySupplier, "registrySupplier");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }
        if (!"debug".equalsIgnoreCase(args[0])) {
            sendHelp(sender);
            return true;
        }
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return matchPrefix(args[0], List.of("debug"));
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
