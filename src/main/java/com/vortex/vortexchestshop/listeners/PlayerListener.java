// src/main/java/com/vortex/vortexchestshop/listeners/PlayerListener.java

package com.vortex.vortexchestshop.listeners;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import org.bukkit.Bukkit; // Added for Bukkit.getOfflinePlayer
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta; // Added for ItemMeta

public class PlayerListener implements Listener {

    private final VortexChestShop plugin;

    public PlayerListener(VortexChestShop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Only process right-click block actions
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        // Ensure the clicked block is a chest
        if (clickedBlock == null || !(clickedBlock.getState() instanceof Chest)) {
            return;
        }

        Player player = event.getPlayer();
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));

        // Get the shop associated with the clicked chest, if any
        ChestShop shop = plugin.getShopManager().getShopAtLocation(clickedBlock.getLocation());

        // If it's not a chest shop, let the event pass (e.g., opening a normal chest)
        if (shop == null) {
            return;
        }

        // --- Shop Interaction Logic ---

        // If the player is the owner of the shop
        if (shop.getOwnerUUID().equals(player.getUniqueId())) {
            // Allow the owner to open the chest to manage stock.
            // If they right-click with an item, they might be trying to put it in or take it out.
            // If they right-click with an empty hand, they are just opening it.
            // We cancel the event first, then uncancel if it's the owner.
            event.setCancelled(false); // Allow the owner to open their own chest
            player.sendMessage(prefix + ChatColor.YELLOW + "You are managing your shop. Right-click with an empty hand to open.");
            return; // Exit as owner has interacted
        }

        // If it's not the owner, it's a potential buyer. Cancel the event to prevent opening the chest.
        event.setCancelled(true);

        // Check if the shop is active and has stock
        if (shop.getCurrentStock() <= 0) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.shop-inactive")));
            return;
        }

        // Check if the player has enough money for one transaction (quantity)
        double totalCost = shop.getPrice(); // Cost for one transaction (quantity)
        if (!plugin.getEconomyManager().has(player, totalCost)) {
            player.sendMessage(prefix + ChatColor.RED + "You don't have enough money! You need $" + String.format("%,.2f", totalCost) + ".");
            return;
        }

        // Prepare the item stack to give to the player
        ItemStack itemToGive = shop.getSoldItem().clone();
        itemToGive.setAmount(shop.getQuantity());

        // Check if player has inventory space for the items
        // This check is a bit simplistic; a more robust check would simulate adding items
        // to account for stack sizes and existing partial stacks.
        if (player.getInventory().firstEmpty() == -1 && !player.getInventory().containsAtLeast(itemToGive, shop.getQuantity())) {
            player.sendMessage(prefix + ChatColor.RED + "Your inventory is full! Make some space.");
            return;
        }

        // Attempt to withdraw money from the buyer
        boolean withdrawalSuccess = plugin.getEconomyManager().withdraw(player, totalCost);
        if (!withdrawalSuccess) {
            player.sendMessage(prefix + ChatColor.RED + "Failed to withdraw money. Please try again.");
            return;
        }

        // Calculate tax and determine amount for the owner
        double taxAmount = plugin.getEconomyManager().calculateTax(Bukkit.getOfflinePlayer(shop.getOwnerUUID()), totalCost);
        double ownerReceiveAmount = totalCost - taxAmount;

        // Attempt to deposit money to the shop owner
        boolean ownerDepositSuccess = plugin.getEconomyManager().deposit(Bukkit.getOfflinePlayer(shop.getOwnerUUID()), ownerReceiveAmount);

        if (!ownerDepositSuccess) {
            // If owner deposit failed, refund the buyer's money
            plugin.getEconomyManager().deposit(player, totalCost);
            player.sendMessage(prefix + ChatColor.RED + "Transaction failed: Could not deposit money to shop owner. Your money has been refunded.");
            Logger.severe("Failed to deposit money to shop owner " + shop.getOwnerUUID() + " for shop at " + shop.getLocation() + ". Buyer " + player.getName() + " was refunded.");
            return;
        }

        // Get the physical chest inventory
        Chest chest = (Chest) clickedBlock.getState();
        // Attempt to remove items from the chest inventory
        int removedCount = 0;
        for (ItemStack chestItem : chest.getInventory().getContents()) {
            if (chestItem != null && chestItem.isSimilar(shop.getSoldItem())) {
                int toRemove = Math.min(chestItem.getAmount(), shop.getQuantity() - removedCount);
                if (toRemove > 0) {
                    chestItem.setAmount(chestItem.getAmount() - toRemove);
                    removedCount += toRemove;
                    if (removedCount >= shop.getQuantity()) {
                        break; // Enough items removed for this transaction
                    }
                }
            }
        }
        // If not enough items were found in the chest, this indicates a discrepancy.
        // In a more robust system, you might refund the player and log an error.
        if (removedCount < shop.getQuantity()) {
            player.sendMessage(prefix + ChatColor.RED + "Error: Not enough items found in the shop chest. Transaction cancelled, money refunded.");
            plugin.getEconomyManager().deposit(player, totalCost); // Refund buyer
            // Refund owner's portion if it was already deposited
            plugin.getEconomyManager().withdraw(Bukkit.getOfflinePlayer(shop.getOwnerUUID()), ownerReceiveAmount);
            Logger.severe("Shop at " + shop.getLocation() + " had insufficient stock. Buyer " + player.getName() + " and owner " + shop.getOwnerUUID() + " were refunded/withdrawn.");
            return;
        }

        chest.update(); // Update the chest block state to reflect item removal

        // Give items to the buyer
        player.getInventory().addItem(itemToGive);

        // Send confirmation message to the buyer
        player.sendMessage(prefix + ChatColor.GREEN + "You bought " + ChatColor.YELLOW + shop.getQuantity() + " " +
                getItemDisplayName(shop.getSoldItem()) +
                ChatColor.GREEN + " for $" + String.format("%,.2f", totalCost) + ".");

        // Notify owner (if online)
        Player owner = shop.getOwnerPlayer();
        if (owner != null && owner.isOnline()) {
            owner.sendMessage(prefix + ChatColor.GREEN + player.getName() + " bought " + ChatColor.YELLOW + shop.getQuantity() + " " +
                    getItemDisplayName(shop.getSoldItem()) +
                    ChatColor.GREEN + " from your shop at " + ChatColor.YELLOW + shop.getLocation().getBlockX() + "," + shop.getLocation().getBlockY() + "," + shop.getLocation().getBlockZ() +
                    ChatColor.GREEN + " for $" + String.format("%,.2f", ownerReceiveAmount) + " (tax: $" + String.format("%,.2f", taxAmount) + ").");
        }

        // Update shop stock and display after transaction
        plugin.getShopManager().updateShopStock(shop);
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
