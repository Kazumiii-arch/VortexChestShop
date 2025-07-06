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
import org.bukkit.plugin.Plugin; // Added for general plugin checks
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects; // Added for Objects.requireNonNull

public final class VortexChestShop extends JavaPlugin {

    // Singleton instance of the plugin
    private static VortexChestShop instance;

    // Managers for various plugin functionalities
    private EconomyManager economyManager;
    private ShopManager shopManager;
    private FloatingItemDisplayManager floatingItemDisplayManager;
    private HologramManager hologramManager;

    // Vault Economy instance, will be set up during onEnable
    private Economy economy = null;

    @Override
    public void onEnable() {
        // Set the singleton instance immediately
        instance = this;
        Logger.info("VortexChestShop is enabling...");

        // Save default config if it doesn't exist. This also loads the config into memory.
        saveDefaultConfig();

        // Initialize managers. Order matters for some (e.g., EconomyManager first).
        initializeManagers();

        // Setup Vault economy. If it fails, disable the plugin as economy is core.
        if (!setupEconomy()) {
            Logger.severe("Vault not found or no economy plugin hooked! Disabling VortexChestShop.");
            getServer().getPluginManager().disablePlugin(this);
            return; // Stop further initialization
        }

        // Check for other soft dependencies and log their status
        checkSoftDependencies();

        // Register all event listeners
        registerListeners();

        // Register all commands and set their executors/tab completers
        registerCommands();

        // Load existing shops from configuration (or database)
        shopManager.loadShops();
        // Start the tasks for floating item displays (rotation, particles, stock checks)
        floatingItemDisplayManager.startDisplayTasks();

        Logger.info("VortexChestShop has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        Logger.info("VortexChestShop is disabling...");

        // Stop all floating item display tasks and clean up spawned entities/holograms
        if (floatingItemDisplayManager != null) {
            floatingItemDisplayManager.stopDisplayTasks();
            floatingItemDisplayManager.cleanupAllDisplays();
        }

        // Save any pending shop data.
        // While individual shop changes are saved immediately, this acts as a final safeguard.
        if (shopManager != null) {
            shopManager.saveShops();
        }

        Logger.info("VortexChestShop has been disabled.");
    }

    /**
     * Gets the singleton instance of the plugin.
     * This allows other classes to easily access plugin managers and methods.
     * @return The plugin instance.
     */
    public static VortexChestShop getInstance() {
        return instance;
    }

    /**
     * Initializes all custom managers used by the plugin.
     * The order of initialization might be important if managers depend on each other.
     */
    private void initializeManagers() {
        this.economyManager = new EconomyManager(this);
        this.shopManager = new ShopManager(this);
        this.floatingItemDisplayManager = new FloatingItemDisplayManager(this);
        this.hologramManager = new HologramManager(this);
        Logger.info("All plugin managers initialized.");
    }

    /**
     * Sets up the Vault economy integration.
     * @return true if economy setup was successful and an economy provider was found, false otherwise.
     */
    private boolean setupEconomy() {
        // Check if Vault plugin is present
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        // Attempt to get the economy service provider from Vault
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        // Get the economy provider
        economy = rsp.getProvider();
        return economy != null; // Return true if an economy provider was successfully obtained
    }

    /**
     * Checks for the presence of soft dependencies and logs their status.
     * This helps server owners identify missing plugins for full functionality.
     */
    private void checkSoftDependencies() {
        Plugin placeholderAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (placeholderAPI == null || !placeholderAPI.isEnabled()) {
            Logger.warning("PlaceholderAPI not found or not enabled! Some dynamic text features will be unavailable.");
        }

        Plugin protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        if (protocolLib == null || !protocolLib.isEnabled()) {
            Logger.warning("ProtocolLib not found or not enabled! Advanced floating item features (glowing, long-range) will be limited.");
        }

        Plugin holographicDisplays = Bukkit.getPluginManager().getPlugin("HolographicDisplays");
        if (holographicDisplays == null || !holographicDisplays.isEnabled()) {
            Logger.warning("HolographicDisplays not found or not enabled! Hologram text will not be displayed.");
        }

        Plugin itemsAdder = Bukkit.getPluginManager().getPlugin("ItemsAdder");
        if (itemsAdder == null || !itemsAdder.isEnabled()) {
            Logger.info("ItemsAdder not found or not enabled. Custom ItemsAdder items will not have special handling.");
        }

        Plugin oraxen = Bukkit.getPluginManager().getPlugin("Oraxen");
        if (oraxen == null || !oraxen.isEnabled()) {
            Logger.info("Oraxen not found or not enabled. Custom Oraxen items will not have special handling.");
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
     * Registers all commands for the plugin and sets their executors and tab completers.
     */
    private void registerCommands() {
        ShopCommand shopCommandExecutor = new ShopCommand(this);
        // Register the main command and its aliases
        Objects.requireNonNull(getCommand("vortexchestshop")).setExecutor(shopCommandExecutor);
        Objects.requireNonNull(getCommand("vortexchestshop")).setTabCompleter(shopCommandExecutor);
        Objects.requireNonNull(getCommand("shop")).setExecutor(shopCommandExecutor);
        Objects.requireNonNull(getCommand("shop")).setTabCompleter(shopCommandExecutor);
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
