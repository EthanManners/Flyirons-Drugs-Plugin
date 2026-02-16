package com.drugs;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Handles placing and harvesting of strain-tagged fern plants.
 */
public class CannabisPlantListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || item.getType() != Material.FERN) return;

        String strainId = DrugItemMetadata.DEFAULT_STRAIN_ID;
        if (item.hasItemMeta()) {
            String itemType = DrugItemMetadata.getItemType(item.getItemMeta());
            if (itemType != null && !"cannabis_plant".equalsIgnoreCase(itemType)) return;
            strainId = DrugItemMetadata.getStrainId(item);
            if (strainId == null) {
                strainId = DrugItemMetadata.DEFAULT_STRAIN_ID;
            }
        }

        CannabisPlantRegistry.setPlant(event.getBlockPlaced().getLocation(), strainId);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.FERN) return;

        String parentStrain = CannabisPlantRegistry.getPlant(block.getLocation());
        if (parentStrain == null) return;

        String childStrain = StrainConfigLoader.rollChildStrain(parentStrain);
        ItemStack drop = createStrainFern(childStrain);

        event.setDropItems(false);
        event.setExpToDrop(0);
        block.setType(Material.AIR);

        Item droppedItem = block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.2, 0.5), drop);
        droppedItem.setPickupDelay(10);

        CannabisPlantRegistry.removePlant(block.getLocation());
    }

    public static ItemStack createStrainFern(String strainId) {
        StrainProfile profile = StrainConfigLoader.getStrain(strainId);
        String resolved = profile != null ? profile.getId() : DrugItemMetadata.DEFAULT_STRAIN_ID;

        ItemStack item = new ItemStack(Material.FERN, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            DrugItemMetadata.setItemType(meta, "cannabis_plant");
            DrugItemMetadata.setStrainId(meta, resolved);
            meta.setDisplayName("§2Cannabis Fern §7(§f" + (profile != null ? profile.getDisplayName() : resolved) + "§7)");
            meta.setLore(java.util.Arrays.asList(
                    "§7Strain: §a" + (profile != null ? profile.getDisplayName() : resolved),
                    "§7Rarity: §e" + (profile != null ? profile.getRarity() : "common"),
                    "§7Mutation Chance: §f" + String.format("%.2f%%", (profile != null ? profile.getMutationChance() : 0.005) * 100.0)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
}
