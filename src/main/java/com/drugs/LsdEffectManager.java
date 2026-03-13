package com.drugs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LsdEffectManager implements Listener {

    private final Plugin plugin;
    private final Map<UUID, LsdSession> activeLsdTasks = new HashMap<>();

    public LsdEffectManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void activate(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        clearPlayer(uuid);

        FileConfiguration config = plugin.getConfig();
        long activationDelayTicks = Math.max(0L, config.getLong("lsd.activation-delay-ticks", 40L));
        long durationTicks = Math.max(20L, config.getLong("lsd.duration-ticks", 2400L));
        int nauseaAmplifier = Math.max(0, config.getInt("lsd.nausea-amplifier", 0));

        LsdSession session = new LsdSession();
        activeLsdTasks.put(uuid, session);

        session.activationTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player onlinePlayer = plugin.getServer().getPlayer(uuid);
            if (onlinePlayer == null || !onlinePlayer.isOnline()) {
                clearPlayer(uuid);
                return;
            }

            onlinePlayer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, (int) durationTicks, nauseaAmplifier, false, true, true));

            session.overlayTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
                Player target = plugin.getServer().getPlayer(uuid);
                if (target == null || !target.isOnline()) {
                    clearPlayer(uuid);
                    return;
                }
                target.setPortalCooldown(100);
            }, 0L, 5L);

            session.endTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Player target = plugin.getServer().getPlayer(uuid);
                if (target != null && target.isOnline()) {
                    target.setPortalCooldown(0);
                }
                clearPlayer(uuid);
            }, durationTicks);
        }, activationDelayTicks);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        clearPlayer(uuid);
        event.getPlayer().setPortalCooldown(0);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        UUID uuid = event.getEntity().getUniqueId();
        clearPlayer(uuid);
        event.getEntity().setPortalCooldown(0);
    }

    public void shutdown() {
        for (UUID uuid : activeLsdTasks.keySet().toArray(new UUID[0])) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.setPortalCooldown(0);
            }
            clearPlayer(uuid);
        }
    }

    private void clearPlayer(UUID uuid) {
        LsdSession session = activeLsdTasks.remove(uuid);
        if (session == null) {
            return;
        }

        cancelTask(session.activationTask);
        cancelTask(session.overlayTask);
        cancelTask(session.endTask);
    }

    private void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    private static class LsdSession {
        private BukkitTask activationTask;
        private BukkitTask overlayTask;
        private BukkitTask endTask;
    }
}
