package com.drugs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

        ConfigurationSection root = config.isConfigurationSection("strains")
                ? config.getConfigurationSection("strains")
                : config;
        if (root == null) {
            Bukkit.getLogger().warning("[DrugsV2] strains.yml is missing a valid root section");
            return;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) continue;

            Map<String, Integer> mutationWeights = new HashMap<>();
            ConfigurationSection mut = section.getConfigurationSection("mutationWeights");
            if (mut == null) {
                mut = section.getConfigurationSection("mutation-weights");
            }
            if (mut != null) {
                for (String child : mut.getKeys(false)) {
                    mutationWeights.put(child.toLowerCase(), Math.max(0, mut.getInt(child)));
                }
            }

            double durationMultiplier = section.getDouble("durationMultiplier",
                    section.getDouble("effect-modifiers.duration-multiplier", 1.0));
            double amplifierMultiplier = section.getDouble("amplifierMultiplier",
                    section.getDouble("effect-modifiers.amplifier-multiplier", 1.0));
            double mutationChance = section.getDouble("mutationChance",
                    section.getDouble("mutation-chance", 0.005));

            List<PotionEffect> strainEffects = parseStrainEffects(section);

            StrainProfile profile = new StrainProfile(
                    id.toLowerCase(),
                    section.getString("display-name", section.getString("displayName", id)),
                    section.getString("rarity", "common"),
                    durationMultiplier,
                    amplifierMultiplier,
                    mutationChance,
                    mutationWeights,
                    strainEffects
            );
            strains.put(id.toLowerCase(), profile);
        }
        Bukkit.getLogger().info("[DrugsV2] Loaded " + strains.size() + " cannabis strains");
    }

    private static List<PotionEffect> parseStrainEffects(ConfigurationSection section) {
        List<PotionEffect> effects = new ArrayList<>();

        List<Map<?, ?>> effectMaps = section.getMapList("effects");
        if (!effectMaps.isEmpty()) {
            for (Map<?, ?> effectMap : effectMaps) {
                Object typeRaw = effectMap.get("type");
                if (typeRaw == null) continue;

                PotionEffectType type = PotionEffectType.getByName(String.valueOf(typeRaw).toUpperCase(Locale.ROOT));
                if (type == null) continue;

                int duration = parseInt(effectMap.get("duration"), 200);
                int amplifier = parseInt(effectMap.get("amplifier"), 0);
                effects.add(new PotionEffect(type, Math.max(1, duration), Math.max(0, amplifier)));
            }
            return effects;
        }

        ConfigurationSection legacyEffects = section.getConfigurationSection("effects");
        if (legacyEffects != null) {
            return EffectUtils.parsePotionEffects(legacyEffects);
        }

        return effects;
    }

    private static int parseInt(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
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
