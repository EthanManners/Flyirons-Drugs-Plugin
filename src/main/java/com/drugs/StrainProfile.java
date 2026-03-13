package com.drugs;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.potion.PotionEffect;

public class StrainProfile {
    private final String id;
    private final String displayName;
    private final String rarity;
    private final double durationMultiplier;
    private final double amplifierMultiplier;
    private final double mutationChance;
    private final Map<String, Integer> mutationWeights;
    private final List<PotionEffect> effects;

    public StrainProfile(String id,
                         String displayName,
                         String rarity,
                         double durationMultiplier,
                         double amplifierMultiplier,
                         double mutationChance,
                         Map<String, Integer> mutationWeights,
                         List<PotionEffect> effects) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.durationMultiplier = durationMultiplier;
        this.amplifierMultiplier = amplifierMultiplier;
        this.mutationChance = mutationChance;
        this.mutationWeights = mutationWeights;
        this.effects = effects;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getRarity() { return rarity; }
    public double getDurationMultiplier() { return durationMultiplier; }
    public double getAmplifierMultiplier() { return amplifierMultiplier; }
    public double getMutationChance() { return mutationChance; }
    public Map<String, Integer> getMutationWeights() { return Collections.unmodifiableMap(mutationWeights); }
    public List<PotionEffect> getEffects() { return Collections.unmodifiableList(effects); }
}
