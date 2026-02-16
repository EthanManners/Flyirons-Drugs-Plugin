package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CannabisPlantRegistry {
    private static final Map<String, String> plants = new ConcurrentHashMap<>();
    private static File file;

    public static void init(File dataFolder) {
        file = new File(dataFolder, "cannabis_plants.yml");
        load();
    }

    private static String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public static void setPlant(Location location, String strainId) {
        if (location == null || location.getWorld() == null || strainId == null) return;
        plants.put(key(location), strainId.toLowerCase());
    }

    public static String getPlant(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return plants.get(key(location));
    }

    public static void removePlant(Location location) {
        if (location == null || location.getWorld() == null) return;
        plants.remove(key(location));
    }

    public static void save() {
        if (file == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, String> entry : plants.entrySet()) {
            yaml.set(entry.getKey(), entry.getValue());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[DrugsV2] Failed to save cannabis plant registry: " + e.getMessage());
        }
    }

    public static void load() {
        plants.clear();
        if (file == null || !file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false)) {
            String value = yaml.getString(key);
            if (value != null) {
                plants.put(key, value.toLowerCase());
            }
        }
    }

    public static void cleanupInvalidPlants() {
        Map<String, String> snapshot = new HashMap<>(plants);
        for (String key : snapshot.keySet()) {
            String[] parts = key.split(":");
            if (parts.length != 4) {
                plants.remove(key);
                continue;
            }

            try {
                World world = Bukkit.getWorld(UUID.fromString(parts[0]));
                if (world == null) {
                    plants.remove(key);
                    continue;
                }

                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                if (world.getBlockAt(x, y, z).getType().isAir()) {
                    plants.remove(key);
                }
            } catch (Exception ignored) {
                plants.remove(key);
            }
        }
    }
}
