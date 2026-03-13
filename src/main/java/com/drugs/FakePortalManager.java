package com.drugs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class FakePortalManager implements Listener {

    private final Plugin plugin;
    private final Map<String, FakePortalData> portals = new HashMap<>();
    private final Map<BlockKey, String> paneLookup = new HashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();

    public FakePortalManager(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortalBlockForm(BlockFormEvent event) {
        if (event.getNewState().getType() != Material.NETHER_PORTAL) {
            return;
        }

        event.setCancelled(true);
        Set<BlockKey> detected = detectConnectedPortalBlocks(event.getBlock());
        if (!detected.isEmpty()) {
            createOrReplacePortal(detected, event.getBlock().getLocation());
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        Set<BlockKey> portalBlocks = new HashSet<>();
        Location seed = null;

        for (var state : event.getBlocks()) {
            if (state.getType() != Material.NETHER_PORTAL) {
                continue;
            }
            Block block = state.getBlock();
            portalBlocks.add(BlockKey.fromBlock(block));
            if (seed == null) {
                seed = block.getLocation();
            }
        }

        if (portalBlocks.isEmpty() || seed == null) {
            return;
        }

        event.setCancelled(true);
        Set<BlockKey> connected = floodFillWithinBounds(BlockKey.fromLocation(seed), portalBlocks, 23);
        createOrReplacePortal(connected, seed);
    }

    @EventHandler
    public void onPaneBreak(BlockBreakEvent event) {
        BlockKey key = BlockKey.fromBlock(event.getBlock());
        String portalId = paneLookup.get(key);
        if (portalId == null) {
            return;
        }

        removePortal(portalId);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        BlockKey key = BlockKey.fromLocation(event.getTo());
        String portalId = paneLookup.get(key);
        if (portalId == null) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID uuid = event.getPlayer().getUniqueId();
        long cooldownMs = Math.max(500L, plugin.getConfig().getLong("fake-portals.teleport-cooldown-ms", 1500L));
        if (teleportCooldowns.containsKey(uuid) && now - teleportCooldowns.get(uuid) < cooldownMs) {
            return;
        }

        teleportCooldowns.put(uuid, now);
        handlePortalTeleport(event.getPlayer());
    }

    public void loadPersistedPortals() {
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("fake-portals.saved");
        if (root == null) {
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }

            String worldName = section.getString("world");
            if (worldName == null) {
                continue;
            }

            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                continue;
            }

            int minX = section.getInt("min-x");
            int maxX = section.getInt("max-x");
            int minY = section.getInt("min-y");
            int maxY = section.getInt("max-y");
            int minZ = section.getInt("min-z");
            int maxZ = section.getInt("max-z");

            Set<BlockKey> panes = buildPaneSet(worldName, minX, maxX, minY, maxY, minZ, maxZ);
            if (panes.isEmpty()) {
                continue;
            }

            FakePortalData data = new FakePortalData(id, worldName, minX, maxX, minY, maxY, minZ, maxZ, panes);
            portals.put(id, data);
            for (BlockKey pane : panes) {
                paneLookup.put(pane, id);
                Block paneBlock = world.getBlockAt(pane.x(), pane.y(), pane.z());
                paneBlock.setType(Material.PURPLE_STAINED_GLASS_PANE, false);
            }
            startParticleTask(data);
        }
    }

    public void shutdown() {
        for (String portalId : portals.keySet().toArray(new String[0])) {
            stopParticleTask(portalId);
        }
        savePersistedPortals();
        portals.clear();
        paneLookup.clear();
        teleportCooldowns.clear();
    }

    private Set<BlockKey> detectConnectedPortalBlocks(Block start) {
        Set<BlockKey> found = new HashSet<>();
        Queue<Block> queue = new ArrayDeque<>();
        queue.add(start);

        int minX = start.getX() - 23;
        int maxX = start.getX() + 23;
        int minY = start.getY() - 23;
        int maxY = start.getY() + 23;
        int minZ = start.getZ() - 23;
        int maxZ = start.getZ() + 23;

        while (!queue.isEmpty() && found.size() < (23 * 23)) {
            Block current = queue.poll();
            if (current.getType() != Material.NETHER_PORTAL) {
                continue;
            }

            BlockKey key = BlockKey.fromBlock(current);
            if (!found.add(key)) {
                continue;
            }

            for (int[] offset : OFFSETS) {
                int nx = current.getX() + offset[0];
                int ny = current.getY() + offset[1];
                int nz = current.getZ() + offset[2];

                if (nx < minX || nx > maxX || ny < minY || ny > maxY || nz < minZ || nz > maxZ) {
                    continue;
                }

                queue.add(current.getWorld().getBlockAt(nx, ny, nz));
            }
        }

        return found;
    }

    private Set<BlockKey> floodFillWithinBounds(BlockKey start, Set<BlockKey> source, int radius) {
        Set<BlockKey> found = new HashSet<>();
        if (!source.contains(start)) {
            return found;
        }

        Queue<BlockKey> queue = new ArrayDeque<>();
        queue.add(start);

        int minX = start.x() - radius;
        int maxX = start.x() + radius;
        int minY = start.y() - radius;
        int maxY = start.y() + radius;
        int minZ = start.z() - radius;
        int maxZ = start.z() + radius;

        while (!queue.isEmpty() && found.size() < (23 * 23)) {
            BlockKey current = queue.poll();
            if (!source.contains(current) || !found.add(current)) {
                continue;
            }

            for (int[] offset : OFFSETS) {
                BlockKey next = new BlockKey(current.world(), current.x() + offset[0], current.y() + offset[1], current.z() + offset[2]);
                if (next.x() < minX || next.x() > maxX || next.y() < minY || next.y() > maxY || next.z() < minZ || next.z() > maxZ) {
                    continue;
                }
                queue.add(next);
            }
        }

        return found;
    }

    private void createOrReplacePortal(Set<BlockKey> portalBlocks, Location soundLocation) {
        if (portalBlocks.isEmpty()) {
            return;
        }

        String world = portalBlocks.iterator().next().world();
        int minX = portalBlocks.stream().mapToInt(BlockKey::x).min().orElse(0);
        int maxX = portalBlocks.stream().mapToInt(BlockKey::x).max().orElse(0);
        int minY = portalBlocks.stream().mapToInt(BlockKey::y).min().orElse(0);
        int maxY = portalBlocks.stream().mapToInt(BlockKey::y).max().orElse(0);
        int minZ = portalBlocks.stream().mapToInt(BlockKey::z).min().orElse(0);
        int maxZ = portalBlocks.stream().mapToInt(BlockKey::z).max().orElse(0);

        Set<BlockKey> panes = buildPaneSet(world, minX, maxX, minY, maxY, minZ, maxZ);
        if (panes.isEmpty()) {
            return;
        }

        String portalId = world + ":" + minX + ":" + minY + ":" + minZ + ":" + maxX + ":" + maxY + ":" + maxZ;
        if (portals.containsKey(portalId)) {
            removePortal(portalId);
        }

        FakePortalData data = new FakePortalData(portalId, world, minX, maxX, minY, maxY, minZ, maxZ, panes);
        portals.put(portalId, data);

        World bukkitWorld = plugin.getServer().getWorld(world);
        if (bukkitWorld == null) {
            return;
        }

        for (BlockKey pane : panes) {
            Block block = bukkitWorld.getBlockAt(pane.x(), pane.y(), pane.z());
            block.setType(Material.PURPLE_STAINED_GLASS_PANE, false);
            paneLookup.put(pane, portalId);
        }

        bukkitWorld.playSound(soundLocation, Sound.BLOCK_PORTAL_AMBIENT, 0.8f, 1.0f);
        startParticleTask(data);
        savePersistedPortals();
    }

    private Set<BlockKey> buildPaneSet(String world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        Set<BlockKey> panes = new HashSet<>();

        if (minX == maxX) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    panes.add(new BlockKey(world, minX, y, z));
                }
            }
            return panes;
        }

        if (minZ == maxZ) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    panes.add(new BlockKey(world, x, y, minZ));
                }
            }
            return panes;
        }

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    panes.add(new BlockKey(world, x, y, z));
                }
            }
        }

        return panes;
    }

    private void startParticleTask(FakePortalData data) {
        stopParticleTask(data.id());

        long period = Math.max(1L, plugin.getConfig().getLong("fake-portals.particle-period-ticks", 10L));
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            World world = plugin.getServer().getWorld(data.world());
            if (world == null) {
                return;
            }

            for (BlockKey pane : data.panes()) {
                double x = pane.x() + 0.5;
                double y = pane.y() + 0.5;
                double z = pane.z() + 0.5;
                world.spawnParticle(Particle.PORTAL, x, y, z, 3, 0.2, 0.25, 0.2, 0.01);
                world.spawnParticle(Particle.WITCH, x, y, z, 1, 0.15, 0.2, 0.15, 0.0);
            }
        }, 0L, period);

        data.particleTask(task);
    }

    private void stopParticleTask(String portalId) {
        FakePortalData data = portals.get(portalId);
        if (data == null || data.task() == null) {
            return;
        }
        data.task().cancel();
        data.particleTask(null);
    }

    private void removePortal(String portalId) {
        FakePortalData data = portals.get(portalId);
        if (data == null) {
            return;
        }

        stopParticleTask(portalId);
        portals.remove(portalId);

        World world = plugin.getServer().getWorld(data.world());
        if (world != null) {
            for (BlockKey pane : data.panes()) {
                paneLookup.remove(pane);
                Block block = world.getBlockAt(pane.x(), pane.y(), pane.z());
                if (block.getType() == Material.PURPLE_STAINED_GLASS_PANE) {
                    block.setType(Material.AIR, false);
                }
            }
        }

        savePersistedPortals();
    }

    private void handlePortalTeleport(org.bukkit.entity.Player player) {
        World current = player.getWorld();
        World.Environment environment = current.getEnvironment();

        World targetWorld;
        double scale;
        if (environment == World.Environment.NORMAL) {
            targetWorld = plugin.getServer().getWorld(current.getName() + "_nether");
            if (targetWorld == null) {
                targetWorld = plugin.getServer().getWorld("world_nether");
            }
            scale = 0.125;
        } else if (environment == World.Environment.NETHER) {
            String overworldName = current.getName().endsWith("_nether")
                    ? current.getName().substring(0, current.getName().length() - "_nether".length())
                    : "world";
            targetWorld = plugin.getServer().getWorld(overworldName);
            if (targetWorld == null) {
                targetWorld = plugin.getServer().getWorld("world");
            }
            scale = 8.0;
        } else {
            return;
        }

        if (targetWorld == null) {
            return;
        }

        Location from = player.getLocation();
        int targetX = (int) Math.round(from.getX() * scale);
        int targetZ = (int) Math.round(from.getZ() * scale);
        int targetY = targetWorld.getHighestBlockYAt(targetX, targetZ) + 1;

        Location destination = new Location(targetWorld, targetX + 0.5, Math.max(targetWorld.getMinHeight() + 1, targetY), targetZ + 0.5, from.getYaw(), from.getPitch());
        player.teleport(destination);
    }

    private void savePersistedPortals() {
        FileConfiguration config = plugin.getConfig();
        config.set("fake-portals.saved", null);

        for (FakePortalData data : portals.values()) {
            String path = "fake-portals.saved." + data.id();
            config.set(path + ".world", data.world());
            config.set(path + ".min-x", data.minX());
            config.set(path + ".max-x", data.maxX());
            config.set(path + ".min-y", data.minY());
            config.set(path + ".max-y", data.maxY());
            config.set(path + ".min-z", data.minZ());
            config.set(path + ".max-z", data.maxZ());
        }

        plugin.saveConfig();
    }

    private static final int[][] OFFSETS = new int[][]{
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private record BlockKey(String world, int x, int y, int z) {
        private static BlockKey fromBlock(Block block) {
            return new BlockKey(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
        }

        private static BlockKey fromLocation(Location location) {
            return new BlockKey(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        }
    }

    private static class FakePortalData {
        private final String id;
        private final String world;
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int minZ;
        private final int maxZ;
        private final Set<BlockKey> panes;
        private BukkitTask task;

        private FakePortalData(String id, String world, int minX, int maxX, int minY, int maxY, int minZ, int maxZ, Set<BlockKey> panes) {
            this.id = id;
            this.world = world;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.panes = panes;
        }

        private String id() { return id; }
        private String world() { return world; }
        private int minX() { return minX; }
        private int maxX() { return maxX; }
        private int minY() { return minY; }
        private int maxY() { return maxY; }
        private int minZ() { return minZ; }
        private int maxZ() { return maxZ; }
        private Set<BlockKey> panes() { return panes; }
        private BukkitTask task() { return task; }
        private void particleTask(BukkitTask task) { this.task = task; }
    }
}
