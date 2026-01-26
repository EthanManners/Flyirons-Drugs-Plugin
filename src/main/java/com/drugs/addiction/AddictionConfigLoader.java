// src/main/java/com/drugs/addiction/AddictionConfigLoader.java
package com.drugs.addiction;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
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

        // If you want to ship a default addiction.yml inside resources, you can do:
        // plugin.saveResource("addiction.yml", false);
        // For now, we'll create a minimal file if missing.
        if (!file.exists()) {
            log.warning("[Addiction] addiction.yml not found, creating a starter file.");
            try {
                writeStarterFile(file);
            } catch (IOException e) {
                log.severe("[Addiction] Failed to create starter addiction.yml: " + e.getMessage());
            }
        }

        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        AddictionConfig cfg = new AddictionConfig();

        cfg.enabled = yml.getBoolean("enabled", true);
        cfg.milkCuresWithdrawal = yml.getBoolean("milk_cures_withdrawal", false);
        cfg.tickIntervalSeconds = Math.max(1, yml.getInt("tick_interval_seconds", 5));
        cfg.withdrawalEffectDurationTicks = Math.max(20, yml.getInt("withdrawal_effect_duration_ticks", 99999));

        // -----------------
        // Load cures
        // -----------------
        ConfigurationSection curesSec = yml.getConfigurationSection("cures");
        if (curesSec != null) {
            for (String cureId : curesSec.getKeys(false)) {
                ConfigurationSection c = curesSec.getConfigurationSection(cureId);
                if (c == null) continue;

                AddictionConfig.CureRule rule = new AddictionConfig.CureRule();
                rule.id = cureId.toLowerCase(Locale.ROOT);
                rule.material = c.getString("material", "GOLDEN_APPLE");
                rule.allowedDrugsRaw = c.getString("allowed_drugs", "*");
                rule.reducePoints = c.getDouble("reduce_points", 0.0);
                rule.blockWithdrawalSeconds = Math.max(0, c.getInt("block_withdrawal_seconds", 0));

                // Validate material
                if (Material.matchMaterial(rule.material) == null) {
                    log.warning("[Addiction] Cure '" + cureId + "' has invalid material '" + rule.material + "'.");
                }

                // allowed drugs
                rule.allowedDrugs.clear();
                if (!"*".equals(rule.allowedDrugsRaw)) {
                    if (c.isList("allowed_drugs")) {
                        for (Object o : c.getList("allowed_drugs")) {
                            if (o == null) continue;
                            rule.allowedDrugs.add(String.valueOf(o).toLowerCase(Locale.ROOT));
                        }
                    } else {
                        // If someone typed a string but not "*", treat as single id
                        rule.allowedDrugs.add(rule.allowedDrugsRaw.toLowerCase(Locale.ROOT));
                    }
                }

                cfg.cures.put(rule.id, rule);
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

                AddictionConfig.DrugAddictionRule rule = new AddictionConfig.DrugAddictionRule();
                rule.id = drugId.toLowerCase(Locale.ROOT);

                rule.pointsPerUse = d.getDouble("points_per_use", 0.0);
                rule.addictedThreshold = d.getDouble("addicted_threshold", 1.0);
                rule.withdrawalAfterSeconds = Math.max(0, d.getInt("withdrawal_after_seconds", 60));

                rule.decayEnabled = d.getBoolean("decay_enabled", false);
                rule.decayPointsPerMinute = d.getDouble("decay_points_per_minute", 0.0);

                // withdrawal_effect
                ConfigurationSection we = d.getConfigurationSection("withdrawal_effect");
                if (we != null) {
                    String typeName = we.getString("type", "POISON");
                    PotionEffectType type = PotionEffectType.getByName(typeName);
                    if (type == null) {
                        log.warning("[Addiction] Drug '" + drugId + "' has invalid withdrawal_effect.type '" + typeName + "'.");
                    }
                    rule.withdrawalEffect.type = type;
                    rule.withdrawalEffect.amplifier = Math.max(0, we.getInt("amplifier", 0));
                } else {
                    // Not required, but you *want* something defined.
                    rule.withdrawalEffect.type = PotionEffectType.POISON;
                    rule.withdrawalEffect.amplifier = 0;
                }

                // cures list
                rule.cures.clear();
                if (d.isList("cures")) {
                    for (Object o : d.getList("cures")) {
                        if (o == null) continue;
                        rule.cures.add(String.valueOf(o).toLowerCase(Locale.ROOT));
                    }
                }

                cfg.drugs.put(rule.id, rule);
            }
        }

        // Cross-validate cures referenced by drugs
        for (AddictionConfig.DrugAddictionRule d : cfg.drugs.values()) {
            for (String cureId : d.cures) {
                if (!cfg.cures.containsKey(cureId)) {
                    log.warning("[Addiction] Drug '" + d.id + "' references unknown cure '" + cureId + "'.");
                }
            }
            if (d.withdrawalEffect.type == null) {
                log.warning("[Addiction] Drug '" + d.id + "' withdrawal_effect.type is null (invalid effect name?).");
            }
        }

        log.info("[Addiction] Loaded addiction.yml: enabled=" + cfg.enabled +
                ", drugs=" + cfg.drugs.size() +
                ", cures=" + cfg.cures.size());

        return cfg;
    }

    private static void writeStarterFile(File file) throws IOException {
        String starter =
                "enabled: true\n" +
                "milk_cures_withdrawal: false\n" +
                "tick_interval_seconds: 5\n" +
                "withdrawal_effect_duration_ticks: 99999\n" +
                "\n" +
                "cures:\n" +
                "  suboxone:\n" +
                "    material: GOLDEN_APPLE\n" +
                "    allowed_drugs: \"*\"\n" +
                "    reduce_points: 0\n" +
                "    block_withdrawal_seconds: 240\n" +
                "\n" +
                "drugs:\n" +
                "  fent:\n" +
                "    points_per_use: 1\n" +
                "    addicted_threshold: 1\n" +
                "    withdrawal_after_seconds: 60\n" +
                "    decay_enabled: false\n" +
                "    withdrawal_effect:\n" +
                "      type: POISON\n" +
                "      amplifier: 0\n" +
                "    cures: [suboxone]\n";

        java.nio.file.Files.writeString(file.toPath(), starter);
    }
}
