package ru.realite.core.boss.ui;

import org.bukkit.entity.Player;
import ru.realite.core.boss.api.RealiteBoss;
import ru.realite.core.boss.core.DespawnReason;

public interface BossUIController {
    BossUIHandle attach(RealiteBoss boss);

    void update(RealiteBoss boss);

    void showTo(RealiteBoss boss, Player player);

    void hideFrom(RealiteBoss boss, Player player);

    void detach(RealiteBoss boss, DespawnReason reason);
}
