package ru.realite.core.api.logging;

import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.logging.StartupBanner.BannerSpec;

public final class Banners {

    private Banners() {
    }

    /*
     * =========================
     * CORE
     * =========================
     */

    public static void REALITE_CORE(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Core",
                "RPG Platform Core",
                "Status  : ENABLED",
                "API ready for plugins");
    }

    /*
     * =========================
     * CHAT
     * =========================
     */

    public static void REALITE_CHAT(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Chat",
                "Chat & Formatting",
                "Status  : ENABLED",
                "Guild/Class hooks");
    }

    /*
     * =========================
     * GUILDS
     * =========================
     */

    public static void REALITE_GUILDS(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Guilds",
                "Guilds & Progression",
                "Status  : ENABLED",
                "Ranks, invites, chat integration");
    }

    /*
     * =========================
     * QUESTS
     * =========================
     */

    public static void REALITE_QUESTS(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Quests",
                "Quest System",
                "Status  : ENABLED",
                "Objectives, rewards, persistence");
    }

    public static void REALITE_QUESTS_WAITING(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Quests",
                "Quest System",
                "Status  : LOADED",
                "Waiting for RealiteCore module enable");
    }

    /*
     * =========================
     * CLASSES
     * =========================
     */

    public static void REALITE_CLASSES(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Classes",
                "Classes & Evolutions",
                "Status  : ENABLED",
                "Profiles, mastery, unlocks");
    }

    public static void REALITE_CLASSES_WAITING(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite Classes",
                "Classes & Evolutions",
                "Status  : LOADED",
                "Waiting for RealiteCore module enable");
    }

    /*
     * =========================
     * CITY
     * =========================
     */

    public static void REALITE_CITY(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite City",
                "Cities & Plots",
                "Status  : ENABLED",
                "Region access & protections");
    }

    public static void REALITE_CITY_WAITING(JavaPlugin plugin) {
        banner(
                plugin,
                "Realite City",
                "Cities & Plots",
                "Status  : LOADED",
                "Waiting for RealiteCore module enable");
    }

    /*
     * =========================
     * INTERNAL
     * =========================
     */

    private static void banner(
            JavaPlugin plugin,
            String productName,
            String subtitle,
            String statusLine,
            String footer) {
        String version = plugin.getPluginMeta().getVersion();

        StartupBanner.print(plugin.getLogger(), new BannerSpec(
                productName,
                subtitle,
                version,
                statusLine,
                footer));
    }
}
