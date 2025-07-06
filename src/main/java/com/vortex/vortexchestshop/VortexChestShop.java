// src/main/java/com/vortex/vortexchestshop/VortexChestShop.java

package com.vortex.vortexchestshop;

import com.vortex.vortexchestshop.commands.ShopCommand;
import com.vortex.vortexchestshop.listeners.PlayerListener;
import com.vortex.vortexchestshop.listeners.ShopListener;
import com.vortex.vortexchestshop.managers.EconomyManager;
import com.vortex.vortexchestshop.managers.FloatingItemDisplayManager;
import com.vortex.vortexchestshop.managers.HologramManager;
import com.vortex.vortexchestshop.managers.ShopManager;
import com.vortex.vortexchestshop.utils.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class VortexChestShop extends JavaPlugin {

    // Singleton instance of the plugin
    private static VortexChestShop instance;

    // Managers for various plugin functionalities
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private FloatingItemDisplayManager floatingItemDisplayManager;
    private HologramManager hologramManager;

    // Vault Economy instance
    private Economy economy = null;

    @Override
    public void onEnable() {
        // Set the singleton instance
        instance = this;
        Logger.info("VortexChestShop is enabling...");

        // Save default config if it doesn't exist
        saveDefaultConfig();

        // Initialize managers
        // Order matters for some managers (e.g., EconomyManager first)
        initializeManagers();

        // Setup Vault economy
        if (!setupEconomy()) {
            Logger.severe("Vault not found or no economy plugin hooked! Disabling VortexChestShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Check for other soft dependencies
        checkSoftDependencies();

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Load existing shops and start their displays
        shopManager.loadShops();
        floatingItemDisplayManager.startDisplayTasks();

        Logger.info("VortexChestShop has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        Logger.info("VortexChestShop is disabling...");

        // Stop all floating item displays and clean up
        if (floatingItemDisplayManager != null) {
            floatingItemDisplayManager.stopDisplayTasks();
            floatingItemDisplayManager.cleanupAllDisplays();
        }

        // Save any pending shop data (though real-time saving is better)
        if (shopManager != null) {
            shopManager.saveShops();
        }

        Logger.info("VortexChestShop has been disabled.");
    }

    /**
     * Gets the singleton instance of the plugin.
     * @return The plugin instance.
     */
    public static VortexChestShop getInstance() {
        return instance;
    }

    /**
     * Initializes all custom managers used by the plugin.
     */
    private void initializeManagers() {
        this.economyManager = new EconomyManager(this);
        this.shopManager = new ShopManager(this);
        this.floatingItemDisplayManager = new FloatingItemDisplayManager(this);
        this.hologramManager = new HologramManager(this);
    }

    /**
     * Sets up the Vault economy integration.
     * @return true if economy setup was successful, false otherwise.
     */
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    /**
     * Checks for the presence of soft dependencies and logs their status.
     */
    private void checkSoftDependencies() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            Logger.warning("PlaceholderAPI not found! Some dynamic text features will be unavailable.");
        }
        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            Logger.warning("ProtocolLib not found! Advanced floating item features (glowing, long-range) will be limited.");
        }
        if (Bukkit.getPluginManager().getPlugin("HolographicDisplays") == null) {
            Logger.warning("HolographicDisplays not found! Hologram text will not be displayed.");
        }
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") == null) {
            Logger.info("ItemsAdder not found. Custom ItemsAdder items will not have special handling.");
        }
        if (Bukkit.getPluginManager().getPlugin("Oraxen") == null) {
            Logger.info("Oraxen not found. Custom Oraxen items will not have special handling.");
        }
    }

    /**
     * Registers all event listeners for the plugin.
     */
    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);
        Logger.info("Registered event listeners.");
    }

    /**
     * Registers all commands for the plugin.
     */
    private void registerCommands() {
        // Register the main command
        Objects.requireNonNull(getCommand("vortexchestshop")).setExecutor(new ShopCommand(this));
        Objects.requireNonNull(getCommand("shop")).setExecutor(new ShopCommand(this));
        Logger.info("Registered commands.");
    }

    // --- Getters for Managers and Dependencies ---

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public FloatingItemDisplayManager getFloatingItemDisplayManager() {
        return floatingItemDisplayManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public Economy getEconomy() {
        return economy;
    }
          }
