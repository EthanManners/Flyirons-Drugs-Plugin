package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a drug's data and how it behaves.
 */
public class DrugEffectProfile {

    /**
     * Sentinel value used to indicate a negative cache result
     */
    public static final DrugEffectProfile NONE = new DrugEffectProfile("none", new ArrayList<>(), Material.AIR, "", new ArrayList<>());

    private final String id;
    private final List<PotionEffect> effects;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    
    // Pre-computed values for performance
    private final String strippedDisplayName;
    private final ItemStack cachedItem;
    private final List<String> formattedLore;

    public DrugEffectProfile(String id, List<PotionEffect> effects, Material material, String displayName, List<String> lore) {
        this.id = id;
        this.effects = new CopyOnWriteArrayList<>(effects); // Thread-safe list
        this.material = material;
        this.displayName = displayName;
        this.lore = new ArrayList<>(lore); // Make a copy to prevent modification
        
        // Pre-compute values for performance
        this.strippedDisplayName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', displayName));
        
        // Pre-format lore for reuse
        List<String> tempLore = new ArrayList<>();
        for (String line : lore) {
            tempLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        this.formattedLore = Collections.unmodifiableList(tempLore);
        
        // Create a cached item with 1 amount
        this.cachedItem = createItemInternal(1);
    }

    /**
     * Applies the drug effects to the player, scaling with tolerance.
     */
    public void applyEffects(Player player, ItemStack sourceItem) {
        int toleranceLevel = ToleranceTracker.getToleranceLevel(player, id);
        int max = ToleranceConfigLoader.getMaxTolerance(id);
        double multiplier = ToleranceTracker.getEffectivenessMultiplier(player, id);

        spawnConsumptionParticles(player);

        StrainProfile strainProfile = null;
        if (StrainConfigLoader.isCannabisDrug(id)) {
            String strainId = DrugItemMetadata.getStrainId(sourceItem);
            strainProfile = StrainConfigLoader.getStrain(strainId);
        }

        // Maxed tolerance logic
        if (toleranceLevel >= max) {
            ToleranceTracker.onDrugUse(player, id); // consume, track use
            AddictionManager.onDrugUse(player, id);

            int overdoseCount = ToleranceTracker.incrementOverdoseCount(player, id);
            boolean shouldDie = OverdoseEffectManager.processOverdose(player, id, overdoseCount);
            
            if (shouldDie) {
                // Let the overdose manager handle the death effects
                player.setHealth(0.0);
            } else if (overdoseCount < 3) {
                player.sendMessage(ChatColor.RED + "You're too tolerant to feel anything.");
            }

            return;
        }

        // Apply scaled effects
        for (PotionEffect baseEffect : effects) {
            int newDuration = (int) (baseEffect.getDuration() * multiplier);
            int amplifier = baseEffect.getAmplifier();

            if (strainProfile != null) {
                newDuration = (int) Math.max(1, Math.round(newDuration * strainProfile.getDurationMultiplier()));
                amplifier = Math.max(0, (int) Math.round(amplifier * strainProfile.getAmplifierMultiplier()));
            }

            if (newDuration > 0) {
                player.addPotionEffect(new PotionEffect(
                        baseEffect.getType(),
                        newDuration,
                        amplifier,
                        baseEffect.isAmbient(),
                        baseEffect.hasParticles(),
                        baseEffect.hasIcon()
                ));
            }
        }

        ToleranceTracker.onDrugUse(player, id);
        AddictionManager.onDrugUse(player, id);
    }


    private void spawnConsumptionParticles(Player player) {
        if (player == null) return;

        Location location = player.getLocation().add(0, 1.1, 0);
        if (id.equalsIgnoreCase("blunt") || id.equalsIgnoreCase("joint")) {
            player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, location, 10, 0.25, 0.35, 0.25, 0.01);
            return;
        }

        if (id.equalsIgnoreCase("cart")) {
            player.getWorld().spawnParticle(Particle.POOF, location, 12, 0.25, 0.3, 0.25, 0.02);
        }
    }

    /**
     * Creates a drug item with the specified amount.
     * Uses cached values for performance.
     */
    public ItemStack createItem(int amount) {
        // If amount is 1, return a clone of the cached item
        if (amount == 1) {
            return cachedItem.clone();
        }
        
        // Otherwise create a new item with the specified amount
        return createItemInternal(amount);
    }

    public ItemStack createItem(int amount, String strainId) {
        ItemStack item = createItem(amount);
        if (!StrainConfigLoader.isCannabisDrug(id) || item == null || !item.hasItemMeta()) {
            return item;
        }

        StrainProfile strain = StrainConfigLoader.getStrain(strainId);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String resolved = strain != null ? strain.getId() : DrugItemMetadata.DEFAULT_STRAIN_ID;
        DrugItemMetadata.setStrainId(meta, resolved);
        List<String> lore = new ArrayList<>(meta.hasLore() ? Objects.requireNonNull(meta.getLore()) : Collections.emptyList());
        lore.removeIf(line -> ChatColor.stripColor(line).toLowerCase().startsWith("strain:"));
        lore.add(ChatColor.DARK_GREEN + "Strain: " + ChatColor.GREEN + (strain != null ? strain.getDisplayName() : resolved));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * Internal method to create an item stack
     */
    private ItemStack createItemInternal(int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            DrugItemMetadata.setDrugId(meta, id);
            DrugItemMetadata.setItemType(meta, id);

            List<String> loreWithStrain = new ArrayList<>(formattedLore);
            if (StrainConfigLoader.isCannabisDrug(id)) {
                String defaultStrain = DrugItemMetadata.DEFAULT_STRAIN_ID;
                DrugItemMetadata.setStrainId(meta, defaultStrain);
                StrainProfile profile = StrainConfigLoader.getStrain(defaultStrain);
                if (profile != null) {
                    loreWithStrain.add(ChatColor.DARK_GREEN + "Strain: " + ChatColor.GREEN + profile.getDisplayName());
                }
            }

            meta.setLore(loreWithStrain);
            if (id.equalsIgnoreCase("cart")) {
                int maxDurability = MechanicsConfig.getCartDurabilityUses();
                DrugItemMetadata.setCartDurability(meta, maxDurability);
                DrugItemMetadata.applyCartDurabilityLore(meta, maxDurability, maxDurability);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if an item matches this drug profile.
     * Optimized for performance.
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        String taggedDrugId = DrugItemMetadata.getDrugId(meta);
        if (taggedDrugId != null) {
            return taggedDrugId.equalsIgnoreCase(id);
        }
        if (!meta.hasDisplayName()) return false;

        String itemName = ChatColor.stripColor(meta.getDisplayName());
        return itemName.equalsIgnoreCase(strippedDisplayName);
    }

    public String getId() {
        return id;
    }

    public List<PotionEffect> getEffects() {
        return Collections.unmodifiableList(effects); // Return unmodifiable to prevent changes
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return Collections.unmodifiableList(lore); // Return unmodifiable to prevent changes
    }
    
    /**
     * Gets the pre-formatted lore with color codes translated
     */
    public List<String> getFormattedLore() {
        return formattedLore;
    }
}
