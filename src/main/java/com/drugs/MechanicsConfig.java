package com.drugs;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class MechanicsConfig {

    private static int cannabisGrowthSeconds = 600;
    private static int cartDurabilityUses = 16;

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
    }

    public static int getCannabisGrowthSeconds() {
        return cannabisGrowthSeconds;
    }

    public static int getCartDurabilityUses() {
        return cartDurabilityUses;
    }
}
