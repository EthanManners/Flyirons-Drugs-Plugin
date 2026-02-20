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


    public static Map<Location, String> getPlantsSnapshot() {
        Map<Location, String> snapshot = new HashMap<>();
        for (Map.Entry<String, String> entry : plants.entrySet()) {
            Location location = parseKey(entry.getKey());
            if (location != null) {
                snapshot.put(location, entry.getValue());
            }
        }
        return snapshot;
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


    private static Location parseKey(String key) {
        String[] parts = key.split(":");
        if (parts.length != 4) {
            return null;
        }

        try {
            World world = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (world == null) {
                return null;
            }

            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void cleanupInvalidPlants() {
        Map<String, String> snapshot = new HashMap<>(plants);
        for (String key : snapshot.keySet()) {
            Location location = parseKey(key);
            if (location == null || location.getBlock().getType().isAir()) {
                plants.remove(key);
            }
        }
    }
}
