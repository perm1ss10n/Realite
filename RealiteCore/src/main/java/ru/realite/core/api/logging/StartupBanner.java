package ru.realite.core.api.logging;

import java.util.Locale;
import java.util.Objects;
import java.util.logging.Logger;

public final class StartupBanner {

    private StartupBanner() {
    }

    public record BannerSpec(
            String productName,
            String subtitle,
            String version,
            String statusLine,
            String footer) {
        public BannerSpec {
            productName = Objects.requireNonNullElse(productName, "Realite");
            subtitle = Objects.requireNonNullElse(subtitle, "");
            version = Objects.requireNonNullElse(version, "");
            statusLine = Objects.requireNonNullElse(statusLine, "");
            footer = Objects.requireNonNullElse(footer, "© Realite Project");
        }
    }

    /**
     * Compact startup line (recommended):
     * [PluginName] ▶ Enabling Realite Core ... ENABLED
     */
    public static void startupLine(Logger log, BannerSpec spec) {
        Objects.requireNonNull(log, "log");
        Objects.requireNonNull(spec, "spec");

        boolean c = isAnsiEnabled();

        String arrow = paint(c, Ansi.DIM + Ansi.WHITE, "▶");
        String text = paint(c, Ansi.WHITE, " Enabling " + spec.productName());
        String dots = paint(c, Ansi.DIM + Ansi.WHITE, " ... ");

        String status = colorizeState(c, spec.statusLine());

        log.info(arrow + text + dots + status);
    }

    /**
     * Old full banner (kept for compatibility / occasional use).
     */
    public static void print(Logger log, BannerSpec spec) {
        Objects.requireNonNull(log, "log");
        Objects.requireNonNull(spec, "spec");

        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  " + spec.productName());
        if (!spec.subtitle().isBlank())
            log.info("  " + spec.subtitle());
        log.info("");
        if (!spec.version().isBlank())
            log.info("  Version : " + spec.version());
        if (!spec.statusLine().isBlank())
            log.info("  " + spec.statusLine());
        log.info("");
        if (!spec.footer().isBlank())
            log.info("  " + spec.footer());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private static String colorizeState(boolean enabled, String statusLine) {
        if (!enabled)
            return statusLine;

        String s = statusLine.toLowerCase(Locale.ROOT);

        String color = (s.contains("enabled") || s.contains("ok")) ? Ansi.GREEN
                : (s.contains("loaded") || s.contains("waiting")) ? Ansi.YELLOW
                        : (s.contains("disabled") || s.contains("error") || s.contains("failed")) ? Ansi.RED
                                : Ansi.WHITE;

        // Output only the state part if format is "Status : XYZ"
        int idx = statusLine.indexOf(':');
        if (idx >= 0 && idx + 1 < statusLine.length()) {
            String state = statusLine.substring(idx + 1).trim();
            return color + state + Ansi.RESET;
        }

        return color + statusLine + Ansi.RESET;
    }

    private static String paint(boolean enabled, String ansiPrefix, String text) {
        if (!enabled)
            return text;
        return ansiPrefix + text + Ansi.RESET;
    }

    /**
     * Color control:
     * -Drealite.ansi=on|off|auto
     * Default: auto
     */
    private static boolean isAnsiEnabled() {
        String mode = System.getProperty("realite.ansi", "auto").toLowerCase(Locale.ROOT).trim();
        return switch (mode) {
            case "on", "true", "1", "yes" -> true;
            case "off", "false", "0", "no" -> false;
            default -> isAnsiLikelySupported();
        };
    }

    private static boolean isAnsiLikelySupported() {
        // Without an interactive console, ANSI often ends up as garbage in file logs /
        // services / docker logs.
        if (System.console() == null)
            return false;

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win"))
            return true;

        // Windows Terminal / VSCode / ConEmu / Git Bash, etc.
        return hasEnv("WT_SESSION") || hasEnv("TERM") || hasEnv("ANSICON") || hasEnv("ConEmuANSI");
    }

    private static boolean hasEnv(String key) {
        String v = System.getenv(key);
        return v != null && !v.isBlank();
    }
}