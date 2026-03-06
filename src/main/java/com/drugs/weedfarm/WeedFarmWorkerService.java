package com.drugs.weedfarm;

import com.drugs.CannabisPlantListener;
import com.drugs.MechanicsConfig;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class WeedFarmWorkerService implements Runnable {
    private static final double MAX_WORK_DISTANCE_SQUARED = 24 * 24;
    private static final double WORK_REACH_DISTANCE_SQUARED = 2.5 * 2.5;

    private final WeedFarmManager manager;
    private final Random random = new Random();

    public WeedFarmWorkerService(WeedFarmManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        for (WeedFarm farm : manager.getFarms()) {
            manager.pruneInvalidVillagers(farm);
            if (!farm.isEnabled() || farm.getAssignedVillagers().isEmpty() || !farm.hasRegion()) {
                continue;
            }

            World world = Bukkit.getWorld(farm.getWorldId());
            if (world == null || world.getPlayers().stream().noneMatch(Player::isOnline)) {
                continue;
            }

            processFarm(farm, world);
        }
    }

    private void processFarm(WeedFarm farm, World world) {
        List<Villager> workers = new ArrayList<>();
        for (UUID workerId : farm.getAssignedVillagers()) {
            Entity entity = Bukkit.getEntity(workerId);
            if (entity instanceof Villager villager && villager.getWorld().equals(world) && villager.isValid() && !villager.isDead()) {
                workers.add(villager);
            }
        }
        if (workers.isEmpty()) {
            return;
        }

        int speedMultiplier = Math.min(WeedFarm.MAX_WORKERS, workers.size());
        int maxHarvest = Math.max(1, MechanicsConfig.getMaxHarvestPerCycle() * speedMultiplier);
        int maxPlant = Math.max(1, MechanicsConfig.getMaxPlantPerCycle() * speedMultiplier);

        List<ItemStack> workerSeeds = new ArrayList<>();
        int harvests = 0;
        int plants = 0;
        int inspected = 0;
        int total = farmVolume(farm);
        int cursor = farm.getScanCursor();

        while (inspected < total && (harvests < maxHarvest || plants < maxPlant)) {
            int index = (cursor + inspected) % total;
            Block block = blockAtIndex(world, farm, index);

            if (harvests < maxHarvest && block.getType() == Material.LARGE_FERN) {
                Villager worker = selectReachableWorker(workers, block);
                if (worker != null) {
                    CannabisPlantListener.HarvestResult result = CannabisPlantListener.harvestCannabis(block, true);
                    if (result != null) {
                        workerSeeds.addAll(result.getDrops());
                        harvests++;
                        moveWorker(worker, result.getRootBlock().getLocation().add(0.5, 0, 0.5));
                        result.getRootBlock().getWorld().spawnParticle(Particle.HAPPY_VILLAGER, result.getRootBlock().getLocation().add(0.5, 0.8, 0.5), 6, 0.2, 0.3, 0.2, 0.0);
                    }
                }
            }

            if (plants < maxPlant && !workerSeeds.isEmpty() && isPlantable(block)) {
                ItemStack seed = workerSeeds.remove(workerSeeds.size() - 1);
                block.setType(Material.FERN, false);
                if (CannabisPlantListener.applyPlacedCannabis(block, seed)) {
                    plants++;
                    moveWorker(workers.get(random.nextInt(workers.size())), block.getLocation().add(0.5, 0, 0.5));
                    world.spawnParticle(Particle.COMPOSTER, block.getLocation().add(0.5, 0.8, 0.5), 4, 0.2, 0.1, 0.2, 0.0);
                } else {
                    block.setType(Material.AIR, false);
                }
            }

            inspected++;
        }

        farm.setScanCursor((cursor + inspected) % Math.max(1, total));

        for (ItemStack seed : workerSeeds) {
            world.dropItemNaturally(farm.getControllerLocation(), seed);
        }
    }

    private Villager selectReachableWorker(List<Villager> workers, Block targetPlant) {
        List<Villager> shuffled = new ArrayList<>(workers);
        java.util.Collections.shuffle(shuffled, random);
        for (Villager worker : shuffled) {
            if (canReach(worker, targetPlant)) {
                return worker;
            }
        }
        return null;
    }

    private boolean canReach(Villager villager, Block targetPlant) {
        if (villager.isDead() || !villager.isValid() || villager.getWorld() != targetPlant.getWorld()) {
            return false;
        }

        if (!isOccupiable(villager.getLocation().getBlock())) {
            return false;
        }

        Location stand = findStandLocation(targetPlant);
        if (stand == null) {
            return false;
        }

        double distSq = villager.getLocation().distanceSquared(stand);
        if (distSq > MAX_WORK_DISTANCE_SQUARED) {
            return false;
        }

        villager.getPathfinder().moveTo(stand);

        if (distSq <= WORK_REACH_DISTANCE_SQUARED) {
            return true;
        }

        return hasLineOfTravel(villager.getEyeLocation(), stand.clone().add(0, 1.0, 0));
    }

    private boolean hasLineOfTravel(Location from, Location to) {
        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length <= 0.001) {
            return true;
        }

        return from.getWorld().rayTraceBlocks(
                from,
                direction.normalize(),
                length,
                FluidCollisionMode.NEVER,
                true
        ) == null;
    }

    private Location findStandLocation(Block plantBlock) {
        Block root = CannabisPlantListener.getRootFernBlock(plantBlock);
        Block[] candidates = new Block[] {
                root.getRelative(1, 0, 0),
                root.getRelative(-1, 0, 0),
                root.getRelative(0, 0, 1),
                root.getRelative(0, 0, -1)
        };

        for (Block candidate : candidates) {
            if (isOccupiable(candidate)) {
                return candidate.getLocation().add(0.5, 0, 0.5);
            }
        }
        return null;
    }

    private boolean isOccupiable(Block feet) {
        Block head = feet.getRelative(0, 1, 0);
        Block ground = feet.getRelative(0, -1, 0);
        return feet.isPassable() && head.isPassable() && ground.getType().isSolid();
    }

    private void moveWorker(Villager villager, Location target) {
        villager.swingMainHand();
        villager.getPathfinder().moveTo(target);
    }

    private boolean isPlantable(Block block) {
        return block.getType().isAir() && block.getRelative(0, -1, 0).getType() == Material.DIRT;
    }

    private int farmVolume(WeedFarm farm) {
        return (farm.getMaxX() - farm.getMinX() + 1) * (farm.getMaxY() - farm.getMinY() + 1) * (farm.getMaxZ() - farm.getMinZ() + 1);
    }

    private Block blockAtIndex(World world, WeedFarm farm, int index) {
        int sizeX = farm.getMaxX() - farm.getMinX() + 1;
        int sizeY = farm.getMaxY() - farm.getMinY() + 1;
        int sizeZ = farm.getMaxZ() - farm.getMinZ() + 1;

        int x = index % sizeX;
        int rem = index / sizeX;
        int y = rem % sizeY;
        int z = rem / sizeY;

        return world.getBlockAt(farm.getMinX() + x, farm.getMinY() + y, farm.getMinZ() + z);
    }
}
