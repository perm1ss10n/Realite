package ru.realite.magic.mastery;

public record LevelUpResult(int oldLevel,
                            int newLevel,
                            int xpInLevel,
                            int xpToNext,
                            int xpRequired,
                            boolean leveledUp) {
}
