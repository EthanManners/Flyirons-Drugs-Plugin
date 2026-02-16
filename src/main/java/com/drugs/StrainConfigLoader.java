package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StrainConfigLoader {
    private static final Map<String, StrainProfile> strains = new ConcurrentHashMap<>();
    private static final Set<String> CANNABIS_DRUGS = new HashSet<>(Arrays.asList("blunt", "joint", "edible", "cart"));
    private static final Random random = new Random();

    public static void load(File dataFolder) {
        File file = new File(dataFolder, "strains.yml");
        if (!file.exists()) {
            DrugsV2.getInstance().saveResource("strains.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        strains.clear();

        for (String id : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(id);
            if (section == null) continue;

            Map<String, Integer> mutationWeights = new HashMap<>();
            ConfigurationSection mut = section.getConfigurationSection("mutation-weights");
            if (mut != null) {
                for (String child : mut.getKeys(false)) {
                    mutationWeights.put(child.toLowerCase(), Math.max(0, mut.getInt(child)));
                }
            }

            StrainProfile profile = new StrainProfile(
                    id.toLowerCase(),
                    section.getString("display-name", id),
                    section.getString("rarity", "common"),
                    section.getDouble("effect-modifiers.duration-multiplier", 1.0),
                    section.getDouble("effect-modifiers.amplifier-multiplier", 1.0),
                    section.getDouble("mutation-chance", 0.005),
                    mutationWeights
            );
            strains.put(id.toLowerCase(), profile);
        }
        Bukkit.getLogger().info("[DrugsV2] Loaded " + strains.size() + " cannabis strains");
    }

    public static boolean isCannabisDrug(String drugId) {
        return drugId != null && CANNABIS_DRUGS.contains(drugId.toLowerCase());
    }

    public static StrainProfile getStrain(String id) {
        if (id == null) return strains.get(DrugItemMetadata.DEFAULT_STRAIN_ID);
        return strains.getOrDefault(id.toLowerCase(), strains.get(DrugItemMetadata.DEFAULT_STRAIN_ID));
    }

    public static Collection<StrainProfile> getAllStrains() {
        return new ArrayList<>(strains.values());
    }

    public static String rollChildStrain(String parentId) {
        StrainProfile parent = getStrain(parentId);
        if (parent == null) return DrugItemMetadata.DEFAULT_STRAIN_ID;

        if (random.nextDouble() > parent.getMutationChance()) {
            return parent.getId();
        }

        Map<String, Integer> weights = parent.getMutationWeights();
        if (weights.isEmpty()) return parent.getId();

        int total = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return parent.getId();

        int roll = random.nextInt(total) + 1;
        int current = 0;
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            current += e.getValue();
            if (roll <= current) return e.getKey();
        }

        return parent.getId();
    }
}
