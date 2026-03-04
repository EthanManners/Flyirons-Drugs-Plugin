package com.drugs.weedfarm;

import com.drugs.DrugItemMetadata;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class WeedFarmItems {
    public static final String ITEM_TYPE_CONTROLLER = "farm_controller";
    public static final String ITEM_TYPE_AREA_WAND = "farm_area_wand";
    public static final String ITEM_TYPE_CHEST_WAND = "farm_chest_wand";
    public static final String ITEM_TYPE_CONTRACT = "farm_work_contract";

    private WeedFarmItems() {}

    public static ItemStack createControllerItem() {
        ItemStack stack = new ItemStack(Material.BARREL);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Farm Controller");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Place this barrel to create", ChatColor.GRAY + "an automated weed farm."));
            DrugItemMetadata.setItemType(meta, ITEM_TYPE_CONTROLLER);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack createAreaWand(String farmId) {
        ItemStack stack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Farm Area Wand");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Left click: set pos1", ChatColor.GRAY + "Right click: set pos2", ChatColor.DARK_GRAY + "Farm: " + farmId));
            DrugItemMetadata.setItemType(meta, ITEM_TYPE_AREA_WAND + ":" + farmId);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack createChestWand(String farmId) {
        ItemStack stack = new ItemStack(Material.STICK);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Chest Link Wand");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Right click a chest to link it", ChatColor.DARK_GRAY + "Farm: " + farmId));
            DrugItemMetadata.setItemType(meta, ITEM_TYPE_CHEST_WAND + ":" + farmId);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static ItemStack createContract(String farmId) {
        ItemStack stack = new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Work Contract");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Right click a villager while", ChatColor.GRAY + "looking at this farm controller.", ChatColor.DARK_GRAY + "Farm: " + farmId));
            DrugItemMetadata.setItemType(meta, ITEM_TYPE_CONTRACT + ":" + farmId);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public static String parseFarmId(ItemStack stack, String prefix) {
        if (stack == null || !stack.hasItemMeta()) return null;
        String itemType = DrugItemMetadata.getItemType(stack.getItemMeta());
        if (itemType == null || !itemType.startsWith(prefix + ":")) {
            return null;
        }
        return itemType.substring(prefix.length() + 1);
    }
}
