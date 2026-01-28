package com.drugs;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads addiction settings, drug profiles, and cure profiles from addiction.yml.
 */
public class AddictionConfigLoader {

    private static AddictionSettings settings = new AddictionSettings();
    private static final Map<String, AddictionDrugProfile> drugProfiles = new HashMap<>();
    private static final Map<String, CureProfile> cureProfiles = new HashMap<>();

    public static void load(Plugin plugin) {
        File file = new File(plugin.getDataFolder(), "addiction.yml");
        if (!file.exists()) {
            plugin.saveResource("addiction.yml", false);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        loadSettings(config.getConfigurationSection("settings"));
        loadCures(config.getConfigurationSection("cures"));
        loadDrugs(config.getConfigurationSection("drugs"));
    }

    private static void loadSettings(ConfigurationSection section) {
        if (section == null) {
            settings = new AddictionSettings();
            return;
        }

        settings = new AddictionSettings(
                section.getBoolean("enabled", true),
                section.getBoolean("milk_clears_withdrawal", false),
                section.getInt("infinite_duration_ticks", 99999),
                section.getInt("heartbeat_ticks", 20)
        );
    }

    private static void loadCures(ConfigurationSection section) {
        cureProfiles.clear();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cureSection = section.getConfigurationSection(key);
            if (cureSection == null) continue;

            boolean enabled = cureSection.getBoolean("enabled", true);
            boolean itemEnabled = cureSection.getBoolean("item_enabled", true);
            Material material = Material.matchMaterial(cureSection.getString("material", "PAPER"));
            if (material == null) {
                material = Material.PAPER;
            }

            String displayName = cureSection.getString("display-name", key);
            List<String> lore = cureSection.getStringList("lore");
            List<String> cures = cureSection.getStringList("cures");
            boolean clearsPoints = cureSection.getBoolean("clears_points", false);
            double reducePoints = cureSection.getDouble("reduce_points", 0);
            int blockWithdrawalSeconds = cureSection.getInt("block_withdrawal_seconds", 0);

            CureProfile profile = new CureProfile(
                    key,
                    enabled,
                    itemEnabled,
                    material,
                    displayName,
                    lore,
                    cures,
                    clearsPoints,
                    reducePoints,
                    blockWithdrawalSeconds
            );

            cureProfiles.put(key.toLowerCase(), profile);
        }
    }

    private static void loadDrugs(ConfigurationSection section) {
        drugProfiles.clear();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection drugSection = section.getConfigurationSection(key);
            if (drugSection == null) continue;

            boolean addictive = drugSection.getBoolean("addictive", false);
            double pointsPerUse = drugSection.getDouble("points_per_use", 0);
            double addictedAtPoints = drugSection.getDouble("addicted_at_points", 0);
            int withdrawalAfterSeconds = drugSection.getInt("withdrawal_after_seconds", 0);

            ConfigurationSection decaySection = drugSection.getConfigurationSection("decay");
            boolean decayEnabled = decaySection != null && decaySection.getBoolean("enabled", false);
            double decayPerMinute = decaySection != null ? decaySection.getDouble("points_decay_per_minute", 0) : 0;

            List<WithdrawalEffect> withdrawalEffects = new ArrayList<>();
            List<Map<?, ?>> effectsList = drugSection.getMapList("withdrawal_effects");
            for (Map<?, ?> entry : effectsList) {
                Object typeObj = entry.get("type");
                if (typeObj == null) continue;
                String typeName = String.valueOf(typeObj);
                int amplifier = 0;
                Object ampObj = entry.get("amplifier");
                if (ampObj instanceof Number number) {
                    amplifier = number.intValue();
                }
                WithdrawalEffect effect = WithdrawalEffect.from(typeName, amplifier);
                if (effect != null) {
                    withdrawalEffects.add(effect);
                }
            }

            AddictionDrugProfile profile = new AddictionDrugProfile(
                    key,
                    addictive,
                    pointsPerUse,
                    addictedAtPoints,
                    withdrawalAfterSeconds,
                    decayEnabled,
                    decayPerMinute,
                    withdrawalEffects
            );

            drugProfiles.put(key.toLowerCase(), profile);
        }
    }

    public static AddictionSettings getSettings() {
        return settings;
    }

    public static AddictionDrugProfile getDrugProfile(String drugId) {
        if (drugId == null) return null;
        return drugProfiles.get(drugId.toLowerCase());
    }

    public static Map<String, AddictionDrugProfile> getDrugProfiles() {
        return Collections.unmodifiableMap(drugProfiles);
    }

    public static CureProfile getCureProfile(String cureId) {
        if (cureId == null) return null;
        return cureProfiles.get(cureId.toLowerCase());
    }

    public static Map<String, CureProfile> getCureProfiles() {
        return Collections.unmodifiableMap(cureProfiles);
    }
}
