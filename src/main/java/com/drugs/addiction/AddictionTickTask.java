package com.drugs.addiction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class AddictionTickTask implements Runnable {

    private final JavaPlugin plugin;

    public AddictionTickTask(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        AddictionConfig cfg = AddictionConfigLoader.get();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();

            // Skip if player has no tracked addictions
            Map<String, AddictionState> map = AddictionManager.getAddictions(uuid);
            if (map.isEmpty()) continue;

            for (Map.Entry<String, AddictionState> entry : map.entrySet()) {
                String drugId = entry.getKey();
                AddictionState state = entry.getValue();

                AddictionConfig.DrugRule rule = cfg.getDrugRule(drugId);
                if (rule == null) continue; // not in config anymore

                // If not addictive, ignore
                if (!rule.addictive) continue;

                // Determine if they're "addicted"
                if (state.getPoints() < rule.addictedThreshold) {
                    // Not addicted yet; make sure withdrawal isn't being held over accidentally
                    // (we'll keep it simple: do nothing)
                    continue;
                }

                // If cure is blocking withdrawal, skip applying withdrawal effects
                if (state.isWithdrawalBlocked()) {
                    // Optional: clear withdrawal effects while blocked
                    AddictionManager.clearWithdrawalEffects(player, drugId);
                    continue;
                }

                long now = System.currentTimeMillis();
                long secondsSinceDose = (now - state.getLastDoseMillis()) / 1000L;

                // Not time yet
                if (secondsSinceDose < rule.withdrawalAfterSeconds) continue;

                // Apply withdrawal effects "constantly"
                AddictionManager.applyWithdrawalEffects(player, drugId, rule);
            }
        }
    }
}
