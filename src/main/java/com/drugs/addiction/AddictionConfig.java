package com.drugs.addiction;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class AddictionConfig {

    public boolean enabled = true;
    public boolean milkCuresWithdrawal = false;
    public int infiniteDurationTicks = 99999;
    public int heartbeatTicks = 20;

    private final Map<String, DrugRule> drugs = new HashMap<>();
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

    public static final class DrugRule {
        public boolean addictive = false;
        public double pointsPerUse = 1.0;
        public double addictedAtPoints = 3.0;
        public int withdrawalAfterSeconds = 180;
        public boolean decayEnabled = true;
        public double decayPointsPerMinute = 0.0;
        public List<EffectSpec> withdrawalEffects = new ArrayList<>();
        public List<EffectSpec> addictedEffects = new ArrayList<>();
    }

    public static final class CureRule {
        public boolean enabled = true;
        public boolean itemEnabled = true;
        public Material material = Material.GOLDEN_APPLE;
        public String displayName;
        public List<String> lore = new ArrayList<>();
        public Set<String> cures = new HashSet<>();
        public boolean clearsPoints = false;
        public double reducePoints = 0.0;
        public int blockWithdrawalSeconds = 0;

        public boolean allowsDrug(String drugId) {
            if (cures.isEmpty()) return true;
            if (cures.contains("*")) return true;
            return cures.contains(drugId.toLowerCase(Locale.ROOT));
        }
    }

    public static final class EffectSpec {
        public PotionEffectType type;
        public int amplifier;

        public EffectSpec() {}

        public EffectSpec(PotionEffectType type, int amplifier) {
            this.type = type;
            this.amplifier = amplifier;
        }
    }
}
