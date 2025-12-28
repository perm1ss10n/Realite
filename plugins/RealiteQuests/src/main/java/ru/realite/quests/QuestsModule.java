package ru.realite.quests;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.Subscription;
import ru.realite.core.api.events.ClassLevelUpEvent;
import ru.realite.core.api.events.ClassSelectedEvent;
import ru.realite.core.api.events.EvolutionCompletedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QuestsModule implements Module {

    private final List<Subscription> subscriptions = new ArrayList<>();
    private final ModuleMetadata metadata = new ModuleMetadata(
            new ModuleId("realite-quests"),
            "RealiteQuests",
            "0.1.0",
            Set.of()
    );

    @Override
    public ModuleMetadata metadata() {
        return metadata;
    }

    @Override
    public void onLoad(ModuleContext ctx) {
        // no-op
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        subscriptions.add(ctx.events().subscribe(ClassSelectedEvent.class, event -> {
            ctx.logger().info("[Quests] Player " + event.playerUuid()
                    + " selected class " + event.classId());
            sendMessage(event.playerUuid(),
                    "Quest update: class selected " + event.classId());
        }));

        subscriptions.add(ctx.events().subscribe(ClassLevelUpEvent.class, event -> {
            ctx.logger().info("[Quests] Player " + event.playerUuid()
                    + " leveled class " + event.classId()
                    + " to " + event.newLevel());
            sendMessage(event.playerUuid(),
                    "Quest update: class " + event.classId() + " reached level " + event.newLevel());
        }));

        subscriptions.add(ctx.events().subscribe(EvolutionCompletedEvent.class, event -> {
            ctx.logger().info("[Quests] Player " + event.playerUuid()
                    + " completed evolution " + event.evolutionId()
                    + " for class " + event.classId());
            sendMessage(event.playerUuid(),
                    "Quest update: evolution completed " + event.evolutionId());
        }));
    }

    @Override
    public void onDisable(ModuleContext ctx) {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
    }

    private void sendMessage(UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }
}
