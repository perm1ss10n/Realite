package ru.realite.familiars.service;

import org.bukkit.entity.Player;

public interface FamiliarService {
    CheckResult canTame(Player player, String typeId);

    CheckResult canSummon(Player player, String typeId);
}
