// src/main/java/com/vortex/vortexchestshop/managers/ShopManager.java

package com.vortex.vortexchestshop.managers;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import com.vortex.vortexchestshop.utils.Logger;
import org.bukkit.Bukkit; // Added for Bukkit.getWorld
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.ChatColor; // Added for ChatColor

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap; // For thread-safe map

public class ShopManager {

    private final VortexChestShop plugin;
    // Map to store active shops: Location (serialized string) -> ChestShop object
    // Using ConcurrentHashMap for thread-safe access if multiple threads interact
    public final Map<String, ChestShop> activeShops;

    public ShopManager(VortexChestShop plugin) {
        this.plugin = plugin;
        this.activeShops = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new chest shop at the given location for the specified player.
     * @param player The player creating the shop.
     * @param location The location of the chest.
     * @param itemStack The item being sold (single item representation).
     * @param price The price of one transaction (for the specified quantity).
     * @param quantity The quantity of items per transaction.
     * @return The created ChestShop object, or null if creation failed (e.g., shop limit, shop already exists).
     */
    public ChestShop createShop(Player player, Location location, ItemStack itemStack, double price, int quantity) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));

        // Check if a shop already exists at this location
        if (getShopAtLocation(location) != null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-shop-exists")));
            Logger.info("Player " + player.getName() + " attempted to create shop at existing shop location: " + location.toString());
            return null;
        }

        // Check player's shop limit
        if (!canCreateShop(player)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.shop-limit-reached")
                            .replace("%limit%", String.valueOf(getPlayerShopLimit(player)))));
            Logger.info("Player " + player.getName() + " reached shop limit (" + getPlayerShopCount(player.getUniqueId()) + "/" + getPlayerShopLimit(player) + ")");
            return null;
        }


        ChestShop shop = new ChestShop(
                UUID.randomUUID(), // Generate a unique ID for this shop
                player.getUniqueId(),
                location,
                itemStack,
                price,
                quantity,
                plugin.getConfig().getBoolean("shop-settings.default-floating-display-enabled", true) // Default display status from config
        );
        activeShops.put(serializeLocation(location), shop);
        saveShop(shop); // Save the new shop to config immediately

        // Update initial stock and create displays
        updateShopStock(shop); // This will also create the floating display and hologram if enabled
        plugin.getFloatingItemDisplayManager().createDisplay(shop); // Ensure display is created/updated

        player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.shop-created")));
        Logger.info("Player " + player.getName() + " created a shop at " + location.toString() + " (ID: " + shop.getId() + ")");
        return shop;
    }

    /**
     * Removes a chest shop at the given location.
     * @param location The location of the shop to remove.
     * @return true if the shop was removed, false if not found.
     */
    public boolean removeShop(Location location) {
        String serializedLoc = serializeLocation(location);
        if (activeShops.containsKey(serializedLoc)) {
            ChestShop shop = activeShops.remove(serializedLoc);
            // Remove associated floating item display and hologram
            plugin.getFloatingItemDisplayManager().removeDisplay(shop);
            plugin.getHologramManager().removeHologram(shop);
            deleteShop(shop); // Delete from config
            Logger.info("Removed shop at " + location.toString() + " (ID: " + shop.getId() + ")");
            return true;
        }
        Logger.warning("Attempted to remove non-existent shop at " + location.toString());
        return false;
    }

    /**
     * Gets a ChestShop object by its location.
     * @param location The location of the chest.
     * @return The ChestShop object, or null if no shop exists at that location.
     */
    public ChestShop getShopAtLocation(Location location) {
        return activeShops.get(serializeLocation(location));
    }

    /**
     * Gets the number of shops a player currently owns.
     * @param playerUUID The UUID of the player.
     * @return The count of shops owned by the player.
     */
    public int getPlayerShopCount(UUID playerUUID) {
        return (int) activeShops.values().stream()
                .filter(shop -> shop.getOwnerUUID().equals(playerUUID))
                .count();
    }

    /**
     * Determines if a player can create another shop based on their shop limit.
     * @param player The player to check.
     * @return true if the player can create another shop, false otherwise.
     */
    public boolean canCreateShop(Player player) {
        int currentShops = getPlayerShopCount(player.getUniqueId());
        int maxShops = getPlayerShopLimit(player);
        return currentShops < maxShops;
    }

    /**
     * Gets the maximum number of shops a player can create.
     * This method integrates with LuckPerms or other permission plugins
     * to determine the limit based on player groups/permissions.
     * Permissions are checked in descending order (e.g., .100, .99, ...)
     * to ensure the highest granted limit is applied.
     *
     * @param player The player to check.
     * @return The maximum number of shops allowed for the player.
     */
    public int getPlayerShopLimit(Player player) {
        // Start with F2P base limit from config
        int limit = plugin.getConfig().getInt("shop-settings.f2p-max-shops", 5);

        // Check for permission-based limits (LuckPerms integration would go here)
        // Example permissions:
        // - vortexchestshop.player.maxshops.f2p.<amount>
        // - vortexchestshop.premium.slots.<amount>
        // We iterate from a high number down to 0 to find the highest granted limit.
        for (int i = 200; i >= 0; i--) { // Max limit can be adjusted, 200 is arbitrary high
            if (player.hasPermission("vortexchestshop.premium.slots." + i)) {
                limit = i;
                break; // Found highest premium limit, no need to check lower
            }
            if (player.hasPermission("vortexchestshop.player.maxshops.f2p." + i)) {
                // Only update if it's higher than the current F2P default
                if (i > limit) {
                    limit = i;
                }
                // Don't break here, as a premium slot might still be higher
            }
        }

        // Future: Add slots from playtime, voting, quests.
        // This would involve a separate progression manager that tracks these and adds to the limit.
        // Example: limit += plugin.getProgressionManager().getEarnedSlots(player);

        return limit;
    }

    /**
     * Updates the current stock of a shop by counting the sold item in its physical chest.
     * Also triggers updates for the floating display and hologram.
     * @param shop The ChestShop object to update.
     */
    public void updateShopStock(ChestShop shop) {
        Block block = shop.getLocation().getBlock();
        // Ensure the block is still a chest
        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            int currentStock = 0;
            // Iterate through chest inventory to count items matching the sold item
            for (ItemStack item : chest.getInventory().getContents()) {
                // Use isSimilar to match item type, name, lore, enchantments, etc.
                if (item != null && item.isSimilar(shop.getSoldItem())) {
                    currentStock += item.getAmount();
                }
            }
            // Only update if stock has actually changed to avoid unnecessary updates
            if (shop.getCurrentStock() != currentStock) {
                shop.setCurrentStock(currentStock); // This will trigger display/hologram updates via setter
                Logger.debug("Shop " + shop.getId() + " stock updated to: " + currentStock);
            }
        } else {
            // The block is no longer a chest, indicating it was broken or changed.
            // Remove the shop from the system.
            Logger.warning("Shop at " + shop.getLocation().toString() + " is no longer a chest. Removing shop automatically.");
            removeShop(shop.getLocation());
        }
    }

    /**
     * Loads all shops from the plugin's configuration file (`config.yml`).
     * This is a basic implementation for persistence. For large servers, a database (e.g., SQLite, MySQL)
     * is highly recommended for better performance and scalability.
     */
    public void loadShops() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection shopsSection = config.getConfigurationSection("shops");
        if (shopsSection == null) {
            Logger.info("No shops found in config.yml to load.");
            return;
        }

        int loadedCount = 0;
        for (String shopIdString : shopsSection.getKeys(false)) {
            ConfigurationSection shopData = shopsSection.getConfigurationSection(shopIdString);
            if (shopData == null) {
                Logger.warning("Skipping empty shop data section for ID: " + shopIdString);
                continue;
            }

            try {
                UUID id = UUID.fromString(shopIdString);
                UUID ownerUUID = UUID.fromString(shopData.getString("ownerUUID"));
                Location location = deserializeLocation(shopData.getString("location"));
                ItemStack soldItem = shopData.getItemStack("soldItem");
                double price = shopData.getDouble("price");
                int quantity = shopData.getInt("quantity");
                boolean displayEnabled = shopData.getBoolean("displayEnabled", true);

                // Basic validation for loaded data
                if (location == null || soldItem == null || location.getWorld() == null) {
                    Logger.warning("Skipping invalid shop data for ID: " + shopIdString + " (missing location, world, or item).");
                    continue;
                }
                // Ensure the chest block actually exists at the location
                if (!(location.getBlock().getState() instanceof Chest)) {
                    Logger.warning("Shop at " + location.toString() + " (ID: " + shopIdString + ") is not a chest. Skipping loading and marking for removal.");
                    // Optionally, remove this entry from config if it's invalid
                    config.set("shops." + shopIdString, null);
                    continue;
                }

                ChestShop shop = new ChestShop(id, ownerUUID, location, soldItem, price, quantity, displayEnabled);
                activeShops.put(serializeLocation(location), shop);
                // Immediately update stock and create displays upon loading
                updateShopStock(shop);
                loadedCount++;
            } catch (IllegalArgumentException e) {
                Logger.severe("Failed to parse UUID for shop " + shopIdString + ": " + e.getMessage() + ". Skipping.");
            } catch (Exception e) {
                Logger.severe("Failed to load shop " + shopIdString + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        plugin.saveConfig(); // Save to remove any invalid entries marked for removal
        Logger.info("Loaded " + loadedCount + " shops from config.yml.");
    }

    /**
     * Saves all currently active shops to the plugin's configuration file.
     * This method rewrites the entire 'shops' section.
     */
    public void saveShops() {
        FileConfiguration config = plugin.getConfig();
        config.set("shops", null); // Clear existing shops to rewrite all active ones

        for (ChestShop shop : activeShops.values()) {
            String shopId = shop.getId().toString();
            config.set("shops." + shopId + ".ownerUUID", shop.getOwnerUUID().toString());
            config.set("shops." + shopId + ".location", serializeLocation(shop.getLocation()));
            config.set("shops." + shopId + ".soldItem", shop.getSoldItem());
            config.set("shops." + shopId + ".price", shop.getPrice());
            config.set("shops." + shopId + ".quantity", shop.getQuantity());
            config.set("shops." + shopId + ".displayEnabled", shop.isDisplayEnabled());
        }
        plugin.saveConfig();
        Logger.info("Saved " + activeShops.size() + " shops to config.yml.");
    }

    /**
     * Saves a single shop to the plugin's configuration file.
     * This is called when a shop is created or its properties are updated.
     * @param shop The ChestShop object to save.
     */
    private void saveShop(ChestShop shop) {
        FileConfiguration config = plugin.getConfig();
        String shopId = shop.getId().toString();
        config.set("shops." + shopId + ".ownerUUID", shop.getOwnerUUID().toString());
        config.set("shops." + shopId + ".location", serializeLocation(shop.getLocation()));
        config.set("shops." + shopId + ".soldItem", shop.getSoldItem());
        config.set("shops." + shopId + ".price", shop.getPrice());
        config.set("shops." + shopId + ".quantity", shop.getQuantity());
        config.set("shops." + shopId + ".displayEnabled", shop.isDisplayEnabled());
        plugin.saveConfig();
        Logger.debug("Saved shop " + shop.getId() + " to config.");
    }

    /**
     * Deletes a single shop's data from the plugin's configuration file.
     * @param shop The ChestShop object to delete.
     */
    private void deleteShop(ChestShop shop) {
        FileConfiguration config = plugin.getConfig();
        config.set("shops." + shop.getId().toString(), null); // Set section to null to remove it
        plugin.saveConfig();
        Logger.debug("Deleted shop " + shop.getId() + " from config.");
    }

    /**
     * Serializes a Location object to a string for storage in config.
     * Format: worldName,x,y,z
     * @param location The Location to serialize.
     * @return The serialized string.
     */
    private String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            Logger.severe("Attempted to serialize null location or location with null world!");
            return ""; // Return empty string for invalid locations
        }
        return String.format("%s,%d,%d,%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * Deserializes a string back into a Location object.
     * @param serializedLocation The string to deserialize.
     * @return The deserialized Location, or null if parsing fails or world is not found.
     */
    private Location deserializeLocation(String serializedLocation) {
        if (serializedLocation == null || serializedLocation.isEmpty()) return null;
        String[] parts = serializedLocation.split(",");
        if (parts.length != 4) {
            Logger.warning("Invalid serialized location format: " + serializedLocation);
            return null;
        }
        try {
            // Ensure the world exists before creating the Location object
            if (Bukkit.getWorld(parts[0]) == null) {
                Logger.warning("World '" + parts[0] + "' not found for deserialized location: " + serializedLocation);
                return null;
            }
            return new Location(
                    Bukkit.getWorld(parts[0]), // Use Bukkit.getWorld to get the World object
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
        } catch (NumberFormatException e) {
            Logger.severe("Failed to parse coordinates for location: " + serializedLocation + " - " + e.getMessage());
            return null;
        }
    }
}
