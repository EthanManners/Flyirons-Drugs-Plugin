package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads and registers cure items from addiction.yml and recipes.
 */
public class CureRegistry {

    private static final Map<String, CureProfile> cureProfiles = new HashMap<>();

    public static void init(Plugin plugin) {
        cureProfiles.clear();
        cureProfiles.putAll(AddictionConfigLoader.getCureProfiles());

        FileConfiguration recipes = plugin instanceof DrugsV2 dp ? dp.getRecipesConfig() : null;
        if (recipes == null) return;

        for (Map.Entry<String, CureProfile> entry : cureProfiles.entrySet()) {
            String cureId = entry.getKey();
            CureProfile profile = entry.getValue();
            if (!profile.isEnabled() || !profile.isItemEnabled()) {
                continue;
            }

            ConfigurationSection recipeSection = recipes.getConfigurationSection(cureId);
            if (recipeSection != null) {
                Bukkit.getLogger().info("[DrugsV2] Registering cure recipe for: " + cureId);
                DrugRecipeHelper.registerItemRecipe(cureId, recipeSection, profile.createItem(1), plugin);
            }
        }
    }

    public static CureProfile getProfileById(String id) {
        if (id == null) return null;
        return cureProfiles.get(id.toLowerCase());
    }

    public static ItemStack getCureItem(String id, int amount) {
        CureProfile profile = getProfileById(id);
        if (profile == null || !profile.isEnabled() || !profile.isItemEnabled()) return null;
        return profile.createItem(amount);
    }

    public static CureProfile getProfileFromItem(ItemStack item) {
        if (item == null) return null;
        for (CureProfile profile : cureProfiles.values()) {
            if (profile.isEnabled() && profile.isItemEnabled() && profile.matches(item)) {
                return profile;
            }
        }
        return null;
    }

    public static Set<String> getRegisteredCureNames() {
        return new HashSet<>(cureProfiles.keySet());
    }
}
