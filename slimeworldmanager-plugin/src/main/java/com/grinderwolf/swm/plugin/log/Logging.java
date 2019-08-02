package com.grinderwolf.swm.plugin.log;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Logging {

    private static final String PREFIX = ChatColor.BLUE + "[SWM] ";

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GRAY + message);
    }

    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + message);
    }

    public static void error(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED + message);
    }
}
