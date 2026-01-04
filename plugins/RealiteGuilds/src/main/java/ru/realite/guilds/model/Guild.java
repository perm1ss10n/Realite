package ru.realite.guilds.model;

import java.util.Map;
import java.util.UUID;

public record Guild(String tag, String name, UUID owner, GuildHome home, GuildClaim claim, int level, long xp,
                    Map<String, Integer> upgradeLevels) {
}
