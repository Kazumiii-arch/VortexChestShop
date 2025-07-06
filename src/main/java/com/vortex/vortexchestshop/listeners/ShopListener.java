// src/main/java/com/vortex/vortexchestshop/listeners/ShopListener.java

package com.vortex.vortexchestshop.listeners;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import com.vortex.vortexchestshop.utils.Logger; // Added for logging
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder; // Added for clarity, though Chest already implements it

public class ShopListener implements Listener {

    private final VortexChestShop plugin;

    public ShopListener(VortexChestShop plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles the breaking of blocks. Ensures that only shop owners or admins can break shop chests.
     * @param event The BlockBreakEvent.
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block brokenBlock = event.getBlock();
        // Check if the broken block is a chest
        if (!(brokenBlock.getState() instanceof Chest)) {
            return; // Not a chest, so not a shop
        }

        // Get the shop associated with the broken chest, if any
        ChestShop shop = plugin.getShopManager().getShopAtLocation(brokenBlock.getLocation());
        if (shop == null) {
            return; // Not a registered chest shop
        }

        Player player = event.getPlayer();
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));

        // Check if the player is the owner or has admin permission to break shops
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("vortexchestshop.admin.removeshop")) {
            event.setCancelled(true); // Cancel the event if not authorized
            player.sendMessage(prefix + ChatColor.RED + "You cannot break this chest shop!");
            Logger.info(player.getName() + " attempted to break unauthorized shop at " + brokenBlock.getLocation());
            return;
        }

        // If authorized, remove the shop from the system
        if (plugin.getShopManager().removeShop(brokenBlock.getLocation())) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.shop-removed")));
            Logger.info("Shop at " + brokenBlock.getLocation() + " removed by " + player.getName());
        } else {
            player.sendMessage(prefix + ChatColor.RED + "Failed to remove shop data. Please contact an admin.");
            Logger.severe("Failed to remove shop data for shop at " + brokenBlock.getLocation() + " broken by " + player.getName());
        }
    }

    /**
     * Handles the placing of blocks. Prevents placing chests directly on top of existing shop locations.
     * @param event The BlockPlaceEvent.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        // Only interested if a chest is being placed
        if (!(placedBlock.getState() instanceof Chest)) {
            return;
        }

        // Check if a shop already exists at the location where the chest is being placed
        if (plugin.getShopManager().getShopAtLocation(placedBlock.getLocation()) != null) {
            event.setCancelled(true); // Cancel the event to prevent placing
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix")) +
                    ChatColor.RED + "Cannot place a chest here, a shop already exists or was recently removed.");
            Logger.info(event.getPlayer().getName() + " attempted to place a chest on an existing shop location at " + placedBlock.getLocation());
            return;
        }
        // Future improvement: Add checks for double chests being formed next to existing shops
    }

    /**
     * Handles the closing of inventories. Updates the stock of a chest shop when its inventory is closed.
     * @param event The InventoryCloseEvent.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        // Ensure the inventory belongs to a chest
        if (!(event.getInventory().getHolder() instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) event.getInventory().getHolder();
        ChestShop shop = plugin.getShopManager().getShopAtLocation(chest.getLocation());

        // If it's a registered chest shop
        if (shop != null) {
            // Update shop stock when its chest inventory is closed
            plugin.getShopManager().updateShopStock(shop);
            // Notify the player (if they are the owner) about the stock update
            if (event.getPlayer().getUniqueId().equals(shop.getOwnerUUID())) {
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix")) +
                        ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.stock-updated")
                                .replace("%stock%", String.valueOf(shop.getCurrentStock()))));
            }
            Logger.debug("Shop stock updated for shop at " + chest.getLocation() + " after inventory close.");
        }
    }

    /**
     * Handles the opening of inventories. Prevents non-owners from directly opening shop chests.
     * @param event The InventoryOpenEvent.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        // Ensure the inventory belongs to a chest
        if (!(event.getInventory().getHolder() instanceof Chest)) {
            return;
        }
        Chest chest = (Chest) event.getInventory().getHolder();
        ChestShop shop = plugin.getShopManager().getShopAtLocation(chest.getLocation());

        // If it's a registered chest shop
        if (shop != null) {
            // If a player other than the owner tries to open a shop chest, cancel the event.
            // The PlayerListener handles right-click for buying, this ensures direct inventory access is blocked.
            if (!shop.getOwnerUUID().equals(event.getPlayer().getUniqueId())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix")) +
                        ChatColor.RED + "You cannot open another player's chest shop directly. Right-click to buy!");
                Logger.info(event.getPlayer().getName() + " attempted to open unauthorized shop at " + chest.getLocation());
            }
            // Owners are allowed to open their own shops to manage stock, as handled in PlayerListener.
        }
    }
              }
