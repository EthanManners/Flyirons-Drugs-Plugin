package com.drugs.addiction;

import java.util.*;

public final class AddictionConfig {

    // How often we tick withdrawal/decay checks
    public int tickIntervalSeconds = 2;

    // If true, milk clears withdrawal effects (you said you want this toggleable)
    public boolean milkCuresWithdrawal = false;

    // Rules by drugId (e.g. "fent", "meth", "molly")
    private final Map<String, DrugRule> drugs = new HashMap<>();

    // Cure items by cureId (e.g. "suboxone")
    private final Map<String, CureRule> cures = new HashMap<>();

    public Map<String, DrugRule> getDrugs() {
        return drugs;
    }

    public Map<String, CureRule> getCures() {
        return cures;
    }

    public DrugRule getDrugRule(String drugId) {
        if (drugId == null) return null;
        return drugs.get(drugId.toLowerCase(Locale.ROOT));
    }

    public CureRule getCureRule(String cureId) {
        if (cureId == null) return null;
        return cures.get(cureId.toLowerCase(Locale.ROOT));
    }

    public void putDrugRule(String drugId, DrugRule rule) {
        if (drugId == null || rule == null) return;
        drugs.put(drugId.toLowerCase(Locale.ROOT), rule);
    }

    public void putCureRule(String cureId, CureRule rule) {
        if (cureId == null || rule == null) return;
        cures.put(cureId.toLowerCase(Locale.ROOT), rule);
    }

    // -------------------------
    // Nested config objects
    // -------------------------

    public static final class DrugRule {
        public boolean addictive = false;

        // Points gained per use
        public double pointsPerUse = 1.0;

        // Points required to be considered "addicted"
        public double addictedThreshold = 3.0;

        // Seconds after last dose before withdrawal begins
        public int withdrawalAfterSeconds = 180;

        // If true, points decay over time (optional per drug)
        public boolean decayEnabled = true;

        // Points removed per minute (or per tick interval, depending how we implement it)
        public double decayPerMinute = 0.0;

        // Withdrawal effects applied when addicted and timer elapsed
        public List<EffectSpec> withdrawalEffects = new ArrayList<>();

        // Duration for withdrawal effects (you said 99999 ticks)
        public int withdrawalEffectDurationTicks = 99999;
    }

    public static final class CureRule {
        // For now we can apply to all drugs, later we can restrict by tags/groups
        public boolean enabled = true;

        // Remove addiction points (per drug)
        public double removePoints = 0.0;

        // Block withdrawal for N seconds
        public int blockWithdrawalSeconds = 0;

        // Optional: only apply to these drug IDs (empty = all)
        public Set<String> onlyDrugs = new HashSet<>();
    }

    public static final class EffectSpec {
        // Bukkit uses PotionEffectType.getByName("POISON"), "HUNGER", etc.
        public String type;
        public int amplifier;

        public EffectSpec() {}

        public EffectSpec(String type, int amplifier) {
            this.type = type;
            this.amplifier = amplifier;
        }
    }
}
