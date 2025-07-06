// src/main/java/com/vortex/vortexchestshop/managers/FloatingItemDisplayManager.java

package com.vortex.vortexchestshop.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import com.vortex.vortexchestshop.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional; // Correct import for java.util.Optional
import java.util.UUID;

public class FloatingItemDisplayManager {

    private final VortexChestShop plugin;
    // Map to store ArmorStand entities for each shop
    private final Map<UUID, ArmorStand> shopDisplays;
    // Map to store running tasks for each shop's display updates (e.g., rotation)
    private final Map<UUID, BukkitTask> shopDisplayTasks;

    public FloatingItemDisplayManager(VortexChestShop plugin) {
        this.plugin = plugin;
        this.shopDisplays = new HashMap<>();
        this.shopDisplayTasks = new HashMap<>();
    }

    /**
     * Creates a floating item display for a given ChestShop.
     * This method spawns a hidden ArmorStand entity that holds the item.
     * For advanced features like glowing outlines and long-range visibility,
     * ProtocolLib packet manipulation is used here.
     *
     * @param shop The ChestShop to create a display for.
     */
    public void createDisplay(ChestShop shop) {
        // If display is disabled for this shop or stock is zero, do not create
        if (!shop.isDisplayEnabled() || shop.getCurrentStock() <= 0) {
            return;
        }
        // If a display already exists for this shop, update it instead
        if (shopDisplays.containsKey(shop.getId())) {
            updateDisplay(shop);
            return;
        }

        Location displayLocation = getDisplayLocation(shop.getLocation());

        // Ensure the chunk is loaded before attempting to spawn the ArmorStand
        if (!displayLocation.getChunk().isLoaded()) {
            displayLocation.getChunk().load(true);
        }

        // Spawn a temporary ArmorStand for the item display
        ArmorStand armorStand = (ArmorStand) displayLocation.getWorld().spawnEntity(displayLocation, EntityType.ARMOR_STAND);
        armorStand.setGravity(false); // Make it float in the air
        armorStand.setBasePlate(false); // No base plate visible
        armorStand.setArms(false); // No arms visible
        armorStand.setVisible(false); // Make the armor stand itself invisible
        armorStand.setSmall(true); // Make it small (optional, can be false for larger items)
        armorStand.setMarker(true); // Prevents interaction and collision with players/entities
        armorStand.setCanPickupItems(false); // Prevents picking up items
        armorStand.setPersistent(false); // Do not save with chunk, will be removed on plugin disable

        // Set the item the armor stand is holding in its helmet slot
        armorStand.getEquipment().setHelmet(shop.getSoldItem().clone());

        shopDisplays.put(shop.getId(), armorStand);
        Logger.debug("Created floating display for shop " + shop.getId() + " at " + displayLocation.toString());

        // Apply premium visual effects using ProtocolLib
        applyPremiumVisuals(shop, armorStand);
        // Start the rotation task for this specific shop
        startDisplayTask(shop,
                plugin.getConfig().getDouble("floating-display.base-rotation-speed", 0.05),
                plugin.getConfig().getDouble("floating-display.premium-rotation-speed", 0.1),
                plugin.getConfig().getLong("performance.floating-item-tick-rate", 1));
    }

    /**
     * Updates an existing floating item display for a given ChestShop.
     * This includes updating the item, stock visibility, and visual effects.
     * @param shop The ChestShop to update.
     */
    public void updateDisplay(ChestShop shop) {
        ArmorStand armorStand = shopDisplays.get(shop.getId());

        // If display is disabled or stock is zero, remove the display
        if (!shop.isDisplayEnabled() || shop.getCurrentStock() <= 0) {
            if (armorStand != null) {
                removeDisplay(shop);
            }
            return;
        }

        // If display doesn't exist but should, create it
        if (armorStand == null) {
            createDisplay(shop);
            return;
        }

        // Update the item if it has changed
        ItemStack currentHelmet = armorStand.getEquipment().getHelmet();
        if (currentHelmet == null || !currentHelmet.isSimilar(shop.getSoldItem())) {
            armorStand.getEquipment().setHelmet(shop.getSoldItem().clone());
        }

        // Re-apply premium visual effects in case settings or player permissions changed
        applyPremiumVisuals(shop, armorStand);

        // Update associated hologram
        plugin.getHologramManager().updateHologram(shop);
        Logger.debug("Updated floating display for shop " + shop.getId());
    }

    /**
     * Removes a floating item display for a given ChestShop.
     * @param shop The ChestShop to remove the display for.
     */
    public void removeDisplay(ChestShop shop) {
        ArmorStand armorStand = shopDisplays.remove(shop.getId());
        if (armorStand != null) {
            armorStand.remove(); // Remove the ArmorStand entity from the world
            Logger.debug("Removed floating display for shop " + shop.getId());
        }
        plugin.getHologramManager().removeHologram(shop); // Also remove associated hologram
        stopDisplayTask(shop.getId()); // Stop its rotation task
    }

    /**
     * Cleans up all active floating item displays and their associated tasks.
     * Called when the plugin is disabled to prevent lingering entities.
     */
    public void cleanupAllDisplays() {
        // Remove all spawned ArmorStands
        for (ArmorStand armorStand : shopDisplays.values()) {
            if (armorStand != null && armorStand.isValid()) {
                armorStand.remove();
            }
        }
        shopDisplays.clear();

        // Cancel all running display tasks
        shopDisplayTasks.values().forEach(BukkitTask::cancel);
        shopDisplayTasks.clear();

        // Clean up all holograms managed by HologramManager
        plugin.getHologramManager().cleanupAllHolograms();
        Logger.info("Cleaned up all floating item displays and holograms.");
    }

    /**
     * Starts the periodic tasks for updating floating item displays (e.g., rotation)
     * and a global task for checking shop stock.
     */
    public void startDisplayTasks() {
        // Iterate over existing shops and start their individual rotation tasks
        plugin.getShopManager().activeShops.values().forEach(shop -> {
            if (shop.isDisplayEnabled() && shop.getCurrentStock() > 0) {
                startDisplayTask(shop,
                        plugin.getConfig().getDouble("floating-display.base-rotation-speed", 0.05),
                        plugin.getConfig().getDouble("floating-display.premium-rotation-speed", 0.1),
                        plugin.getConfig().getLong("performance.floating-item-tick-rate", 1));
            }
        });

        // Schedule a global repeating task to check and update shop stock periodically
        long stockCheckRate = plugin.getConfig().getLong("performance.stock-check-rate", 40);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            plugin.getShopManager().activeShops.values().forEach(shop -> {
                plugin.getShopManager().updateShopStock(shop);
            });
        }, stockCheckRate, stockCheckRate);

        Logger.info("Started floating item display update and stock check tasks.");
    }

    /**
     * Stops all periodic tasks related to floating item displays.
     */
    public void stopDisplayTasks() {
        shopDisplayTasks.values().forEach(BukkitTask::cancel);
        shopDisplayTasks.clear();
        Logger.info("Stopped all floating item display update tasks.");
    }

    /**
     * Starts the rotation and particle effect task for a specific shop's display.
     * @param shop The ChestShop to start the task for.
     * @param baseRotationSpeed The base rotation speed for F2P shops.
     * @param premiumRotationSpeed The enhanced rotation speed for premium shops.
     * @param tickRate The interval in ticks for updating the display.
     */
    private void startDisplayTask(ChestShop shop, double baseRotationSpeed, double premiumRotationSpeed, long tickRate) {
        // Cancel any existing task for this shop to prevent duplicates
        stopDisplayTask(shop.getId());

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            ArmorStand armorStand = shopDisplays.get(shop.getId());
            if (armorStand == null || !armorStand.isValid()) {
                // ArmorStand is gone or invalid, stop and remove this task
                stopDisplayTask(shop.getId());
                return;
            }

            // Determine rotation speed based on the shop owner's premium permission
            double rotationSpeed = baseRotationSpeed;
            if (shop.getOwnerPlayer() != null && shop.getOwnerPlayer().hasPermission("vortexchestshop.premium.visuals.enhancedrotation")) {
                rotationSpeed = premiumRotationSpeed;
            }

            // Rotate the item around its Y-axis
            EulerAngle currentHeadPose = armorStand.getHeadPose();
            EulerAngle newHeadPose = currentHeadPose.setY(currentHeadPose.getY() + rotationSpeed);
            armorStand.setHeadPose(newHeadPose);

            // Apply particle effects if enabled for premium shops
            if (shop.getOwnerPlayer() != null &&
                shop.getOwnerPlayer().hasPermission("vortexchestshop.premium.visuals.particles") &&
                plugin.getConfig().getBoolean("floating-display.premium-particles-enabled", true)) {
                Location particleLoc = armorStand.getLocation().add(0, 0.5, 0); // Slightly above the item
                String particleTypeName = plugin.getConfig().getString("floating-display.premium-particle-type", "SPARKLE");
                int particleAmount = plugin.getConfig().getInt("floating-display.premium-particle-amount", 5);
                try {
                    org.bukkit.Particle particle = org.bukkit.Particle.valueOf(particleTypeName.toUpperCase());
                    armorStand.getWorld().spawnParticle(particle, particleLoc, particleAmount, 0.1, 0.1, 0.1, 0.01);
                } catch (IllegalArgumentException e) {
                    Logger.warning("Invalid particle type specified in config: " + particleTypeName + ". Defaulting to SPARKLE.");
                }
            }

        }, 0L, tickRate); // Start immediately, repeat every 'tickRate' ticks
        shopDisplayTasks.put(shop.getId(), task);
    }

    /**
     * Stops the rotation task for a specific shop's display.
     * @param shopId The UUID of the shop whose task should be stopped.
     */
    private void stopDisplayTask(UUID shopId) {
        BukkitTask task = shopDisplayTasks.remove(shopId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Calculates the exact location for the floating item display above a chest.
     * @param chestLocation The location of the chest block.
     * @return The Location for the ArmorStand.
     */
    private Location getDisplayLocation(Location chestLocation) {
        // Adjust Y-coordinate to be slightly above the chest, centered horizontally
        return chestLocation.clone().add(0.5, 1.2, 0.5); // X+0.5, Z+0.5 for center, Y+1.2 for height
    }

    /**
     * Applies premium visual effects to the ArmorStand using ProtocolLib if available.
     * This includes making the item glow (glowing outline) and potentially extending its render distance.
     * @param shop The ChestShop object.
     * @param armorStand The ArmorStand entity.
     */
    private void applyPremiumVisuals(ChestShop shop, ArmorStand armorStand) {
        // Check if ProtocolLib is installed and enabled
        boolean protocolLibAvailable = Bukkit.getPluginManager().getPlugin("ProtocolLib") != null &&
                                       Bukkit.getPluginManager().getPlugin("ProtocolLib").isEnabled();
        if (!protocolLibAvailable) {
            return; // Cannot apply advanced visuals without ProtocolLib
        }

        // Get configuration settings for premium visuals
        boolean glowingConfigEnabled = plugin.getConfig().getBoolean("floating-display.premium-glowing-outline-enabled", true);
        boolean longRangeConfigEnabled = plugin.getConfig().getBoolean("floating-display.premium-long-range-visibility", true);

        // Determine if the shop owner has premium permissions for these visuals
        boolean hasGlowingPermission = shop.getOwnerPlayer() != null && shop.getOwnerPlayer().hasPermission("vortexchestshop.premium.visuals.glowingoutline");
        boolean hasLongRangePermission = shop.getOwnerPlayer() != null && shop.getOwnerPlayer().hasPermission("vortexchestshop.premium.visuals.longrange");

        try {
            // Create a packet to modify entity metadata
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_METADATA);
            packet.getIntegers().write(0, armorStand.getEntityId()); // Set the entity ID

            // Get the current data watcher for the ArmorStand
            WrappedDataWatcher watcher = new WrappedDataWatcher(armorStand);

            // --- Glowing Outline (Entity Flags - Byte 0, Bit 6) ---
            // The first byte (index 0) in the data watcher contains a bitmask for various entity flags.
            // Bit 6 (value 0x40 or 64) controls the glowing effect.
            byte entityFlags = watcher.getByte(0); // Get current entity flags
            if (glowingConfigEnabled && hasGlowingPermission) {
                entityFlags = (byte) (entityFlags | (1 << 6)); // Set bit 6 to enable glowing
            } else {
                entityFlags = (byte) (entityFlags & ~(1 << 6)); // Unset bit 6 to disable glowing
            }
            // Update the watcher object for entity flags
            watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class)), entityFlags);

            // --- Long-Range Visibility (Custom Name & Custom Name Visible - Index 2 & 3) ---
            // A common trick to extend render distance for entities is to give them a custom name
            // and make that name visible, even if the name is empty. Minecraft's client often
            // renders entities with visible names from further away.
            if (longRangeConfigEnabled && hasLongRangePermission) {
                // Set custom name (empty string, but present)
                watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), Optional.of(WrappedChatComponent.fromText("").getHandle()));
                // Make custom name visible
                watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), true);
            } else {
                // Remove custom name and make it not visible
                watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(2, WrappedDataWatcher.Registry.getChatComponentSerializer(true)), Optional.empty());
                watcher.setObject(new WrappedDataWatcher.WrappedDataWatcherObject(3, WrappedDataWatcher.Registry.get(Boolean.class)), false);
            }

            // Set the modified data watcher to the packet
            packet.getWatchableCollections().write(0, watcher.getWatchableObjects());

            // Send the packet to all online players to update their client's view of the ArmorStand
            for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                ProtocolLibrary.getProtocolManager().sendServerPacket(onlinePlayer, packet);
            }

        } catch (Exception e) {
            Logger.severe("Failed to apply premium visual effects using ProtocolLib for shop " + shop.getId() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
                              }
