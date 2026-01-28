package com.drugs;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class AddictionListener implements Listener {

    @EventHandler
    public void onCureUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType().isAir()) return;

        CureProfile cure = CureRegistry.getProfileFromItem(item);
        if (cure == null) return;

        boolean applied = AddictionManager.applyCure(player, cure);
        if (!applied) return;

        event.setCancelled(true);

        if (player.getGameMode() != GameMode.CREATIVE) {
            int newAmount = item.getAmount() - 1;
            if (newAmount <= 0) {
                player.getInventory().setItemInMainHand(null);
            } else {
                item.setAmount(newAmount);
            }
        }
    }

    @EventHandler
    public void onSleepCure(PlayerBedEnterEvent event) {
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) return;

        CureProfile sleepCure = AddictionConfigLoader.getCureProfile("sleep");
        if (sleepCure == null || !sleepCure.isEnabled()) return;

        AddictionManager.applyCure(event.getPlayer(), sleepCure);
    }

    @EventHandler
    public void onMilkConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != Material.MILK_BUCKET) return;
        AddictionManager.handleMilkConsumed(event.getPlayer());
    }
}
