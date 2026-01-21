package ru.realite.ui.boss;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.core.boss.api.BossPhase;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.DespawnReason;
import ru.realite.core.boss.ui.BossUIController;
import ru.realite.core.boss.ui.BossUIHandle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class BossUIControllerImpl implements BossUIController {
    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, Set<UUID>> viewers = new HashMap<>();

    @Override
    public BossUIHandle attach(RealiteBoss boss) {
        UUID instanceId = boss.instanceId();
        bossBars.computeIfAbsent(instanceId, id -> createBossBar(boss));
        return new BossUIHandle(instanceId);
    }

    @Override
    public void update(RealiteBoss boss) {
        BossBar bar = bossBars.get(boss.instanceId());
        if (bar == null) {
            return;
        }
        bar.name(titleFor(boss));
        bar.progress(progressFor(boss));
    }

    @Override
    public void showTo(RealiteBoss boss, Player player) {
        BossBar bar = bossBars.computeIfAbsent(boss.instanceId(), id -> createBossBar(boss));
        viewers.computeIfAbsent(boss.instanceId(), id -> new HashSet<>()).add(player.getUniqueId());
        player.showBossBar(bar);
    }

    @Override
    public void hideFrom(RealiteBoss boss, Player player) {
        BossBar bar = bossBars.get(boss.instanceId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
        Set<UUID> list = viewers.get(boss.instanceId());
        if (list != null) {
            list.remove(player.getUniqueId());
        }
    }

    @Override
    public void detach(RealiteBoss boss, DespawnReason reason) {
        UUID instanceId = boss.instanceId();
        BossBar bar = bossBars.remove(instanceId);
        Set<UUID> list = viewers.remove(instanceId);
        if (bar == null || list == null) {
            return;
        }
        for (UUID playerId : list) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.hideBossBar(bar);
            }
        }
    }

    private BossBar createBossBar(RealiteBoss boss) {
        return BossBar.bossBar(titleFor(boss), progressFor(boss), BossBar.Color.RED, BossBar.Overlay.PROGRESS);
    }

    private Component titleFor(RealiteBoss boss) {
        BossPhase phase = boss.getPhase();
        String phaseLabel = phase == null ? "" : " [" + phase.id() + "]";
        return Component.text(boss.bossId() + phaseLabel);
    }

    private float progressFor(RealiteBoss boss) {
        double max = boss.getMaxHp();
        if (max <= 0) {
            return 0f;
        }
        double pct = Math.max(0.0, Math.min(1.0, boss.getHp() / max));
        return (float) pct;
    }
}
