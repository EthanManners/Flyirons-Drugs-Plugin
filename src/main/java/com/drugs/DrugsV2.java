package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * Main plugin class for DrugsV2.
 */
public class DrugsV2 extends JavaPlugin {

    private static DrugsV2 instance;
    private FileConfiguration recipesConfig;
    private File recipesFile;
    private BongListener bongListener;

    public static DrugsV2 getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        // Load default configs
        saveDefaultConfig();
        saveRecipesConfig();
        saveToleranceConfig();
        saveAchievementSettingsConfig();
        saveAchievementsConfig();
        saveOverdoseConfig();
        saveAddictionConfig();
        saveStrainsConfig();
        saveBongConfig();

        // Initialize performance optimizer first
        PerformanceOptimizer.initialize();
        
        // Load tolerance settings
        ToleranceConfigLoader.load(getDataFolder());
        
        // Load achievement settings and achievements
        AchievementSettingsLoader.load(getDataFolder());
        CustomAchievementLoader.load(getDataFolder());
        
        // Load overdose effect settings
        OverdoseEffectManager.load(getDataFolder());

        // Load addiction settings and cure definitions
        AddictionConfigLoader.load(this);
        StrainConfigLoader.load(getDataFolder());
        BongConfigLoader.load(getDataFolder());
        CannabisPlantRegistry.init(getDataFolder());
        BongRegistry.init(getDataFolder());

        // Initialize core drug system
        DrugRegistry.init(this);
        CureRegistry.init(this);
        registerBongRecipe();

        AddictionManager.init(this);

        // Start tolerance decay
        ToleranceTracker.startDecayTask();

        // Register events
        getServer().getPluginManager().registerEvents(new DrugMenuListener(), this);
        getServer().getPluginManager().registerEvents(new DrugUseListener(), this);
        getServer().getPluginManager().registerEvents(new AddictionListener(), this);
        getServer().getPluginManager().registerEvents(new CannabisPlantListener(), this);
        getServer().getPluginManager().registerEvents(new StrainCraftingListener(), this);
        getServer().getPluginManager().registerEvents(new StrainsMenuListener(), this);
        bongListener = new BongListener();
        getServer().getPluginManager().registerEvents(bongListener, this);
        Bukkit.getPluginManager().registerEvents(new AchievementsGUI(), DrugsV2.getInstance());

        BongRegistry.respawnMissing(bongListener);

        // Register commands
        getCommand("drugs").setExecutor(new DrugsCommand());
        getCommand("drugs").setTabCompleter(new DrugsTabCompleter());
        getCommand("tolerance").setExecutor(new ToleranceCommand());
        getCommand("strains").setExecutor(new StrainsCommand());

        // Register PlaceholderAPI expansion if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DrugsPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
        }

        getLogger().info("DrugsV2 enabled!");
    }

    @Override
    public void onDisable() {
        CannabisPlantRegistry.save();
        BongRegistry.save();
        AddictionManager.shutdown();
        getLogger().info("DrugsV2 disabled.");
    }

    public void saveRecipesConfig() {
        if (recipesFile == null) {
            recipesFile = new File(getDataFolder(), "recipes.yml");
        }
        if (!recipesFile.exists()) {
            saveResource("recipes.yml", false);
        }
        recipesConfig = YamlConfiguration.loadConfiguration(recipesFile);
    }

    public void saveToleranceConfig() {
        File file = new File(getDataFolder(), "tolerance.yml");
        if (!file.exists()) {
            saveResource("tolerance.yml", false);
        }
    }
    
    public void saveAchievementSettingsConfig() {
        File file = new File(getDataFolder(), "achievement_settings.yml");
        if (!file.exists()) {
            saveResource("achievement_settings.yml", false);
        }
    }
    
    public void saveAchievementsConfig() {
        File file = new File(getDataFolder(), "achievements.yml");
        if (!file.exists()) {
            saveResource("achievements.yml", false);
        }
    }

    /**
     * Saves the overdose config file
     */
    public void saveOverdoseConfig() {
        File file = new File(getDataFolder(), "overdose.yml");
        if (!file.exists()) {
            saveResource("overdose.yml", false);
        }
    }

    public void saveAddictionConfig() {
        File file = new File(getDataFolder(), "addiction.yml");
        if (!file.exists()) {
            saveResource("addiction.yml", false);
        }
    }

    public void saveStrainsConfig() {
        File file = new File(getDataFolder(), "strains.yml");
        if (!file.exists()) {
            saveResource("strains.yml", false);
        }
    }

    public void saveBongConfig() {
        File file = new File(getDataFolder(), "bong.yml");
        if (!file.exists()) {
            saveResource("bong.yml", false);
        }
    }

    private void registerBongRecipe() {
        if (recipesConfig == null) return;

        var recipeSection = recipesConfig.getConfigurationSection("bong");
        if (recipeSection == null) return;

        DrugRecipeHelper.registerItemRecipe("bong", recipeSection, BongItemFactory.createBongItem(1), this);
    }

    public FileConfiguration getRecipesConfig() {
        return recipesConfig;
    }

    public BongListener getBongListener() {
        return bongListener;
    }
}
