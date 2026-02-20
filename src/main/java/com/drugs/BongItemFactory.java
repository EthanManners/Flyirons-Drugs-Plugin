package com.drugs;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public final class BongItemFactory {

    private BongItemFactory() {
    }

    public static ItemStack createBongItem(int amount) {
        return createBongItem(amount, MechanicsConfig.getBongDurabilityUses());
    }

    public static ItemStack createBongItem(int amount, int durability) {
        ItemStack item = new ItemStack(BongConfigLoader.getBongItemMaterial(), Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', BongConfigLoader.getBongDisplayName()));
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Place on a block to set up a bong.",
                ChatColor.DARK_GREEN + "Right-click with weed to take a hit."
        ));
        if (BongConfigLoader.getBongCustomModelData() > 0) {
            meta.setCustomModelData(BongConfigLoader.getBongCustomModelData());
        }
        DrugItemMetadata.setItemType(meta, "bong");
        int maxDurability = MechanicsConfig.getBongDurabilityUses();
        int appliedDurability = Math.max(0, Math.min(durability, maxDurability));
        DrugItemMetadata.setBongDurability(meta, appliedDurability);
        DrugItemMetadata.applyBongDurabilityLore(meta, appliedDurability, maxDurability);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBongItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        String itemType = DrugItemMetadata.getItemType(item.getItemMeta());
        return itemType != null && itemType.equalsIgnoreCase("bong");
    }
}
