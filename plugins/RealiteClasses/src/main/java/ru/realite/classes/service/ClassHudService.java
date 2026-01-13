package ru.realite.classes.service;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import ru.realite.classes.model.HudMode;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClassHudService {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final EvolutionService evolutionService;
    private final ClassLevelXpService levelXpService;
    private final boolean legacyBossBarEnabled;

    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public ClassHudService(ClassService classService,
            ClassConfigRepository classConfig,
            EvolutionService evolutionService,
            ClassLevelXpService levelXpService,
            boolean legacyBossBarEnabled) {
        this.classService = classService;
        this.classConfig = classConfig;
        this.evolutionService = evolutionService;
        this.levelXpService = levelXpService;
        this.legacyBossBarEnabled = legacyBossBarEnabled;
    }

    public void tick(Player p) {
        var prof = classService.getProfile(p);
        if (prof == null || !prof.hasClass()) {
            clearAll(p);
            return;
        }

        var progressDataOpt = levelXpService.getLevelXp(p);
        if (progressDataOpt.isEmpty()) {
            clearAll(p);
            return;
        }
        var progressData = progressDataOpt.get();

        HudMode mode = prof.getHudMode();

        // вычисления прогресса
        var def = classConfig.get(prof.getClassId());
        int xpPerLevel = progressData.maxXpForLevel();
        int inLevel = progressData.currentXp();
        double progress = (double) inLevel / (double) xpPerLevel;

        int evoNum = evolutionService.getEvolutionNumber(prof);
        String evoRoman = toRoman(evoNum);

        String className = (def != null ? def.name : prof.getClassId().name());
        String title = "§6" + className + " §7(" + evoRoman + ")  §bУр. " + progressData.level()
                + "  §7XP §b" + inLevel + "§7/§b" + xpPerLevel;
        Component bossBarTitle = Component.text(
                "Уровень " + progressData.level() + " \u2022 XP " + inLevel + "/" + xpPerLevel);

        // переключение режимов
        switch (mode) {
            case BOSSBAR -> {
                if (legacyBossBarEnabled) {
                    showBossBar(p, bossBarTitle, progress);
                } else {
                    clearBossBar(p);
                }
                clearActionBar(p);
                clearSidebar(p);
            }
            case ACTIONBAR -> {
                clearBossBar(p);
                showActionBar(p, title);
                clearSidebar(p);
            }
            case SIDEBAR -> {
                clearBossBar(p);
                clearActionBar(p);
                showSidebar(p, prof, className, evoRoman, inLevel, xpPerLevel);
            }
            case OFF -> clearAll(p);
        }
    }

    public void refreshNow(Player p) {
        tick(p);
    }

    public void clearAll(Player p) {
        clearBossBar(p);
        clearActionBar(p);
        clearSidebar(p);
    }

    private void showBossBar(Player p, Component title, double progress01) {
        BossBar bar = bossBars.computeIfAbsent(p.getUniqueId(),
                id -> BossBar.bossBar(title, (float) clamp(progress01), BossBar.Color.BLUE,
                        BossBar.Overlay.PROGRESS));

        bar.name(title);
        bar.progress((float) clamp(progress01));
        p.showBossBar(bar);
    }

    private void clearBossBar(Player p) {
        BossBar bar = bossBars.remove(p.getUniqueId());
        if (bar != null) {
            p.hideBossBar(bar);
        }
    }

    private void showActionBar(Player p, String text) {
        //TODO: допилить хуйню эту
        p.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
    }

    private void clearActionBar(Player p) {
        // просто отправляем пустое, чтобы не висело
        p.sendActionBar(Component.empty());
    }

    private void showSidebar(Player p, PlayerProfile prof, String className, String evoRoman, long inLevel,
            int xpPerLevel) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective(
                "realiteclass",
                Criteria.DUMMY,
                LegacyComponentSerializer.legacySection().deserialize("§6§lКласс"),
                RenderType.INTEGER);

        setLine(obj, 6, "§7Класс: §e" + className);
        setLine(obj, 5, "§7Эволюция: §a(" + evoRoman + ")");
        setLine(obj, 4, "§7Уровень: §b" + prof.getClassLevel());
        setLine(obj, 3, "§7XP: §b" + inLevel + "§7/§b" + xpPerLevel);

        var next = evolutionService.getNextEvolution(prof);
        if (next != null) {
            setLine(obj, 2, "§7Эволюция на §b" + next.requiredLevel + "§7 ур.");
        } else {
            setLine(obj, 2, "§aФинальная эволюция");
        }

        p.setScoreboard(sb);
    }

    private void clearSidebar(Player p) {
        // возвращаем основной scoreboard сервера
        p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private static void setLine(Objective obj, int score, String text) {
        obj.getScore(text).setScore(score);
    }

    private static double clamp(double v) {
        if (v < 0)
            return 0;
        if (v > 1)
            return 1;
        return v;
    }

    private static String toRoman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }
}
