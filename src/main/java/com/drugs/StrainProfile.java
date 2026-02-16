package com.drugs;

import java.util.Collections;
import java.util.Map;

public class StrainProfile {
    private final String id;
    private final String displayName;
    private final String rarity;
    private final double durationMultiplier;
    private final double amplifierMultiplier;
    private final double mutationChance;
    private final Map<String, Integer> mutationWeights;

    public StrainProfile(String id,
                         String displayName,
                         String rarity,
                         double durationMultiplier,
                         double amplifierMultiplier,
                         double mutationChance,
                         Map<String, Integer> mutationWeights) {
        this.id = id;
        this.displayName = displayName;
        this.rarity = rarity;
        this.durationMultiplier = durationMultiplier;
        this.amplifierMultiplier = amplifierMultiplier;
        this.mutationChance = mutationChance;
        this.mutationWeights = mutationWeights;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getRarity() { return rarity; }
    public double getDurationMultiplier() { return durationMultiplier; }
    public double getAmplifierMultiplier() { return amplifierMultiplier; }
    public double getMutationChance() { return mutationChance; }
    public Map<String, Integer> getMutationWeights() { return Collections.unmodifiableMap(mutationWeights); }
}
