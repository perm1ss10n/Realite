package ru.realite.classes.service;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import ru.realite.classes.model.HudMode;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClassHudService {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final EvolutionService evolutionService;

    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public ClassHudService(ClassService classService,
                           ClassConfigRepository classConfig,
                           EvolutionService evolutionService) {
        this.classService = classService;
        this.classConfig = classConfig;
        this.evolutionService = evolutionService;
    }

    public void tick(Player p) {
        var prof = classService.getProfile(p);
        if (prof == null || !prof.hasClass()) {
            clearAll(p);
            return;
        }

        HudMode mode = prof.getHudMode();

        // вычисления прогресса
        var def = classConfig.get(prof.getClassId());
        int xpPerLevel = (def != null ? Math.max(1, def.xpPerLevel) : 100);

        long totalXp = prof.getClassXp();
        long inLevel = totalXp % xpPerLevel;
        double progress = (double) inLevel / (double) xpPerLevel;

        int evoNum = evolutionService.getEvolutionNumber(prof);
        String evoRoman = toRoman(evoNum);

        String className = (def != null ? def.name : prof.getClassId().name());
        String title = "§6" + className + " §7(" + evoRoman + ")  §bУр. " + prof.getClassLevel()
                + "  §7XP §b" + inLevel + "§7/§b" + xpPerLevel;

        // переключение режимов
        switch (mode) {
            case BOSSBAR -> {
                showBossBar(p, title, progress);
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

    private void showBossBar(Player p, String title, double progress01) {
        BossBar bar = bossBars.computeIfAbsent(p.getUniqueId(),
                id -> Bukkit.createBossBar(title, BarColor.BLUE, BarStyle.SOLID));

        bar.setTitle(title);
        bar.setProgress(clamp(progress01));
        if (!bar.getPlayers().contains(p)) bar.addPlayer(p);
        bar.setVisible(true);
    }

    private void clearBossBar(Player p) {
        BossBar bar = bossBars.remove(p.getUniqueId());
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    private void showActionBar(Player p, String text) {
        p.sendActionBar(Component.text(text.replace('§', '§'))); // оставляем цвет-коды как есть
    }

    private void clearActionBar(Player p) {
        // просто отправляем пустое, чтобы не висело
        p.sendActionBar(Component.empty());
    }

    private void showSidebar(Player p, PlayerProfile prof, String className, String evoRoman, long inLevel, int xpPerLevel) {
        // ВАЖНО: если у тебя другой плагин держит scoreboard — будет конфликт.
        // Пока ок, для MVP.
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("realiteclass", "dummy", Component.text("§6§lКласс"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

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
        if (v < 0) return 0;
        if (v > 1) return 1;
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
