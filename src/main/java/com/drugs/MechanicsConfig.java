package com.drugs;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class MechanicsConfig {

    private static int cannabisGrowthSeconds = 600;
    private static int cartDurabilityUses = 16;
    private static int bongDurabilityUses = 24;
    private static int workerTickInterval = 15;
    private static int maxHarvestPerCycle = 8;
    private static int maxPlantPerCycle = 8;

    private MechanicsConfig() {
    }

    public static void load(File dataFolder) {
        File file = new File(dataFolder, "mechanics.yml");
        if (!file.exists()) {
            DrugsV2.getInstance().saveResource("mechanics.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        cannabisGrowthSeconds = Math.max(1, yaml.getInt("cannabis.growth-seconds", 600));
        cartDurabilityUses = Math.max(1, yaml.getInt("cart.durability-uses", 16));
        bongDurabilityUses = Math.max(1, yaml.getInt("bong.durability-uses", 24));
        workerTickInterval = Math.max(10, yaml.getInt("weed-farm.worker-tick-interval", 15));
        maxHarvestPerCycle = Math.max(1, yaml.getInt("weed-farm.max-harvest-per-cycle", 8));
        maxPlantPerCycle = Math.max(1, yaml.getInt("weed-farm.max-plant-per-cycle", 8));
    }

    public static int getCannabisGrowthSeconds() {
        return cannabisGrowthSeconds;
    }

    public static int getCartDurabilityUses() {
        return cartDurabilityUses;
    }

    public static int getBongDurabilityUses() {
        return bongDurabilityUses;
    }

    public static int getWorkerTickInterval() {
        return workerTickInterval;
    }

    public static int getMaxHarvestPerCycle() {
        return maxHarvestPerCycle;
    }

    public static int getMaxPlantPerCycle() {
        return maxPlantPerCycle;
    }
}
