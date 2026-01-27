package com.drugs;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class OverdoseDeathListener implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        OverdoseEffectManager.DeathMessageContext context =
                OverdoseEffectManager.consumeDeathMessage(player.getUniqueId());
        if (context == null) return;

        if (!context.broadcast()) {
            event.setDeathMessage(null);
            return;
        }

        String message = context.message();
        if (message == null || message.isBlank()) {
            event.setDeathMessage(null);
            return;
        }

        event.setDeathMessage(message);
    }
}
