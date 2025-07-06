// src/main/java/com/vortex/vortexchestshop/managers/EconomyManager.java

package com.vortex.vortexchestshop.managers;

import com.vortex.vortexchestshop.VortexChestShop;
import com.vortex.vortexchestshop.utils.Logger; // Added for logging
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse; // Added for more detailed transaction results
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player; // Added for Player specific permission checks

public class EconomyManager {

    private final VortexChestShop plugin;
    private final Economy economy;

    public EconomyManager(VortexChestShop plugin) {
        this.plugin = plugin;
        this.economy = plugin.getEconomy(); // Get the Vault Economy instance from the main class
        if (this.economy == null) {
            Logger.severe("EconomyManager initialized without a valid Vault Economy provider. Economy features will be disabled.");
        }
    }

    /**
     * Checks if a player has a sufficient amount of money.
     * @param player The player to check.
     * @param amount The amount required.
     * @return true if the player has enough money, false otherwise.
     */
    public boolean has(OfflinePlayer player, double amount) {
        if (economy == null) {
            Logger.warning("Economy provider is null. Cannot check balance for " + player.getName());
            return false;
        }
        return economy.has(player, amount);
    }

    /**
     * Deposits money into a player's account.
     * @param player The player to deposit to.
     * @param amount The amount to deposit.
     * @return true if the deposit was successful, false otherwise.
     */
    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null) {
            Logger.warning("Economy provider is null. Cannot deposit money to " + player.getName());
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            Logger.warning("Failed to deposit $" + amount + " to " + player.getName() + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Withdraws money from a player's account.
     * @param player The player to withdraw from.
     * @param amount The amount to withdraw.
     * @return true if the withdrawal was successful, false otherwise.
     */
    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null) {
            Logger.warning("Economy provider is null. Cannot withdraw money from " + player.getName());
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            Logger.warning("Failed to withdraw $" + amount + " from " + player.getName() + ": " + response.errorMessage);
        }
        return response.transactionSuccess();
    }

    /**
     * Gets the balance of a player.
     * @param player The player to get the balance of.
     * @return The player's balance. Returns 0.0 if economy provider is not available.
     */
    public double getBalance(OfflinePlayer player) {
        if (economy == null) {
            Logger.warning("Economy provider is null. Cannot get balance for " + player.getName());
            return 0.0;
        }
        return economy.getBalance(player);
    }

    /**
     * Calculates the transaction tax for a given amount based on player's premium status.
     * @param player The player (seller) involved in the transaction. This should be the shop owner.
     * @param amount The total amount of the transaction.
     * @return The calculated tax amount.
     */
    public double calculateTax(OfflinePlayer player, double amount) {
        double taxRate;
        // Check if the player is online and has the premium tax reduction permission
        // Only check for online players as OfflinePlayer does not have hasPermission method directly
        if (player.isOnline()) {
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer != null && onlinePlayer.hasPermission("vortexchestshop.premium.tax.reduced")) {
                taxRate = plugin.getConfig().getDouble("shop-settings.premium-transaction-tax", 0.02);
            } else {
                taxRate = plugin.getConfig().getDouble("shop-settings.f2p-transaction-tax", 0.05);
            }
        } else {
            // If player is offline, assume F2P tax rate or a default if premium status can't be determined
            // A more advanced system might store premium status persistently for offline players.
            taxRate = plugin.getConfig().getDouble("shop-settings.f2p-transaction-tax", 0.05);
            Logger.debug("Player " + player.getName() + " is offline. Applying F2P tax rate for transaction.");
        }
        return amount * taxRate;
    }
}
