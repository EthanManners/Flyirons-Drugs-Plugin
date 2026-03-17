package com.drugs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles Salvia's block-disguise effect.
 */
public class SalviaEffectManager implements Listener {

    private final DrugsV2 plugin;
    private final Map<UUID, BlockDisplay> activeDisplays = new HashMap<>();
    private final Map<UUID, BukkitTask> movementTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> endTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> activationDelayTasks = new HashMap<>();
    private final Map<UUID, Location> lastKnownLocations = new HashMap<>();

    public SalviaEffectManager(DrugsV2 plugin) {
        this.plugin = plugin;
    }

    public void startEffect(Player player) {
        UUID uuid = player.getUniqueId();
        cleanupPlayer(uuid);

        int delaySeconds = Math.max(0, plugin.getConfig().getInt("salvia.delay", 3));
        int durationSeconds = Math.max(1, plugin.getConfig().getInt("salvia.duration", 45));

        long delayTicks = delaySeconds * 20L;
        long durationTicks = durationSeconds * 20L;

        BukkitTask activationTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player online = plugin.getServer().getPlayer(uuid);
            if (online == null || !online.isOnline() || online.isDead()) {
                cleanupPlayer(uuid);
                return;
            }

            online.setInvisible(true);

            Location startLocation = online.getLocation();
            BlockDisplay blockDisplay = online.getWorld().spawn(startLocation, BlockDisplay.class, display -> {
                display.setBlock(Material.DIRT.createBlockData());
                display.setTeleportDuration(1);
            });

            activeDisplays.put(uuid, blockDisplay);
            lastKnownLocations.put(uuid, startLocation.clone());

            BukkitTask movementTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                Player current = plugin.getServer().getPlayer(uuid);
                BlockDisplay display = activeDisplays.get(uuid);
                if (current == null || !current.isOnline() || current.isDead() || display == null || !display.isValid()) {
                    cleanupPlayer(uuid);
                    return;
                }

                Location next = current.getLocation();
                display.teleport(next);
                lastKnownLocations.put(uuid, next.clone());
            }, 2L, 2L);
            movementTasks.put(uuid, movementTask);

            BukkitTask stopTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> cleanupPlayer(uuid), durationTicks);
            endTasks.put(uuid, stopTask);

            activationDelayTasks.remove(uuid);
        }, delayTicks);

        activationDelayTasks.put(uuid, activationTask);
    }

    public void cleanupAll() {
        for (UUID uuid : activeDisplays.keySet().toArray(new UUID[0])) {
            cleanupPlayer(uuid);
        }

        for (BukkitTask task : activationDelayTasks.values()) {
            task.cancel();
        }
        activationDelayTasks.clear();
    }

    private void cleanupPlayer(UUID uuid) {
        BukkitTask movementTask = movementTasks.remove(uuid);
        if (movementTask != null) {
            movementTask.cancel();
        }

        BukkitTask endTask = endTasks.remove(uuid);
        if (endTask != null) {
            endTask.cancel();
        }

        BukkitTask activationDelayTask = activationDelayTasks.remove(uuid);
        if (activationDelayTask != null) {
            activationDelayTask.cancel();
        }

        BlockDisplay display = activeDisplays.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }

        lastKnownLocations.remove(uuid);

        Player player = plugin.getServer().getPlayer(uuid);
        if (player != null && player.isOnline()) {
            player.setInvisible(false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        cleanupPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        cleanupPlayer(event.getEntity().getUniqueId());
    }
}
