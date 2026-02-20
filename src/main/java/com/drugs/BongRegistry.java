package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BongRegistry {

    public static class BongData {
        private final String worldId;
        private final int x;
        private final int y;
        private final int z;
        private final float yaw;
        private final List<UUID> displayIds;
        private UUID interactionId;

        public BongData(Location anchor, float yaw, List<UUID> displayIds, UUID interactionId) {
            this.worldId = anchor.getWorld().getUID().toString();
            this.x = anchor.getBlockX();
            this.y = anchor.getBlockY();
            this.z = anchor.getBlockZ();
            this.yaw = yaw;
            this.displayIds = displayIds == null ? new ArrayList<>() : new ArrayList<>(displayIds);
            this.interactionId = interactionId;
        }

        public Location getAnchor() {
            World world = Bukkit.getWorld(UUID.fromString(worldId));
            if (world == null) return null;
            return new Location(world, x, y, z);
        }

        public boolean hasEntity(UUID entityId) {
            if (entityId == null) return false;
            return entityId.equals(interactionId) || displayIds.contains(entityId);
        }
    }

    private static final Map<String, BongData> bongs = new ConcurrentHashMap<>();
    private static File file;

    private BongRegistry() {}

    public static void init(File dataFolder) {
        file = new File(dataFolder, "bongs.yml");
        load();
    }

    public static void put(Location location, BongData data) {
        if (location == null || data == null || location.getWorld() == null) return;
        bongs.put(key(location), data);
    }

    public static BongData get(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return bongs.get(key(location));
    }

    public static BongData findByInteraction(UUID interactionId) {
        if (interactionId == null) return null;
        for (BongData data : bongs.values()) {
            if (interactionId.equals(data.interactionId)) return data;
        }
        return null;
    }

    public static BongData findByEntity(UUID entityId) {
        if (entityId == null) return null;
        for (BongData data : bongs.values()) {
            if (data.hasEntity(entityId)) return data;
        }
        return null;
    }

    public static void remove(Location location) {
        BongData data = get(location);
        if (data != null) {
            for (UUID displayId : data.displayIds) {
                removeEntity(displayId);
            }
            removeEntity(data.interactionId);
            bongs.remove(key(location));
        }
    }

    public static void save() {
        if (file == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<String, BongData> entry : bongs.entrySet()) {
            String base = entry.getKey();
            BongData data = entry.getValue();
            yaml.set(base + ".world", data.worldId);
            yaml.set(base + ".x", data.x);
            yaml.set(base + ".y", data.y);
            yaml.set(base + ".z", data.z);
            yaml.set(base + ".yaw", data.yaw);
            yaml.set(base + ".displays", data.displayIds.stream().map(UUID::toString).toList());
            yaml.set(base + ".interaction", data.interactionId == null ? null : data.interactionId.toString());
        }
        try {
            yaml.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[DrugsV2] Failed to save bong registry: " + e.getMessage());
        }
    }

    public static void load() {
        bongs.clear();
        if (file == null || !file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String node : yaml.getKeys(false)) {
            String worldId = yaml.getString(node + ".world");
            if (worldId == null) continue;

            World world = Bukkit.getWorld(UUID.fromString(worldId));
            if (world == null) continue;

            int x = yaml.getInt(node + ".x");
            int y = yaml.getInt(node + ".y");
            int z = yaml.getInt(node + ".z");
            float yaw = (float) yaml.getDouble(node + ".yaw", 0.0);

            List<UUID> displays = new ArrayList<>();
            for (String display : yaml.getStringList(node + ".displays")) {
                UUID parsed = parseUuid(display);
                if (parsed != null) {
                    displays.add(parsed);
                }
            }
            if (displays.isEmpty()) {
                UUID legacyDisplay = parseUuid(yaml.getString(node + ".display"));
                if (legacyDisplay != null) {
                    displays.add(legacyDisplay);
                }
            }

            UUID interaction = parseUuid(yaml.getString(node + ".interaction"));

            Location location = new Location(world, x, y, z);
            bongs.put(key(location), new BongData(location, yaw, displays, interaction));
        }
    }

    public static void respawnMissing(BongListener listener) {
        Map<String, BongData> snapshot = new HashMap<>(bongs);
        for (Map.Entry<String, BongData> entry : snapshot.entrySet()) {
            BongData data = entry.getValue();
            Location anchor = data.getAnchor();
            if (anchor == null) {
                bongs.remove(entry.getKey());
                continue;
            }

            if (!anchor.getChunk().isLoaded()) {
                anchor.getChunk().load();
            }

            Entity existingInteraction = getEntity(data.interactionId);
            boolean allDisplaysPresent = !data.displayIds.isEmpty();
            for (UUID displayId : data.displayIds) {
                if (getEntity(displayId) == null) {
                    allDisplaysPresent = false;
                    break;
                }
            }

            if (existingInteraction != null && allDisplaysPresent) {
                continue;
            }

            listener.removeOrphanEntitiesAtAnchor(anchor);
            listener.spawnOrReplace(anchor, data.yaw);
        }
    }

    private static Entity getEntity(UUID id) {
        if (id == null) return null;
        return Bukkit.getEntity(id);
    }

    private static void removeEntity(UUID id) {
        Entity entity = getEntity(id);
        if (entity != null) entity.remove();
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }

    private static String key(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }
}
