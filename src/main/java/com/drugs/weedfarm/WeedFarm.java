package com.drugs.weedfarm;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WeedFarm {
    public static final int MAX_AREA_SIDE = 16;
    public static final int MAX_WORKERS = 5;

    private final String farmId;
    private final Location controllerLocation;
    private UUID worldId;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;
    private final Set<UUID> assignedVillagers = new HashSet<>();
    private boolean enabled = true;
    private int scanCursor = 0;

    public WeedFarm(String farmId, Location controllerLocation) {
        this.farmId = farmId;
        this.controllerLocation = controllerLocation;
        if (controllerLocation.getWorld() != null) {
            this.worldId = controllerLocation.getWorld().getUID();
        }
    }

    public String getFarmId() { return farmId; }
    public UUID getWorldId() { return worldId; }
    public Location getControllerLocation() { return controllerLocation; }
    public Set<UUID> getAssignedVillagers() { return assignedVillagers; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getScanCursor() { return scanCursor; }
    public void setScanCursor(int scanCursor) { this.scanCursor = scanCursor; }

    public boolean canApplyRegion(Location first, Location second) {
        int xSize = Math.abs(first.getBlockX() - second.getBlockX()) + 1;
        int zSize = Math.abs(first.getBlockZ() - second.getBlockZ()) + 1;
        return xSize <= MAX_AREA_SIDE && zSize <= MAX_AREA_SIDE;
    }

    public void setRegion(World world, Location first, Location second) {
        this.worldId = world.getUID();
        this.minX = Math.min(first.getBlockX(), second.getBlockX());
        this.minY = Math.min(first.getBlockY(), second.getBlockY());
        this.minZ = Math.min(first.getBlockZ(), second.getBlockZ());
        this.maxX = Math.max(first.getBlockX(), second.getBlockX());
        this.maxY = Math.max(first.getBlockY(), second.getBlockY());
        this.maxZ = Math.max(first.getBlockZ(), second.getBlockZ());
    }

    public boolean hasRegion() {
        return maxX >= minX && maxY >= minY && maxZ >= minZ;
    }

    public boolean contains(Block block) {
        if (worldId == null || block.getWorld() == null || !worldId.equals(block.getWorld().getUID()) || !hasRegion()) {
            return false;
        }
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
