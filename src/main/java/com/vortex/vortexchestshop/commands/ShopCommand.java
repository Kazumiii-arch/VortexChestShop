// src/main/java/com/vortex/vortexchestshop/commands/ShopCommand.java

package com.vortex.vortexchestshop.commands;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.models.ChestShop;
import com.vortex.vortexchestshop.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta; // Import for ItemMeta

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final VortexChestShop plugin;

    public ShopCommand(VortexChestShop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                handleCreateCommand(player, args);
                break;
            case "remove":
                handleRemoveCommand(player);
                break;
            case "setitem":
                handleSetItemCommand(player);
                break;
            case "setprice":
                handleSetPriceCommand(player, args);
                break;
            case "setquantity":
                handleSetQuantityCommand(player, args);
                break;
            case "setdisplay":
                handleSetDisplayCommand(player, args);
                break;
            case "admin":
                handleAdminCommand(player, args);
                break;
            case "stats":
                handleStatsCommand(player);
                break;
            case "reload": // Added direct reload for convenience, also under admin
                handleAdminReload(player);
                break;
            default:
                player.sendMessage(prefix + ChatColor.RED + "Unknown command. Use /shop help for a list of commands.");
                break;
        }

        return true;
    }

    /**
     * Sends the help message to the player, listing available commands.
     * @param player The player to send the help message to.
     */
    private void sendHelpMessage(Player player) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&b--- VortexChestShop Commands ---"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop create <price> <quantity> &7- Create a new shop with the item in your hand."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop remove &7- Remove the shop you are looking at."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop setitem &7- Set the item being sold in your shop to the item in your hand."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop setprice <price> &7- Set the price of the item in your shop."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop setquantity <quantity> &7- Set the quantity per transaction in your shop."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop setdisplay [on/off] &7- Toggle floating item display for your shop."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e/shop stats &7- View your shop performance statistics."));
        if (player.hasPermission("vortexchestshop.admin.use")) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6--- Admin Commands ---"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/shop admin setdisplay <player> [on/off] &7- Toggle display for another player's shops."));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/shop admin grantslot <player> <amount> &7- Grant extra shop slots."));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/shop admin unlockarea <player> <zone> &7- Unlock premium market access."));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6/shop reload &7- Reload plugin configuration.")); // Also listed here for clarity
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b----------------------------"));
    }

    /**
     * Handles the /shop create command.
     * @param player The player executing the command.
     * @param args The command arguments.
     */
    private void handleCreateCommand(Player player, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        if (!player.hasPermission("vortexchestshop.player.createshop")) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: /shop create <price> <quantity>");
            return;
        }

        double price;
        int quantity;
        try {
            price = Double.parseDouble(args[1]);
            quantity = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + ChatColor.RED + "Invalid price or quantity. Please use numbers.");
            return;
        }

        if (price <= 0 || quantity <= 0) {
            player.sendMessage(prefix + ChatColor.RED + "Price and quantity must be positive numbers.");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5); // Get block player is looking at within 5 blocks
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-a-chest")));
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(prefix + ChatColor.RED + "You must be holding an item to sell.");
            return;
        }

        // Create a clone to avoid modifying the original item in hand
        ItemStack soldItem = itemInHand.clone();
        soldItem.setAmount(1); // Store as single item, quantity handles amount per transaction

        plugin.getShopManager().createShop(player, targetBlock.getLocation(), soldItem, price, quantity);
    }

    /**
     * Handles the /shop remove command.
     * @param player The player executing the command.
     */
    private void handleRemoveCommand(Player player) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        if (!player.hasPermission("vortexchestshop.player.createshop")) { // Same permission for creating/removing
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-a-chest")));
            return;
        }

        ChestShop shop = plugin.getShopManager().getShopAtLocation(targetBlock.getLocation());
        if (shop == null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-shop-not-found")));
            return;
        }

        // Allow owner or admin to remove the shop
        if (!shop.getOwnerUUID().equals(player.getUniqueId()) && !player.hasPermission("vortexchestshop.admin.removeshop")) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-your-shop")));
            return;
        }

        if (plugin.getShopManager().removeShop(targetBlock.getLocation())) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.shop-removed")));
        } else {
            player.sendMessage(prefix + ChatColor.RED + "Failed to remove shop.");
        }
    }

    /**
     * Handles the /shop setitem command.
     * @param player The player executing the command.
     */
    private void handleSetItemCommand(Player player) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-a-chest")));
            return;
        }

        ChestShop shop = plugin.getShopManager().getShopAtLocation(targetBlock.getLocation());
        if (shop == null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-shop-not-found")));
            return;
        }

        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-your-shop")));
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage(prefix + ChatColor.RED + "You must be holding an item to set as sold item.");
            return;
        }

        ItemStack newSoldItem = itemInHand.clone();
        newSoldItem.setAmount(1); // Store as single item

        shop.setSoldItem(newSoldItem);
        // Save the updated shop to config
        plugin.getShopManager().saveShops(); // Re-save all shops for simplicity, or implement single shop save
        player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.item-changed")
                .replace("%item_name%", getItemDisplayName(newSoldItem))));
    }

    /**
     * Handles the /shop setprice command.
     * @param player The player executing the command.
     * @param args The command arguments.
     */
    private void handleSetPriceCommand(Player player, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        if (args.length < 2) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: /shop setprice <price>");
            return;
        }

        double newPrice;
        try {
            newPrice = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + ChatColor.RED + "Invalid price. Please use a number.");
            return;
        }

        if (newPrice <= 0) {
            player.sendMessage(prefix + ChatColor.RED + "Price must be a positive number.");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-a-chest")));
            return;
        }

        ChestShop shop = plugin.getShopManager().getShopAtLocation(targetBlock.getLocation());
        if (shop == null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-shop-not-found")));
            return;
        }

        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-your-shop")));
            return;
        }

        shop.setPrice(newPrice);
        plugin.getShopManager().saveShops();
        player.sendMessage(prefix + ChatColor.GREEN + "Shop price updated to: " + ChatColor.YELLOW + String.format("%,.2f", newPrice));
    }

    /**
     * Handles the /shop setquantity command.
     * @param player The player executing the command.
     * @param args The command arguments.
     */
    private void handleSetQuantityCommand(Player player, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        if (args.length < 2) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: /shop setquantity <quantity>");
            return;
        }

        int newQuantity;
        try {
            newQuantity = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + ChatColor.RED + "Invalid quantity. Please use a whole number.");
            return;
        }

        if (newQuantity <= 0) {
            player.sendMessage(prefix + ChatColor.RED + "Quantity must be a positive number.");
            return;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-a-chest")));
            return;
        }

        ChestShop shop = plugin.getShopManager().getShopAtLocation(targetBlock.getLocation());
        if (shop == null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-shop-not-found")));
            return;
        }

        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-your-shop")));
            return;
        }

        shop.setQuantity(newQuantity);
        plugin.getShopManager().saveShops();
        player.sendMessage(prefix + ChatColor.GREEN + "Shop quantity per transaction updated to: " + ChatColor.YELLOW + newQuantity);
    }

    /**
     * Handles the /shop setdisplay command.
     * @param player The player executing the command.
     * @param args The command arguments.
     */
    private void handleSetDisplayCommand(Player player, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Chest)) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-a-chest")));
            return;
        }

        ChestShop shop = plugin.getShopManager().getShopAtLocation(targetBlock.getLocation());
        if (shop == null) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-shop-not-found")));
            return;
        }

        if (!shop.getOwnerUUID().equals(player.getUniqueId())) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-not-your-shop")));
            return;
        }

        boolean enable;
        if (args.length < 2) {
            // Toggle if no argument provided
            enable = !shop.isDisplayEnabled();
        } else {
            String state = args[1].toLowerCase();
            if (state.equals("on")) {
                enable = true;
            } else if (state.equals("off")) {
                enable = false;
            } else {
                player.sendMessage(prefix + ChatColor.RED + "Usage: /shop setdisplay [on/off]");
                return;
            }
        }

        shop.setDisplayEnabled(enable);
        plugin.getShopManager().saveShops();
        player.sendMessage(prefix + (enable ?
                ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.admin-display-toggle-on").replace("%player%", player.getName())) :
                ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.admin-display-toggle-off").replace("%player%", player.getName()))));
    }

    /**
     * Handles the /shop admin sub-commands.
     * @param player The player executing the command.
     * @param args The command arguments.
     */
    private void handleAdminCommand(Player player, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        if (!player.hasPermission("vortexchestshop.admin.use")) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: /shop admin <subcommand>");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "setdisplay":
                handleAdminSetDisplay(player, args);
                break;
            case "grantslot":
                handleAdminGrantSlot(player, args);
                break;
            case "unlockarea":
                handleAdminUnlockArea(player, args);
                break;
            case "reload":
                handleAdminReload(player);
                break;
            default:
                player.sendMessage(prefix + ChatColor.RED + "Unknown admin subcommand. Use /shop help for admin commands.");
                break;
        }
    }

    /**
     * Handles the /shop admin setdisplay <player> [on/off] command.
     * @param player The admin executing the command.
     * @param args The command arguments.
     */
    private void handleAdminSetDisplay(Player player, String[] args) {
        String prefix = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.prefix"));
        if (!player.hasPermission("vortexchestshop.admin.setdisplay")) {
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.no-permission")));
            return;
        }
        if (args.length < 4) {
            player.sendMessage(prefix + ChatColor.RED + "Usage: /shop admin setdisplay <player> [on/off]");
            return;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[2]);
        if (targetPlayer == null || !targetPlayer.hasPlayedBefore()) { // Check if player exists
            player.sendMessage(prefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("messages.error-player-not-fo
