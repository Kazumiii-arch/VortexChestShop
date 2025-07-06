// src/main/java/com/vortex/vortexchestshop/utils/Logger.java

package com.vortex.vortexchestshop.utils;

import com.vortex.vortexchestshop.VortexChestShop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Logger {

    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&8[&6VortexChestShop&8] &r");

    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GREEN + message);
    }

    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + message);
    }

    public static void severe(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED + message);
    }

    public static void debug(String message) {
        // Only log debug messages if debug mode is enabled in config (future feature)
        // if (VortexChestShop.getInstance().getConfig().getBoolean("debug-mode", false)) {
        //     Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GRAY + "[DEBUG] " + message);
        // }
    }
}
