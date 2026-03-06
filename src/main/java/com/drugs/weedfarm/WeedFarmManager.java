package com.drugs.weedfarm;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WeedFarmManager {
    private final Map<String, WeedFarm> farmsById = new ConcurrentHashMap<>();
    private final Map<String, String> farmByController = new ConcurrentHashMap<>();
    private final Map<UUID, String> farmByVillager = new ConcurrentHashMap<>();
    private final File file;

    public WeedFarmManager(File dataFolder) {
        this.file = new File(dataFolder, "weed_farms.yml");
    }

    public WeedFarm createFarm(Location controller) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        WeedFarm farm = new WeedFarm(id, controller);
        farmsById.put(id, farm);
        farmByController.put(locKey(controller), id);
        return farm;
    }

    public WeedFarm getByController(Location location) {
        String id = farmByController.get(locKey(location));
        return id == null ? null : farmsById.get(id);
    }

    public Collection<WeedFarm> getFarms() {
        return farmsById.values();
    }

    public WeedFarm getByVillager(UUID uuid) {
        String farmId = farmByVillager.get(uuid);
        return farmId == null ? null : farmsById.get(farmId);
    }

    public boolean assignVillager(WeedFarm farm, UUID villagerId) {
        if (farm.getAssignedVillagers().size() >= WeedFarm.MAX_WORKERS && !farm.getAssignedVillagers().contains(villagerId)) {
            return false;
        }

        WeedFarm existing = getByVillager(villagerId);
        if (existing != null && existing != farm) {
            existing.getAssignedVillagers().remove(villagerId);
        }
        farmByVillager.put(villagerId, farm.getFarmId());
        return farm.getAssignedVillagers().add(villagerId);
    }

    public void removeFarm(WeedFarm farm) {
        farmsById.remove(farm.getFarmId());
        farmByController.remove(locKey(farm.getControllerLocation()));
        for (UUID villagerId : farm.getAssignedVillagers()) {
            farmByVillager.remove(villagerId);
        }
        farm.getAssignedVillagers().clear();
    }

    public WeedFarm getByFarmId(String farmId) {
        return farmsById.get(farmId);
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (WeedFarm farm : farmsById.values()) {
            String base = "farms." + farm.getFarmId();
            yaml.set(base + ".controller", serializeLocation(farm.getControllerLocation()));
            yaml.set(base + ".world", farm.getWorldId() == null ? null : farm.getWorldId().toString());
            yaml.set(base + ".enabled", farm.isEnabled());
            yaml.set(base + ".region.minX", farm.getMinX());
            yaml.set(base + ".region.minY", farm.getMinY());
            yaml.set(base + ".region.minZ", farm.getMinZ());
            yaml.set(base + ".region.maxX", farm.getMaxX());
            yaml.set(base + ".region.maxY", farm.getMaxY());
            yaml.set(base + ".region.maxZ", farm.getMaxZ());
            List<String> villagers = new ArrayList<>();
            for (UUID id : farm.getAssignedVillagers()) {
                villagers.add(id.toString());
            }
            yaml.set(base + ".villagers", villagers);
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[DrugsV2] Failed to save weed farms: " + e.getMessage());
        }
    }

    public void load() {
        farmsById.clear();
        farmByController.clear();
        farmByVillager.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("farms");
        if (section == null) {
            return;
        }

        for (String farmId : section.getKeys(false)) {
            String base = "farms." + farmId;
            Location controller = deserializeLocation(yaml.getString(base + ".controller"));
            if (controller == null) {
                continue;
            }

            WeedFarm farm = new WeedFarm(farmId, controller);
            farm.setEnabled(yaml.getBoolean(base + ".enabled", true));
            String worldRaw = yaml.getString(base + ".world");
            World world = worldRaw != null ? Bukkit.getWorld(UUID.fromString(worldRaw)) : controller.getWorld();
            if (world != null) {
                Location first = new Location(world,
                        yaml.getInt(base + ".region.minX", controller.getBlockX()),
                        yaml.getInt(base + ".region.minY", controller.getBlockY()),
                        yaml.getInt(base + ".region.minZ", controller.getBlockZ()));
                Location second = new Location(world,
                        yaml.getInt(base + ".region.maxX", controller.getBlockX()),
                        yaml.getInt(base + ".region.maxY", controller.getBlockY()),
                        yaml.getInt(base + ".region.maxZ", controller.getBlockZ()));
                farm.setRegion(world, first, second);
            }

            for (String villagerId : yaml.getStringList(base + ".villagers")) {
                try {
                    UUID uuid = UUID.fromString(villagerId);
                    if (farm.getAssignedVillagers().size() >= WeedFarm.MAX_WORKERS) {
                        continue;
                    }
                    farm.getAssignedVillagers().add(uuid);
                    farmByVillager.put(uuid, farmId);
                } catch (IllegalArgumentException ignored) {
                }
            }

            farmsById.put(farmId, farm);
            farmByController.put(locKey(controller), farmId);
        }
    }

    private String locKey(Location location) {
        return serializeLocation(location);
    }

    private static String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static Location deserializeLocation(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] split = raw.split(":");
        if (split.length != 4) return null;
        try {
            World world = Bukkit.getWorld(UUID.fromString(split[0]));
            if (world == null) return null;
            return new Location(world,
                    Integer.parseInt(split[1]),
                    Integer.parseInt(split[2]),
                    Integer.parseInt(split[3]));
        } catch (Exception ex) {
            return null;
        }
    }
}
