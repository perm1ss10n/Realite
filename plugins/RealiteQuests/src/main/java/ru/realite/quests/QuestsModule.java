package ru.realite.quests;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.Module;
import ru.realite.core.api.Subscription;
import ru.realite.core.api.events.ClassLevelUpEvent;
import ru.realite.core.api.events.ClassSelectedEvent;
import ru.realite.core.api.events.EvolutionCompletedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuestsModule implements Module {

    private final List<Subscription> subscriptions = new ArrayList<>();

    @Override
    public String id() {
        return "quests";
    }

    @Override
    public void onEnable(CoreApi core) {
        subscriptions.add(core.events().subscribe(ClassSelectedEvent.class, event -> {
            core.platform().info("[Quests] Player " + event.playerUuid()
                    + " selected class " + event.classId());
            sendMessage(event.playerUuid(),
                    "Quest update: class selected " + event.classId());
        }));

        subscriptions.add(core.events().subscribe(ClassLevelUpEvent.class, event -> {
            core.platform().info("[Quests] Player " + event.playerUuid()
                    + " leveled class " + event.classId()
                    + " to " + event.newLevel());
            sendMessage(event.playerUuid(),
                    "Quest update: class " + event.classId() + " reached level " + event.newLevel());
        }));

        subscriptions.add(core.events().subscribe(EvolutionCompletedEvent.class, event -> {
            core.platform().info("[Quests] Player " + event.playerUuid()
                    + " completed evolution " + event.evolutionId()
                    + " for class " + event.classId());
            sendMessage(event.playerUuid(),
                    "Quest update: evolution completed " + event.evolutionId());
        }));
    }

    @Override
    public void onDisable() {
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
