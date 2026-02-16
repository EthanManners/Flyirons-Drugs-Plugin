package com.drugs;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
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
        if (block.getType() != Material.FERN && block.getType() != Material.LARGE_FERN) return;

        Block rootBlock = getRootFernBlock(block);
        String parentStrain = CannabisPlantRegistry.getPlant(rootBlock.getLocation());
        if (parentStrain == null) return;

        // Normal break (non-shears): return one inherited/mutated fern.
        String childStrain = StrainConfigLoader.rollChildStrain(parentStrain);
        ItemStack drop = createStrainFern(childStrain);

        event.setDropItems(false);
        event.setExpToDrop(0);
        clearFernPlant(rootBlock);

        Item droppedItem = rootBlock.getWorld().dropItemNaturally(rootBlock.getLocation().add(0.5, 0.2, 0.5), drop);
        droppedItem.setPickupDelay(10);

        CannabisPlantRegistry.removePlant(rootBlock.getLocation());
    }

    @EventHandler
    public void onFernBonemeal(BlockFertilizeEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.FERN) return;

        String parentStrain = CannabisPlantRegistry.getPlant(block.getLocation());
        if (parentStrain == null) return;

        // Keep strain anchored at bottom block after bonemeal growth into large fern.
        CannabisPlantRegistry.setPlant(block.getLocation(), parentStrain);
    }

    @EventHandler
    public void onLargeFernShear(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) return;
        if (event.getClickedBlock() == null) return;

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.SHEARS) return;

        Block clicked = event.getClickedBlock();
        if (clicked.getType() != Material.LARGE_FERN) return;

        Block rootBlock = getRootFernBlock(clicked);
        String parentStrain = CannabisPlantRegistry.getPlant(rootBlock.getLocation());
        if (parentStrain == null) {
            parentStrain = DrugItemMetadata.DEFAULT_STRAIN_ID;
        }

        event.setCancelled(true);

        clearFernPlant(rootBlock);
        CannabisPlantRegistry.removePlant(rootBlock.getLocation());

        // Shearing large fern should drop two small ferns, each with independent mutation roll.
        for (int i = 0; i < 2; i++) {
            String childStrain = StrainConfigLoader.rollChildStrain(parentStrain);
            ItemStack drop = createStrainFern(childStrain);
            Item dropped = rootBlock.getWorld().dropItemNaturally(rootBlock.getLocation().add(0.5, 0.2, 0.5), drop);
            dropped.setPickupDelay(10);
        }

        if (hand.hasItemMeta() && hand.getItemMeta() instanceof Damageable damageable) {
            int nextDamage = damageable.getDamage() + 1;
            damageable.setDamage(Math.min(nextDamage, hand.getType().getMaxDurability()));
            hand.setItemMeta(damageable);
        }
    }

    private Block getRootFernBlock(Block block) {
        if (block.getType() == Material.LARGE_FERN && block.getRelative(0, -1, 0).getType() == Material.LARGE_FERN) {
            return block.getRelative(0, -1, 0);
        }
        return block;
    }

    private void clearFernPlant(Block rootBlock) {
        if (rootBlock.getType() == Material.FERN) {
            rootBlock.setType(Material.AIR);
            return;
        }

        if (rootBlock.getType() == Material.LARGE_FERN) {
            Block top = rootBlock.getRelative(0, 1, 0);
            if (top.getType() == Material.LARGE_FERN) {
                top.setType(Material.AIR);
            }
            rootBlock.setType(Material.AIR);
        }
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
