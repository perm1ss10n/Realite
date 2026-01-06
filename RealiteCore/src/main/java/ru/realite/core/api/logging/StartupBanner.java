package ru.realite.core.api.logging;

import java.util.Objects;
import java.util.logging.Logger;

public final class StartupBanner {

    private StartupBanner() {}

    public record BannerSpec(
            String productName,
            String subtitle,
            String version,
            String statusLine,
            String footer
    ) {
        public BannerSpec {
            productName = Objects.requireNonNullElse(productName, "Realite");
            subtitle = Objects.requireNonNullElse(subtitle, "");
            version = Objects.requireNonNullElse(version, "");
            statusLine = Objects.requireNonNullElse(statusLine, "");
            footer = Objects.requireNonNullElse(footer, "© Realite Project");
        }
    }

    public static void print(Logger log, BannerSpec spec) {
        Objects.requireNonNull(log, "log");
        Objects.requireNonNull(spec, "spec");

        log.info("");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("  " + spec.productName());
        if (!spec.subtitle().isBlank()) log.info("  " + spec.subtitle());
        log.info("");
        if (!spec.version().isBlank()) log.info("  Version : " + spec.version());
        if (!spec.statusLine().isBlank()) log.info("  " + spec.statusLine());
        log.info("");
        if (!spec.footer().isBlank()) log.info("  " + spec.footer());
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }
}
