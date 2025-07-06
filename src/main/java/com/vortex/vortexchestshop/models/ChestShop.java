// src/main/java/com/vortex/vortexchestshop/models/ChestShop.java

package com.vortex.vortexchestshop.models;

import com.vortex.vortexchestshop.VortexChestShop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ChestShop {

    private final UUID id; // Unique ID for this shop instance
    private final UUID ownerUUID; // UUID of the player who owns this shop
    private final Location location; // Location of the chest block for this shop
    private ItemStack soldItem; // The item being sold (stored as a single item, quantity handled separately)
    private double price; // Price per transaction (for the specified quantity)
    private int quantity; // Quantity of items sold per transaction
    private int currentStock; // Current available stock of the sold item in the chest
    private boolean displayEnabled; // Whether the floating item display is enabled for this shop

    /**
     * Constructor for a new ChestShop.
     * @param id A unique UUID for this shop instance.
     * @param ownerUUID The UUID of the player who owns this shop.
     * @param location The Location of the chest block.
     * @param soldItem The ItemStack representing the item being sold (amount should be 1).
     * @param price The price for one transaction (selling 'quantity' of items).
     * @param quantity The number of items sold per transaction.
     * @param displayEnabled Initial state of the floating item display.
     */
    public ChestShop(UUID id, UUID ownerUUID, Location location, ItemStack soldItem, double price, int quantity, boolean displayEnabled) {
        this.id = id;
        this.ownerUUID = ownerUUID;
        this.location = location;
        this.soldItem = soldItem;
        this.price = price;
        this.quantity = quantity;
        this.displayEnabled = displayEnabled;
        this.currentStock = 0; // Initialized to 0, will be updated by ShopManager on load/creation
    }

    // --- Getters ---
    public UUID getId() {
        return id;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    /**
     * Gets the online Player object of the shop owner.
     * @return The Player object if online, otherwise null.
     */
    public Player getOwnerPlayer() {
        return Bukkit.getPlayer(ownerUUID);
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getSoldItem() {
        return soldItem;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public boolean isDisplayEnabled() {
        return displayEnabled;
    }

    // --- Setters ---
    /**
     * Sets the item being sold in the shop.
     * Calling this will trigger an update to the floating item display.
     * @param soldItem The new ItemStack to be sold.
     */
    public void setSoldItem(ItemStack soldItem) {
        this.soldItem = soldItem;
        // Trigger display update when item changes
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getFloatingItemDisplayManager() != null) {
            VortexChestShop.getInstance().getFloatingItemDisplayManager().updateDisplay(this);
        }
    }

    /**
     * Sets the price of the item per transaction.
     * Calling this will trigger an update to the hologram text display.
     * @param price The new price.
     */
    public void setPrice(double price) {
        this.price = price;
        // Trigger hologram update when price changes
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getHologramManager() != null) {
            VortexChestShop.getInstance().getHologramManager().updateHologram(this);
        }
    }

    /**
     * Sets the quantity of items sold per transaction.
     * Calling this will trigger an update to the hologram text display if quantity is part of it.
     * @param quantity The new quantity.
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        // Trigger hologram update if quantity is part of display
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getHologramManager() != null) {
            VortexChestShop.getInstance().getHologramManager().updateHologram(this);
        }
    }

    /**
     * Sets the current stock of the shop.
     * Calling this will trigger updates to both the floating item display (e.g., hide/show if empty)
     * and the hologram display (if stock is shown).
     * @param currentStock The new current stock.
     */
    public void setCurrentStock(int currentStock) {
        this.currentStock = currentStock;
        // Trigger display update when stock changes (e.g., hide/show if empty)
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getFloatingItemDisplayManager() != null) {
            VortexChestShop.getInstance().getFloatingItemDisplayManager().updateDisplay(this);
        }
        // Trigger hologram update if stock is displayed in the hologram
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getHologramManager() != null) {
            VortexChestShop.getInstance().getHologramManager().updateHologram(this);
        }
    }

    /**
     * Enables or disables the floating item display for this shop.
     * @param displayEnabled True to enable, false to disable.
     */
    public void setDisplayEnabled(boolean displayEnabled) {
        this.displayEnabled = displayEnabled;
        if (VortexChestShop.getInstance() != null && VortexChestShop.getInstance().getFloatingItemDisplayManager() != null) {
            if (displayEnabled) {
                // If enabling, try to create/show the display
                VortexChestShop.getInstance().getFloatingItemDisplayManager().createDisplay(this);
            } else {
                // If disabling, remove the display
                VortexChestShop.getInstance().getFloatingItemDisplayManager().removeDisplay(this);
            }
        }
    }
}

