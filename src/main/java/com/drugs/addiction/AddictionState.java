package com.drugs.addiction;

public class AddictionState {

    private double points;
    private long lastDoseMillis;

    // If a cure blocks withdrawal for X seconds, this is the "until" time.
    private long withdrawalBlockedUntilMillis = 0L;

    // ✅ Needed because AddictionManager currently does: new AddictionState()
    public AddictionState() {
        this(0.0, System.currentTimeMillis());
    }

    public AddictionState(double points, long lastDoseMillis) {
        this.points = points;
        this.lastDoseMillis = lastDoseMillis;
    }

    public double getPoints() {
        return points;
    }

    public void addPoints(double amount) {
        this.points += amount;
    }

    // Your original method name
    public void reducePoints(double amount) {
        this.points = Math.max(0, this.points - amount);
    }

    // ✅ Alias so AddictionManager's "removePoints" compiles
    public void removePoints(double amount) {
        reducePoints(amount);
    }

    public long getLastDoseMillis() {
        return lastDoseMillis;
    }

    public void updateLastDose() {
        this.lastDoseMillis = System.currentTimeMillis();
    }

    // ✅ Needed for cures (Suboxone etc.)
    public void blockWithdrawalForSeconds(int seconds) {
        long now = System.currentTimeMillis();
        long until = now + (seconds * 1000L);
        if (until > this.withdrawalBlockedUntilMillis) {
            this.withdrawalBlockedUntilMillis = until;
        }
    }

    public boolean isWithdrawalBlocked() {
        return System.currentTimeMillis() < withdrawalBlockedUntilMillis;
    }

    public long getWithdrawalBlockedUntilMillis() {
        return withdrawalBlockedUntilMillis;
    }
}
