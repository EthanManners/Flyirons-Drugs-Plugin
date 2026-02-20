package com.drugs;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public final class BongConfigLoader {

    private static boolean enabled = true;
    private static String baseDrugId = "blunt";
    private static long cooldownMillis = 1500L;

    private static Material bongItemMaterial = Material.GLASS_BOTTLE;
    private static String bongDisplayName = "&bGlass Bong";
    private static int bongCustomModelData = 0;

    private BongConfigLoader() {
    }

    public static void load(File dataFolder) {
        File file = new File(dataFolder, "bong.yml");
        if (!file.exists()) {
            DrugsV2.getInstance().saveResource("bong.yml", false);
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        enabled = yaml.getBoolean("enabled", true);
        baseDrugId = yaml.getString("base-drug-id", "blunt").toLowerCase();
        cooldownMillis = Math.max(0L, yaml.getLong("cooldown-ms", 1500L));

        bongItemMaterial = Material.matchMaterial(yaml.getString("item.material", "GLASS_BOTTLE"));
        if (bongItemMaterial == null) {
            bongItemMaterial = Material.GLASS_BOTTLE;
        }
        bongDisplayName = yaml.getString("item.display-name", "&bGlass Bong");
        bongCustomModelData = Math.max(0, yaml.getInt("item.custom-model-data", 0));
    }

    public static boolean isEnabled() { return enabled; }
    public static String getBaseDrugId() { return baseDrugId; }
    public static long getCooldownMillis() { return cooldownMillis; }
    public static Material getBongItemMaterial() { return bongItemMaterial; }
    public static String getBongDisplayName() { return bongDisplayName; }
    public static int getBongCustomModelData() { return bongCustomModelData; }
}
