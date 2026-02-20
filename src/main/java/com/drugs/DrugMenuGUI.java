package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a paginated GUI showing all available drugs.
 */
public class DrugMenuGUI {

    private static final int ITEMS_PER_PAGE = 27; // 3 rows (excluding control row)
    private static final int GUI_SIZE = 36;       // 3x9 grid + 1 row for controls

    /**
     * Opens the drug selection menu for a player.
     *
     * @param player The player to open the menu for
     * @param page   The page number (0-based)
     */
    public static void open(Player player, int page) {
        List<ItemStack> menuItems = new ArrayList<>();
        List<String> drugIds = new ArrayList<>(DrugRegistry.getRegisteredDrugNames());
        List<String> cureIds = new ArrayList<>(CureRegistry.getRegisteredCureNames());
        drugIds.sort(String::compareToIgnoreCase);
        cureIds.sort(String::compareToIgnoreCase);

        for (String drugId : drugIds) {
            DrugEffectProfile profile = DrugRegistry.getProfileById(drugId);
            if (profile != null) {
                menuItems.add(profile.createItem(1));
            }
        }

        for (String cureId : cureIds) {
            CureProfile profile = CureRegistry.getProfileById(cureId);
            if (profile != null && profile.isEnabled() && profile.isItemEnabled()) {
                menuItems.add(profile.createItem(1));
            }
        }

        if (BongConfigLoader.isEnabled()) {
            menuItems.add(BongItemFactory.createBongItem(1));
        }

        int maxPage = Math.max(1, (int) Math.ceil(menuItems.size() / (double) ITEMS_PER_PAGE));

        // Clamp page number
        if (page < 0) page = 0;
        if (page >= maxPage) page = maxPage - 1;

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE, ChatColor.DARK_GREEN + "Drugs Menu (Page " + (page + 1) + ")");

        // Add drug items for current page
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, menuItems.size());

        for (int i = start; i < end; i++) {
            gui.setItem(i - start, menuItems.get(i));
        }

        // Add navigation controls (last row)
        if (page > 0) {
            gui.setItem(27, MenuUtils.createButtonItem("§ePrevious Page", org.bukkit.Material.ARROW));
        }
        if (page < maxPage - 1) {
            gui.setItem(35, MenuUtils.createButtonItem("§eNext Page", org.bukkit.Material.ARROW));
        }

        // Close button in the center
        gui.setItem(31, MenuUtils.createButtonItem("§cClose", org.bukkit.Material.BARRIER));

        player.openInventory(gui);
    }
}
