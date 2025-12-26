package ru.realite.classes.util;

import org.bukkit.ChatColor;

@SuppressWarnings("deprecation")
public final class Text {
    private Text() {}

    public static String c(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
