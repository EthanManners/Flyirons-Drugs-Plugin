package com.drugs;

/**
 * Tracks addiction state for a specific player and drug.
 */
public class AddictionState {

    private double points;
    private long lastUseMillis;
    private long withdrawalBlockedUntilMillis;
    private long lastDecayMillis;
    private boolean withdrawalActive;

    public AddictionState(double points, long lastUseMillis, long withdrawalBlockedUntilMillis, long lastDecayMillis) {
        this.points = points;
        this.lastUseMillis = lastUseMillis;
        this.withdrawalBlockedUntilMillis = withdrawalBlockedUntilMillis;
        this.lastDecayMillis = lastDecayMillis;
        this.withdrawalActive = false;
    }

    public double getPoints() {
        return points;
    }

    public void setPoints(double points) {
        this.points = Math.max(0, points);
    }

    public long getLastUseMillis() {
        return lastUseMillis;
    }

    public void setLastUseMillis(long lastUseMillis) {
        this.lastUseMillis = lastUseMillis;
    }

    public long getWithdrawalBlockedUntilMillis() {
        return withdrawalBlockedUntilMillis;
    }

    public void setWithdrawalBlockedUntilMillis(long withdrawalBlockedUntilMillis) {
        this.withdrawalBlockedUntilMillis = withdrawalBlockedUntilMillis;
    }

    public long getLastDecayMillis() {
        return lastDecayMillis;
    }

    public void setLastDecayMillis(long lastDecayMillis) {
        this.lastDecayMillis = lastDecayMillis;
    }

    public boolean isWithdrawalActive() {
        return withdrawalActive;
    }

    public void setWithdrawalActive(boolean withdrawalActive) {
        this.withdrawalActive = withdrawalActive;
    }
}
