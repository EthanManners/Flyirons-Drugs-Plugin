// src/main/java/com/drugs/addiction/AddictionConfigLoader.java
package com.drugs.addiction;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public final class AddictionConfigLoader {

    private AddictionConfigLoader() {}

    public static AddictionConfig load(JavaPlugin plugin) {
        Logger log = plugin.getLogger();

        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            log.warning("[Addiction] Could not create plugin data folder: " + dataFolder.getAbsolutePath());
        }

        File file = new File(dataFolder, "addiction.yml");
        if (!file.exists()) {
            log.warning("[Addiction] addiction.yml not found, saving default.");
            plugin.saveResource("addiction.yml", false);
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        AddictionConfig cfg = new AddictionConfig();

        ConfigurationSection settings = yml.getConfigurationSection("settings");
        if (settings == null) settings = yml;

        cfg.enabled = settings.getBoolean("enabled", true);
        cfg.milkCuresWithdrawal = settings.getBoolean(
                "milk_clears_withdrawal",
                settings.getBoolean("milk_cures_withdrawal", false)
        );
        cfg.infiniteDurationTicks = Math.max(20, settings.getInt("infinite_duration_ticks", 99999));
        cfg.heartbeatTicks = Math.max(1, settings.getInt("heartbeat_ticks", 20));

        // -----------------
        // Load cures
        // -----------------
        ConfigurationSection curesSec = yml.getConfigurationSection("cures");
        if (curesSec != null) {
            for (String cureId : curesSec.getKeys(false)) {
                ConfigurationSection c = curesSec.getConfigurationSection(cureId);
                if (c == null) continue;

                AddictionConfig.CureRule rule = new AddictionConfig.CureRule();
                String materialName = c.getString("material", "GOLDEN_APPLE");
                org.bukkit.Material material = org.bukkit.Material.matchMaterial(materialName);
                if (material == null) {
                    log.warning("[Addiction] Cure '" + cureId + "' has invalid material '" + materialName + "'.");
                    material = org.bukkit.Material.GOLDEN_APPLE;
                }
                rule.material = material;
                rule.displayName = c.getString("display-name", null);
                rule.lore = c.getStringList("lore");
                rule.enabled = c.getBoolean("enabled", true);
                rule.clearsPoints = c.getBoolean("clears_points", false);
                rule.reducePoints = c.getDouble("reduce_points", 0.0);
                rule.blockWithdrawalSeconds = Math.max(0, c.getInt("block_withdrawal_seconds", 0));

                rule.cures.clear();
                if (c.isList("cures")) {
                    for (Object o : c.getList("cures")) {
                        if (o == null) continue;
                        rule.cures.add(String.valueOf(o).toLowerCase(Locale.ROOT));
                    }
                } else if (c.isString("cures")) {
                    rule.cures.add(c.getString("cures", "*").toLowerCase(Locale.ROOT));
                }

                cfg.putCureRule(cureId, rule);
            }
        }

        // -----------------
        // Load drugs
        // -----------------
        ConfigurationSection drugsSec = yml.getConfigurationSection("drugs");
        if (drugsSec == null) {
            log.warning("[Addiction] No 'drugs:' section found in addiction.yml.");
        } else {
            for (String drugId : drugsSec.getKeys(false)) {
                ConfigurationSection d = drugsSec.getConfigurationSection(drugId);
                if (d == null) continue;

                AddictionConfig.DrugRule rule = new AddictionConfig.DrugRule();

                rule.addictive = d.getBoolean("addictive", false);
                rule.pointsPerUse = d.getDouble("points_per_use", 0.0);
                rule.addictedAtPoints = d.getDouble("addicted_at_points", 1.0);
                rule.withdrawalAfterSeconds = Math.max(0, d.getInt("withdrawal_after_seconds", 60));

                ConfigurationSection decay = d.getConfigurationSection("decay");
                if (decay != null) {
                    rule.decayEnabled = decay.getBoolean("enabled", false);
                    rule.decayPointsPerMinute = decay.getDouble("points_decay_per_minute", 0.0);
                } else {
                    rule.decayEnabled = d.getBoolean("decay_enabled", false);
                    rule.decayPointsPerMinute = d.getDouble("decay_points_per_minute", 0.0);
                }

                rule.withdrawalEffects = loadEffects(log, drugId, d, "withdrawal_effects");
                rule.addictedEffects = loadEffects(log, drugId, d, "addicted_effects");

                cfg.putDrugRule(drugId, rule);
            }
        }

        log.info("[Addiction] Loaded addiction.yml: enabled=" + cfg.enabled +
                ", drugs=" + cfg.getDrugs().size() +
                ", cures=" + cfg.getCures().size());

        return cfg;
    }

    private static List<AddictionConfig.EffectSpec> loadEffects(
            Logger log,
            String drugId,
            ConfigurationSection parent,
            String key
    ) {
        List<AddictionConfig.EffectSpec> effects = new ArrayList<>();
        if (parent == null || !parent.isList(key)) return effects;

        for (Object o : parent.getList(key)) {
            if (!(o instanceof Map<?, ?> raw)) continue;
            String typeName = String.valueOf(raw.get("type"));
            PotionEffectType type = PotionEffectType.getByName(typeName);
            if (type == null) {
                log.warning("[Addiction] Drug '" + drugId + "' has invalid effect type '" + typeName + "'.");
                continue;
            }
            int amplifier = 0;
            Object ampValue = raw.get("amplifier");
            if (ampValue instanceof Number number) {
                amplifier = Math.max(0, number.intValue());
            }
            effects.add(new AddictionConfig.EffectSpec(type, amplifier));
        }
        return effects;
    }
}
