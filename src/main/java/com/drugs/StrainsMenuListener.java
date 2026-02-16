package com.drugs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class StrainsMenuListener implements Listener {

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!StrainsMenuGUI.isStrainsMenu(title)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || clicked.getItemMeta() == null) {
            return;
        }

        String clickedName = ChatColor.stripColor(clicked.getItemMeta().getDisplayName());
        int currentPage = StrainsMenuGUI.parseGlobalPage(title);
        if ("Next Page".equalsIgnoreCase(clickedName)) {
            StrainsMenuGUI.open(player, currentPage + 1);
        } else if ("Previous Page".equalsIgnoreCase(clickedName)) {
            StrainsMenuGUI.open(player, currentPage - 1);
        }
    }
}
