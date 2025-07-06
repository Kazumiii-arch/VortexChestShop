// src/main/java/com/vortex/vortexchestshop/utils/Logger.java

package com.vortex.vortexchestshop.utils;

import com.vortex.vortexchestshop.VortexChestShop; // Import the main plugin class to access its config
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Logger {

    // The plugin prefix for all log messages, translated to Minecraft color codes
    private static final String PREFIX = ChatColor.translateAlternateColorCodes('&', "&8[&6VortexChestShop&8] &r");

    /**
     * Logs an informational message to the console.
     * @param message The message to log.
     */
    public static void info(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GREEN + message);
    }

    /**
     * Logs a warning message to the console.
     * @param message The message to log.
     */
    public static void warning(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.YELLOW + message);
    }

    /**
     * Logs a severe error message to the console.
     * @param message The message to log.
     */
    public static void severe(String message) {
        Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.RED + message);
    }

    /**
     * Logs a debug message to the console.
     * This message will only be displayed if 'debug-mode' is enabled in the plugin's config.yml.
     * @param message The message to log.
     */
    public static void debug(String message) {
        // Check if the main plugin instance is available and if debug mode is enabled in its config
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getConfig().getBoolean("debug-mode", false)) {
            Bukkit.getConsoleSender().sendMessage(PREFIX + ChatColor.GRAY + "[DEBUG] " + message);
        }
    }
}
