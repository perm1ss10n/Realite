package ru.realite.guilds.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.service.GuildRankPermission;

public final class GuildSalaryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JavaPlugin plugin;
    private final FileConfiguration config;
    private final GuildRepository repository;
    private final GuildMessages messages;
    private final GuildRankService rankService;
    private final EconomyService economy;
    private final ZoneId zoneId;
    private final LocalTime dailyTime;
    private final boolean enabled;
    private BukkitTask scheduledTask;

    public GuildSalaryService(JavaPlugin plugin, FileConfiguration config, GuildRepository repository,
                              GuildMessages messages, GuildRankService rankService, EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.repository = repository;
        this.messages = messages;
        this.rankService = rankService;
        this.economy = economy;
        this.zoneId = resolveZoneId(config.getString("salary.timezone", "server"));
        this.dailyTime = parseDailyTime(config.getString("salary.dailyTime", "06:00"));
        this.enabled = config.getBoolean("salary.enabled", true) && economy.isAvailable();
        if (config.getBoolean("salary.enabled", true) && !economy.isAvailable()) {
            plugin.getLogger().warning("[Salary] Economy not available, salary is disabled.");
        }
        if (enabled) {
            scheduleNextRun();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void handleSalaryInfo(Player player) {
        if (!enabled) {
            messages.send(player, "salary.disabled");
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            messages.send(player, "error.guild.no_member");
            return;
        }
        if (!rankService.hasPermission(member.role(), GuildRankPermission.SALARY_VIEW)) {
            messages.send(player, "error.no_permission");
            return;
        }
        GuildRankService.GuildRank rank = rankService.getRank(rankService.resolveRankId(member.role()));
        if (rank == null) {
            messages.send(player, "salary.disabled");
            return;
        }
        String rankName = messages.raw(rank.displayNameKey());
        String nextRun = formatNextRun(ZonedDateTime.now(zoneId));
        messages.send(player, "salary.info",
                "rate", formatAmount(rank.salaryDaily()),
                "rank", rankName,
                "next", nextRun);
    }

    public void handleAdminRun(CommandSender sender) {
        if (!enabled) {
            sender.sendMessage(messages.msg("salary.disabled"));
            return;
        }
        sender.sendMessage(messages.msg("salary.admin.run.start"));
        SalaryRunResult result = runSalaryForOnline(getEligibleDate(ZonedDateTime.now(zoneId)), true);
        sender.sendMessage(messages.msg("salary.admin.run.done",
                "count", String.valueOf(result.playersPaid()),
                "total", formatAmount(result.totalPaid())));
    }

    public void handlePlayerJoin(Player player) {
        if (!enabled || !config.getBoolean("salary.catchupOnJoin", true)) {
            return;
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            return;
        }
        LocalDate eligibleDate = getEligibleDate(ZonedDateTime.now(zoneId));
        if (eligibleDate == null) {
            return;
        }
        int days = calculateCatchupDays(member.lastSalaryDate(), eligibleDate);
        if (days <= 0) {
            return;
        }
        GuildRankService.GuildRank rank = rankService.getRank(rankService.resolveRankId(member.role()));
        if (rank == null || rank.salaryDaily() <= 0) {
            return;
        }
        double amount = rank.salaryDaily() * days;
        economy.deposit(player, amount);
        repository.saveMember(new GuildMember(member.uuid(), member.tag(), member.role(), eligibleDate));
        messages.send(player, "salary.paid", "amount", formatAmount(amount));
        logPayment(player, amount, days, "catchup");
    }

    public void runScheduledSalary() {
        if (!enabled) {
            return;
        }
        runSalaryForOnline(LocalDate.now(zoneId), false);
    }

    private SalaryRunResult runSalaryForOnline(LocalDate eligibleDate, boolean adminRun) {
        int paidPlayers = 0;
        double totalPaid = 0.0;
        for (GuildMember member : repository.getMembers()) {
            Player player = Bukkit.getPlayer(member.uuid());
            if (player == null || !player.isOnline()) {
                continue;
            }
            GuildRankService.GuildRank rank = rankService.getRank(rankService.resolveRankId(member.role()));
            if (rank == null || rank.salaryDaily() <= 0) {
                continue;
            }
            if (member.lastSalaryDate() != null && !member.lastSalaryDate().isBefore(eligibleDate)) {
                continue;
            }
            double amount = rank.salaryDaily();
            economy.deposit(player, amount);
            repository.saveMember(new GuildMember(member.uuid(), member.tag(), member.role(), eligibleDate));
            messages.send(player, "salary.paid", "amount", formatAmount(amount));
            logPayment(player, amount, 1, adminRun ? "admin" : "daily");
            paidPlayers++;
            totalPaid += amount;
        }
        return new SalaryRunResult(paidPlayers, totalPaid);
    }

    private void scheduleNextRun() {
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        ZonedDateTime next = calculateNextRun(now);
        long delayTicks = Math.max(1L, Duration.between(now, next).toMillis() / 50L);
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
        scheduledTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            runScheduledSalary();
            scheduleNextRun();
        }, delayTicks);
    }

    private ZonedDateTime calculateNextRun(ZonedDateTime now) {
        ZonedDateTime candidate = now.with(dailyTime);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private LocalDate getEligibleDate(ZonedDateTime now) {
        if (now.toLocalTime().isBefore(dailyTime)) {
            return now.toLocalDate().minusDays(1);
        }
        return now.toLocalDate();
    }

    private int calculateCatchupDays(LocalDate lastPaid, LocalDate eligibleDate) {
        if (eligibleDate == null) {
            return 0;
        }
        if (lastPaid != null && !lastPaid.isBefore(eligibleDate)) {
            return 0;
        }
        int days;
        if (lastPaid == null) {
            days = 1;
        } else {
            days = (int) ChronoUnit.DAYS.between(lastPaid.plusDays(1), eligibleDate.plusDays(1));
        }
        int maxMissedDays = Math.max(1, config.getInt("salary.maxMissedDays", 3));
        return Math.min(days, maxMissedDays);
    }

    private String formatNextRun(ZonedDateTime now) {
        ZonedDateTime next = calculateNextRun(now);
        return TIME_FORMATTER.format(next);
    }

    private String formatAmount(double amount) {
        if (amount == Math.rint(amount)) {
            return String.valueOf((long) amount);
        }
        return String.format(Locale.US, "%.2f", amount);
    }

    private void logPayment(Player player, double amount, int days, String reason) {
        if (!config.getBoolean("salary.logPayments", true)) {
            return;
        }
        plugin.getLogger().info(String.format(
                Locale.US,
                "[Salary] Paid %s to %s (%d day(s), %s)",
                formatAmount(amount),
                player.getName(),
                days,
                reason));
    }

    private ZoneId resolveZoneId(String raw) {
        if (raw == null || raw.isBlank() || "server".equalsIgnoreCase(raw)) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(raw);
        } catch (Exception ex) {
            plugin.getLogger().warning("[Salary] Invalid timezone '" + raw + "', using server timezone.");
            return ZoneId.systemDefault();
        }
    }

    private LocalTime parseDailyTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return LocalTime.of(6, 0);
        }
        try {
            return LocalTime.parse(raw);
        } catch (Exception ex) {
            plugin.getLogger().warning("[Salary] Invalid daily time '" + raw + "', using 06:00.");
            return LocalTime.of(6, 0);
        }
    }

    private record SalaryRunResult(int playersPaid, double totalPaid) {
    }
}
