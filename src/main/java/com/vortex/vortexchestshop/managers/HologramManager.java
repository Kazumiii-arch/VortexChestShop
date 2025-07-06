// src/main/java/com/vortex/vortexchestshop/managers/HologramManager.java

package com.vortex.vortexchestshop.managers;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import com.vortex.vortexchestshop.utils.Logger;
import me.clip.placeholderapi.PlaceholderAPI; // PlaceholderAPI import
import eu.decentsoftware.holograms.api.DHAPI; // DecentHolograms API main class
import eu.decentsoftware.holograms.api.holograms.Hologram; // DecentHolograms Hologram class
import org.bukkit.Bukkit;
import org.bukkit.ChatColor; // Still useful for translating config messages if needed elsewhere
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack; // Added for getItemDisplayName
import org.bukkit.inventory.meta.ItemMeta; // Added for getItemDisplayName

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe map

public class HologramManager {

    private final VortexChestShop plugin;
    // Map to store active holograms, keyed by shop UUID
    private final Map<UUID, Hologram> activeHolograms;
    // Flag to indicate if DecentHolograms is available
    private boolean decentHologramsAvailable = false;

    public HologramManager(VortexChestShop plugin) {
        this.plugin = plugin;
        this.activeHolograms = new ConcurrentHashMap<>();

        // Attempt to hook into DecentHolograms
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") != null &&
            Bukkit.getPluginManager().getPlugin("DecentHolograms").isEnabled()) {
            this.decentHologramsAvailable = true;
            Logger.info("DecentHolograms hook successful.");
        } else {
            Logger.warning("DecentHolograms not found or not enabled. Holograms will not be displayed.");
            this.decentHologramsAvailable = false;
        }
    }

    /**
     * Creates a hologram for a given ChestShop using DecentHolograms API.
     * @param shop The ChestShop to create a hologram for.
     */
    public void createHologram(ChestShop shop) {
        // Do not create if DecentHolograms is not available, holograms are disabled in config,
        // or if the shop has no stock (unless you want to show "Out of Stock" hologram).
        if (!decentHologramsAvailable || !plugin.getConfig().getBoolean("hologram-text.enabled", true) || shop.getCurrentStock() <= 0) {
            return;
        }
        // If a hologram already exists for this shop, update it instead
        if (activeHolograms.containsKey(shop.getId())) {
            updateHologram(shop);
            return;
        }

        Location hologramLocation = getHologramLocation(shop.getLocation());
        String hologramName = "VCS_Shop_" + shop.getId().toString(); // Unique name for the hologram

        List<String> lines = getHologramLines(shop);

        try {
            // Create and spawn the hologram using DHAPI
            Hologram hologram = DHAPI.createHologram(hologramName, hologramLocation);
            DHAPI.setHologramLines(hologram, lines); // Set all lines at once

            activeHolograms.put(shop.getId(), hologram);
            Logger.debug("Created hologram for shop " + shop.getId() + " at " + hologramLocation.toString());
        } catch (Exception e) {
            Logger.severe("Failed to create hologram for shop " + shop.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Updates an existing hologram for a given ChestShop using DecentHolograms API.
     * @param shop The ChestShop to update.
     */
    public void updateHologram(ChestShop shop) {
        // If DecentHolograms is not available, holograms are disabled, or stock is zero, ensure it's removed
        if (!decentHologramsAvailable || !plugin.getConfig().getBoolean("hologram-text.enabled", true) || shop.getCurrentStock() <= 0) {
            removeHologram(shop);
            return;
        }

        Hologram hologram = activeHolograms.get(shop.getId());
        if (hologram == null) {
            createHologram(shop); // Create if it doesn't exist yet
            return;
        }

        List<String> newLines = getHologramLines(shop);
        try {
            DHAPI.setHologramLines(hologram, newLines); // Update all lines at once
            Logger.debug("Updated hologram for shop " + shop.getId());
        } catch (Exception e) {
            Logger.severe("Failed to update hologram for shop " + shop.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Removes a hologram for a given ChestShop using DecentHolograms API.
     * @param shop The ChestShop to remove the hologram for.
     */
    public void removeHologram(ChestShop shop) {
        if (!decentHologramsAvailable) return; // Cannot remove if DH is not available

        Hologram hologram = activeHolograms.remove(shop.getId());
        if (hologram != null) {
            try {
                DHAPI.removeHologram(hologram.getName()); // Remove by name
                Logger.debug("Removed hologram for shop " + shop.getId());
            } catch (Exception e) {
                Logger.severe("Failed to remove hologram " + hologram.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Cleans up all active holograms managed by this plugin using DecentHolograms API.
     * Called on plugin disable to prevent lingering holograms.
     */
    public void cleanupAllHolograms() {
        if (!decentHologramsAvailable) return;
        for (Hologram hologram : activeHolograms.values()) {
            if (hologram != null) {
                try {
                    DHAPI.removeHologram(hologram.getName());
                } catch (Exception e) {
                    Logger.severe("Failed to cleanup hologram " + hologram.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        activeHolograms.clear();
        Logger.info("Cleaned up all holograms.");
    }

    /**
     * Generates the lines of text for a shop's hologram, applying PlaceholderAPI if available.
     * @param shop The ChestShop object.
     * @return A list of formatted strings for the hologram lines.
     */
    private List<String> getHologramLines(ChestShop shop) {
        List<String> lines = new ArrayList<>();
        String itemNameFormat = plugin.getConfig().getString("hologram-text.item-name-format", "&b%item_display_name%");
        String priceFormat = plugin.getConfig().getString("hologram-text.price-format", "&aPrice: &e$%price% each");

        String itemDisplayName = getItemDisplayName(shop.getSoldItem());
        String itemNameLine = itemNameFormat.replace("%item_display_name%", itemDisplayName);

        String formattedPrice = String.format("%,.2f", shop.getPrice());
        String priceLine = priceFormat.replace("%price%", formattedPrice);

        // Apply PlaceholderAPI if available and the owner is online
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && shop.getOwnerPlayer() != null) {
            itemNameLine = PlaceholderAPI.setPlaceholders(shop.getOwnerPlayer(), itemNameLine);
            priceLine = PlaceholderAPI.setPlaceholders(shop.getOwnerPlayer(), priceLine);
        }

        // DecentHolograms automatically handles Bukkit color codes, so no need for ChatColor.translateAlternateColorCodes here
        // if you configure your messages in config.yml with '&' codes.
        lines.add(itemNameLine);
        lines.add(priceLine);

        // Example for showing stock, if enabled in config
        // if (plugin.getConfig().getBoolean("hologram-text.show-stock", false)) {
        //     lines.add("&7Stock: &f" + shop.getCurrentStock());
        // }

        return lines;
    }

    /**
     * Calculates the exact location for the hologram display below the floating item.
     * @param chestLocation The location of the chest block.
     * @return The Location for the Hologram.
     */
    private Location getHologramLocation(Location chestLocation) {
        double yOffset = plugin.getConfig().getDouble("hologram-text.y-offset", 0.5);
        // Adjust Y-coordinate to be below the floating item and above the chest, centered horizontally
        return chestLocation.clone().add(0.5, yOffset, 0.5); // X+0.5, Z+0.5 for center
    }

    /**
     * Helper method to get the display name of an ItemStack.
     * @param item The ItemStack.
     * @return The display name or default name.
     */
    private String getItemDisplayName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name().replace("_", " ").toLowerCase();
    }
}
