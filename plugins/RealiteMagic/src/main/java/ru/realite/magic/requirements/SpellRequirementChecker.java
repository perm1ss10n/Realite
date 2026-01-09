package ru.realite.magic.requirements;

import org.bukkit.entity.Player;
import ru.realite.magic.spell.SpellDefinition;

public interface SpellRequirementChecker {

    CheckResult check(Player player, SpellDefinition spell);
}
