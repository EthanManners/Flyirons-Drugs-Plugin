package com.drugs;

import java.util.Collections;
import java.util.List;

/**
 * Configuration profile for a drug's addiction behavior.
 */
public class AddictionDrugProfile {

    private final String id;
    private final boolean addictive;
    private final double pointsPerUse;
    private final double addictedAtPoints;
    private final int withdrawalAfterSeconds;
    private final boolean decayEnabled;
    private final double decayPointsPerMinute;
    private final List<WithdrawalEffect> withdrawalEffects;

    public AddictionDrugProfile(String id,
                                boolean addictive,
                                double pointsPerUse,
                                double addictedAtPoints,
                                int withdrawalAfterSeconds,
                                boolean decayEnabled,
                                double decayPointsPerMinute,
                                List<WithdrawalEffect> withdrawalEffects) {
        this.id = id;
        this.addictive = addictive;
        this.pointsPerUse = pointsPerUse;
        this.addictedAtPoints = addictedAtPoints;
        this.withdrawalAfterSeconds = withdrawalAfterSeconds;
        this.decayEnabled = decayEnabled;
        this.decayPointsPerMinute = decayPointsPerMinute;
        this.withdrawalEffects = withdrawalEffects == null ? List.of() : List.copyOf(withdrawalEffects);
    }

    public String getId() {
        return id;
    }

    public boolean isAddictive() {
        return addictive;
    }

    public double getPointsPerUse() {
        return pointsPerUse;
    }

    public double getAddictedAtPoints() {
        return addictedAtPoints;
    }

    public int getWithdrawalAfterSeconds() {
        return withdrawalAfterSeconds;
    }

    public boolean isDecayEnabled() {
        return decayEnabled;
    }

    public double getDecayPointsPerMinute() {
        return decayPointsPerMinute;
    }

    public List<WithdrawalEffect> getWithdrawalEffects() {
        return Collections.unmodifiableList(withdrawalEffects);
    }
}
