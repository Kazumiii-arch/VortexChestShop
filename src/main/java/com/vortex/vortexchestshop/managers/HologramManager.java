// src/main/java/com/vortex/vortexchestshop/managers/HologramManager.java

package com.vortex.vortexchestshop.managers;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import com.vortex.vortexchestshop.utils.Logger;
import me.clip.placeholderapi.PlaceholderAPI; // PlaceholderAPI import
import com.sainttx.holograms.api.Hologram; // HolographicDisplays API Hologram class
import com.sainttx.holograms.api.HologramManager; // HolographicDisplays API Manager class
import com.sainttx.holograms.api.line.TextLine; // HolographicDisplays API TextLine class
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.inventory.meta.ItemMeta; // For getItemDisplayName

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe map

public class HologramManager {

    private final VortexChestShop plugin;
    // Map to store active holograms, keyed by shop UUID
    private final Map<UUID, Hologram> activeHolograms;
    // HolographicDisplays API Manager instance
    private HologramManager hdManager;

    public HologramManager(VortexChestShop plugin) {
        this.plugin = plugin;
        this.activeHolograms = new ConcurrentHashMap<>(); // Use ConcurrentHashMap for thread safety

        // Attempt to hook into HolographicDisplays
        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") != null &&
            Bukkit.getPluginManager().getPlugin("HolographicDisplays").isEnabled() && // Ensure it's enabled
            Bukkit.getPluginManager().getPlugin("HolographicDisplays") instanceof com.sainttx.holograms.HologramPlugin) {
            this.hdManager = ((com.sainttx.holograms.HologramPlugin) Bukkit.getPluginManager().getPlugin("HolographicDisplays")).getHologramManager();
            Logger.info("HolographicDisplays hook successful.");
        } else {
            Logger.warning("HolographicDisplays not found or not loaded correctly. Holograms will not be displayed.");
            this.hdManager = null; // Explicitly set to null if not available
        }
    }

    /**
     * Creates a hologram for a given ChestShop.
     * @param shop The ChestShop to create a hologram for.
     */
    public void createHologram(ChestShop shop) {
        // Do not create if HolographicDisplays is not available or holograms are disabled in config
        if (hdManager == null || !plugin.getConfig().getBoolean("hologram-text.enabled", true)) {
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

        // Create and spawn the hologram
        Hologram hologram = new Hologram(hologramName, hologramLocation, false); // false for not persistent (plugin manages)
        for (String line : lines) {
            hologram.addLine(new TextLine(hologram, line));
        }
        hdManager.addHologram(hologram); // Add to HD manager
        hologram.spawn(); // Spawn the hologram entity

        activeHolograms.put(shop.getId(), hologram);
        Logger.debug("Created hologram for shop " + shop.getId() + " at " + hologramLocation.toString());
    }

    /**
     * Updates an existing hologram for a given ChestShop.
     * @param shop The ChestShop to update.
     */
    public void updateHologram(ChestShop shop) {
        // If HolographicDisplays is not available or holograms are disabled, ensure it's removed
        if (hdManager == null || !plugin.getConfig().getBoolean("hologram-text.enabled", true) || shop.getCurrentStock() <= 0) {
            removeHologram(shop);
            return;
        }

        Hologram hologram = activeHolograms.get(shop.getId());
        if (hologram == null) {
            createHologram(shop); // Create if it doesn't exist yet
            return;
        }

        List<String> newLines = getHologramLines(shop);

        // Update existing lines or add/remove as needed
        // Iterate up to the maximum number of lines between old and new
        for (int i = 0; i < Math.max(hologram.getLines().size(), newLines.size()); i++) {
            if (i < newLines.size() && i < hologram.getLines().size()) {
                // Update existing line if text has changed
                TextLine textLine = (TextLine) hologram.getLine(i);
                if (!textLine.getText().equals(newLines.get(i))) {
                    textLine.setText(newLines.get(i));
                }
            } else if (i < newLines.size()) {
                // Add new line if there are more new lines than old
                hologram.addLine(new TextLine(hologram, newLines.get(i)));
            } else {
                // Remove excess line if there are fewer new lines than old
                hologram.removeLine(hologram.getLine(i));
            }
        }
        hologram.update(); // Update the hologram for players

        Logger.debug("Updated hologram for shop " + shop.getId());
    }

    /**
     * Removes a hologram for a given ChestShop.
     * @param shop The ChestShop to remove the hologram for.
     */
    public void removeHologram(ChestShop shop) {
        if (hdManager == null) return; // Cannot remove if HD is not available

        Hologram hologram = activeHolograms.remove(shop.getId());
        if (hologram != null) {
            hologram.despawn(); // Despawn the hologram entity
            hdManager.removeHologram(hologram); // Remove from HolographicDisplays manager
            Logger.debug("Removed hologram for shop " + shop.getId());
        }
    }

    /**
     * Cleans up all active holograms managed by this plugin.
     * Called on plugin disable to prevent lingering holograms.
     */
    public void cleanupAllHolograms() {
        if (hdManager == null) return; // Cannot clean up if HD is not available
        for (Hologram hologram : activeHolograms.values()) {
            if (hologram != null) {
                hologram.despawn();
                hdManager.removeHologram(hologram);
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

        // Get the display name of the item
        String itemDisplayName = getItemDisplayName(shop.getSoldItem());
        String itemNameLine = itemNameFormat.replace("%item_display_name%", itemDisplayName);

        // Format price with commas (e.g., 5000 -> 5,000.00)
        String formattedPrice = String.format("%,.2f", shop.getPrice());
        String priceLine = priceFormat.replace("%price%", formattedPrice);

        // Apply PlaceholderAPI if available and the owner is online
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null && shop.getOwnerPlayer() != null) {
            itemNameLine = PlaceholderAPI.setPlaceholders(shop.getOwnerPlayer(), itemNameLine);
            priceLine = PlaceholderAPI.setPlaceholders(shop.getOwnerPlayer(), priceLine);
        }

        // Translate color codes and add to list
        lines.add(ChatColor.translateAlternateColorCodes('&', itemNameLine));
        lines.add(ChatColor.translateAlternateColorCodes('&', priceLine));

        // You can add more lines here, e.g., stock count
        // if (plugin.getConfig().getBoolean("hologram-text.show-stock", false)) {
        //     lines.add(ChatColor.translateAlternateColorCodes('&', "&7Stock: &f" + shop.getCurrentStock()));
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
        // Fallback to a user-friendly name if no custom display name
        return item.getType().name().replace("_", " ").toLowerCase();
    }
          }
              
