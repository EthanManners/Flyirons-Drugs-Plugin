package com.drugs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a configured cure item or action.
 */
public class CureProfile {

    private final String id;
    private final boolean enabled;
    private final boolean itemEnabled;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final List<String> cures;
    private final boolean clearsPoints;
    private final double reducePoints;
    private final int blockWithdrawalSeconds;

    private final String strippedDisplayName;
    private final ItemStack cachedItem;
    private final List<String> formattedLore;

    public CureProfile(String id,
                       boolean enabled,
                       boolean itemEnabled,
                       Material material,
                       String displayName,
                       List<String> lore,
                       List<String> cures,
                       boolean clearsPoints,
                       double reducePoints,
                       int blockWithdrawalSeconds) {
        this.id = id;
        this.enabled = enabled;
        this.itemEnabled = itemEnabled;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore == null ? List.of() : new ArrayList<>(lore);
        this.cures = cures == null ? List.of() : new ArrayList<>(cures);
        this.clearsPoints = clearsPoints;
        this.reducePoints = reducePoints;
        this.blockWithdrawalSeconds = blockWithdrawalSeconds;

        this.strippedDisplayName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', displayName));
        List<String> tempLore = new ArrayList<>();
        for (String line : this.lore) {
            tempLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        this.formattedLore = Collections.unmodifiableList(tempLore);
        this.cachedItem = createItemInternal(1);
    }

    public ItemStack createItem(int amount) {
        if (amount == 1) {
            return cachedItem.clone();
        }
        return createItemInternal(amount);
    }

    private ItemStack createItemInternal(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            meta.setLore(new ArrayList<>(formattedLore));
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String itemName = ChatColor.stripColor(meta.getDisplayName());
        return itemName.equalsIgnoreCase(strippedDisplayName);
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isItemEnabled() {
        return itemEnabled;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore);
    }

    public List<String> getCures() {
        return Collections.unmodifiableList(cures);
    }

    public boolean isClearsPoints() {
        return clearsPoints;
    }

    public double getReducePoints() {
        return reducePoints;
    }

    public int getBlockWithdrawalSeconds() {
        return blockWithdrawalSeconds;
    }
}
