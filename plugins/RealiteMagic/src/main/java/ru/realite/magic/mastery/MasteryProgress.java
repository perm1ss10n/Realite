package ru.realite.magic.mastery;

public final class MasteryProgress {

    private int level;
    private int xp;
    private long casts;
    private long hits;
    private long kills;

    public MasteryProgress(int level, int xp, long casts, long hits, long kills) {
        this.level = Math.max(1, level);
        this.xp = Math.max(0, xp);
        this.casts = Math.max(0L, casts);
        this.hits = Math.max(0L, hits);
        this.kills = Math.max(0L, kills);
    }

    public int level() {
        return level;
    }

    public void level(int level) {
        this.level = Math.max(1, level);
    }

    public int xp() {
        return xp;
    }

    public void xp(int xp) {
        this.xp = Math.max(0, xp);
    }

    public long casts() {
        return casts;
    }

    public void casts(long casts) {
        this.casts = Math.max(0L, casts);
    }

    public long hits() {
        return hits;
    }

    public void hits(long hits) {
        this.hits = Math.max(0L, hits);
    }

    public long kills() {
        return kills;
    }

    public void kills(long kills) {
        this.kills = Math.max(0L, kills);
    }
}
