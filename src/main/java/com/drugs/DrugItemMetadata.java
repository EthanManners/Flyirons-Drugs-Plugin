package com.drugs;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Utility class for writing/reading persistent metadata on drug items.
 */
public final class DrugItemMetadata {

    public static final String DEFAULT_STRAIN_ID = "reggie";

    private DrugItemMetadata() {
    }

    private static NamespacedKey key(String key) {
        return new NamespacedKey(DrugsV2.getInstance(), key);
    }

    public static void setDrugId(ItemMeta meta, String drugId) {
        meta.getPersistentDataContainer().set(key("drug_id"), PersistentDataType.STRING, drugId.toLowerCase());
    }

    public static void setItemType(ItemMeta meta, String itemType) {
        meta.getPersistentDataContainer().set(key("item_type"), PersistentDataType.STRING, itemType.toLowerCase());
    }

    public static void setStrainId(ItemMeta meta, String strainId) {
        meta.getPersistentDataContainer().set(key("strain_id"), PersistentDataType.STRING, strainId.toLowerCase());
    }

    public static String getDrugId(ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(key("drug_id"), PersistentDataType.STRING);
    }

    public static String getItemType(ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(key("item_type"), PersistentDataType.STRING);
    }

    public static String getStrainId(ItemMeta meta) {
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(key("strain_id"), PersistentDataType.STRING);
    }

    public static String getStrainId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return getStrainId(item.getItemMeta());
    }

    public static boolean hasDrugId(ItemMeta meta) {
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(key("drug_id"), PersistentDataType.STRING);
    }
}
