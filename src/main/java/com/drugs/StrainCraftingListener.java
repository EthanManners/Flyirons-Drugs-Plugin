package com.drugs;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class StrainCraftingListener implements Listener {

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        applyStrainToResult(event.getInventory());
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        if (event.getInventory() instanceof CraftingInventory craftingInventory) {
            applyStrainToResult(craftingInventory);
        }
    }

    private void applyStrainToResult(CraftingInventory inventory) {
        ItemStack result = inventory.getResult();
        if (result == null) return;

        DrugEffectProfile profile = DrugRegistry.getProfileFromItem(result);
        if (profile == null || !StrainConfigLoader.isCannabisDrug(profile.getId())) return;

        String strainId = null;
        for (ItemStack ingredient : inventory.getMatrix()) {
            if (ingredient == null || !ingredient.hasItemMeta()) continue;
            String ingredientStrain = DrugItemMetadata.getStrainId(ingredient);
            if (ingredientStrain == null) continue;

            if (strainId == null) {
                strainId = ingredientStrain;
            } else if (!strainId.equalsIgnoreCase(ingredientStrain)) {
                // Mixed strains in a recipe are not allowed for deterministic outputs.
                inventory.setResult(null);
                return;
            }
        }

        if (strainId == null) {
            strainId = DrugItemMetadata.DEFAULT_STRAIN_ID;
        }

        inventory.setResult(DrugRegistry.getDrugItem(profile.getId(), result.getAmount(), strainId));
    }
}
