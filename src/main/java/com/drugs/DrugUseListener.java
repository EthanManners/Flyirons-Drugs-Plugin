package com.drugs;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DrugUseListener implements Listener {

    // Track drug use count per player
    private static final Map<String, Integer> drugUseCount = new HashMap<>();

    @EventHandler
    public void onDrugUse(PlayerInteractEvent event) {
        // Only handle right-click interactions for drug use
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Skip if item is null or air
        if (item == null || item.getType().isAir()) return;

        DrugEffectProfile profile = DrugRegistry.getProfileFromItem(item);
        if (profile == null) return;

        String drugId = profile.getId();

        if (item.hasItemMeta() && !DrugItemMetadata.hasDrugId(item.getItemMeta())) {
            ItemStack migrated = DrugRegistry.getDrugItem(drugId, item.getAmount(), DrugItemMetadata.getStrainId(item));
            if (migrated != null) {
                player.getInventory().setItemInMainHand(migrated);
                item = migrated;
            }
        }

        // Only cancel the event if we've confirmed this is a valid drug use
        event.setCancelled(true);

        // ----------------------------------------
        // Track drug use count for achievements
        // ----------------------------------------
        String playerKey = player.getUniqueId().toString();
        int useCount = drugUseCount.getOrDefault(playerKey, 0) + 1;
        drugUseCount.put(playerKey, useCount);

        // ----------------------------------------
        // Process achievement triggers
        // ----------------------------------------
        
        // First drug use achievement
        Map<String, Object> firstUseContext = AchievementManager.createContext();
        AchievementManager.processTrigger(player, "first_drug_use", firstUseContext);
        
        // Specific drug use achievement
        Map<String, Object> specificDrugContext = AchievementManager.createContext();
        specificDrugContext.put("drug_id", drugId);
        AchievementManager.processTrigger(player, "use_specific_drug", specificDrugContext);
        
        // Use count achievement
        Map<String, Object> countContext = AchievementManager.createContext();
        countContext.put("count", useCount);
        AchievementManager.processTrigger(player, "use_count", countContext);
        
        // Track for connoisseur achievement (all drugs used)
        trackDrugUsedForConnoisseur(player, drugId);
        
        // Max tolerance achievement
        if (ToleranceTracker.isAtMaxTolerance(player.getUniqueId(), drugId)) {
            Map<String, Object> maxToleranceContext = AchievementManager.createContext();
            AchievementManager.processTrigger(player, "use_at_max", maxToleranceContext);
        }

        // ----------------------------------------
        // 🧪 Apply effects
        // ----------------------------------------
        profile.applyEffects(player, item);

        if (player.getGameMode() != GameMode.CREATIVE) {
            if (drugId.equalsIgnoreCase("cart")) {
                handleCartDurability(player, item);
            } else {
                int newAmount = item.getAmount() - 1;
                if (newAmount <= 0) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    item.setAmount(newAmount);
                }
            }
        }
    }
    

    private void handleCartDurability(Player player, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        var meta = item.getItemMeta();
        int maxDurability = MechanicsConfig.getCartDurabilityUses();
        Integer currentDurability = DrugItemMetadata.getCartDurability(meta);

        if (currentDurability == null || currentDurability <= 0 || currentDurability > maxDurability) {
            currentDurability = maxDurability;
        }

        int nextDurability = currentDurability - 1;
        if (nextDurability <= 0) {
            int newAmount = item.getAmount() - 1;
            if (newAmount <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(newAmount);
                var refreshedMeta = item.getItemMeta();
                if (refreshedMeta != null) {
                    DrugItemMetadata.setCartDurability(refreshedMeta, maxDurability);
                    DrugItemMetadata.applyCartDurabilityLore(refreshedMeta, maxDurability, maxDurability);
                    item.setItemMeta(refreshedMeta);
                }
            }
            return;
        }

        DrugItemMetadata.setCartDurability(meta, nextDurability);
        DrugItemMetadata.applyCartDurabilityLore(meta, nextDurability, maxDurability);
        item.setItemMeta(meta);
    }

    /**
     * Tracks drug usage for the connoisseur achievement
     */
    private void trackDrugUsedForConnoisseur(Player player, String drugId) {
        PlayerAchievementData data = new PlayerAchievementData(player.getUniqueId());
        String perDrugKey = "connoisseur-used-" + drugId;

        if (!data.hasAchievement(perDrugKey)) {
            data.grantAchievement(perDrugKey);

            Set<String> required = new HashSet<>();
            for (String id : DrugRegistry.getRegisteredDrugNames()) {
                required.add("connoisseur-used-" + id);
            }

            if (data.getUnlockedAchievements().containsAll(required)) {
                Map<String, Object> context = AchievementManager.createContext();
                AchievementManager.processTrigger(player, "all_drugs_used", context);
            }
        }
    }
}
