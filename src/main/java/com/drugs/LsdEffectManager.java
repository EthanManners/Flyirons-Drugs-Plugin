package com.drugs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles LSD-specific visual and nausea effects.
 */
public class LsdEffectManager implements Listener {

    private static final BlockData PORTAL_BLOCK = Material.NETHER_PORTAL.createBlockData();
    private static final BlockData AIR_BLOCK = Material.AIR.createBlockData();

    private final DrugsV2 plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> endTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> activationDelayTasks = new HashMap<>();
    private final Map<UUID, Location> lastKnownLocations = new HashMap<>();

    public LsdEffectManager(DrugsV2 plugin) {
        this.plugin = plugin;
    }

    public void startEffect(Player player) {
        UUID uuid = player.getUniqueId();
        cleanupPlayer(uuid);

        int delaySeconds = Math.max(0, plugin.getConfig().getInt("lsd.delay", 5));
        int durationSeconds = Math.max(1, plugin.getConfig().getInt("lsd.duration", 180));

        long delayTicks = delaySeconds * 20L;
        long durationTicks = durationSeconds * 20L;

        BukkitTask activationTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player online = plugin.getServer().getPlayer(uuid);
            if (online == null || !online.isOnline() || online.isDead()) {
                cleanupPlayer(uuid);
                return;
            }

            Location initialLocation = online.getLocation().getBlock().getLocation();
            online.sendBlockChange(initialLocation, PORTAL_BLOCK);
            lastKnownLocations.put(uuid, initialLocation);

            online.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (int) durationTicks, 0, false, true, true));

            BukkitTask moveTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                Player current = plugin.getServer().getPlayer(uuid);
                if (current == null || !current.isOnline() || current.isDead()) {
                    cleanupPlayer(uuid);
                    return;
                }

                Location previous = lastKnownLocations.get(uuid);
                if (previous != null) {
                    current.sendBlockChange(previous, AIR_BLOCK);
                }

                Location next = current.getLocation().getBlock().getLocation();
                current.sendBlockChange(next, PORTAL_BLOCK);
                lastKnownLocations.put(uuid, next);
            }, 2L, 2L);
            activeTasks.put(uuid, moveTask);

            BukkitTask stopTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                cleanupPlayer(uuid);
            }, durationTicks);
            endTasks.put(uuid, stopTask);

            activationDelayTasks.remove(uuid);
        }, delayTicks);

        activationDelayTasks.put(uuid, activationTask);
    }

    public void cleanupAll() {
        for (UUID uuid : lastKnownLocations.keySet().toArray(new UUID[0])) {
            cleanupPlayer(uuid);
        }

        for (BukkitTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();

        for (BukkitTask task : endTasks.values()) {
            task.cancel();
        }
        endTasks.clear();

        for (BukkitTask task : activationDelayTasks.values()) {
            task.cancel();
        }
        activationDelayTasks.clear();
    }

    private void cleanupPlayer(UUID uuid) {
        BukkitTask activeTask = activeTasks.remove(uuid);
        if (activeTask != null) {
            activeTask.cancel();
        }

        BukkitTask endTask = endTasks.remove(uuid);
        if (endTask != null) {
            endTask.cancel();
        }

        BukkitTask activationDelayTask = activationDelayTasks.remove(uuid);
        if (activationDelayTask != null) {
            activationDelayTask.cancel();
        }

        Player player = plugin.getServer().getPlayer(uuid);
        Location last = lastKnownLocations.remove(uuid);
        if (player != null && player.isOnline()) {
            if (last != null) {
                player.sendBlockChange(last, AIR_BLOCK);
            }
            player.removePotionEffect(PotionEffectType.NAUSEA);
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
