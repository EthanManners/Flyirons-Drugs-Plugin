package com.drugs;

/**
 * Settings for the addiction system.
 */
public class AddictionSettings {

    private final boolean enabled;
    private final boolean milkClearsWithdrawal;
    private final int infiniteDurationTicks;
    private final int heartbeatTicks;

    public AddictionSettings() {
        this(true, false, 99999, 20);
    }

    public AddictionSettings(boolean enabled, boolean milkClearsWithdrawal, int infiniteDurationTicks, int heartbeatTicks) {
        this.enabled = enabled;
        this.milkClearsWithdrawal = milkClearsWithdrawal;
        this.infiniteDurationTicks = infiniteDurationTicks;
        this.heartbeatTicks = heartbeatTicks;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isMilkClearsWithdrawal() {
        return milkClearsWithdrawal;
    }

    public int getInfiniteDurationTicks() {
        return infiniteDurationTicks;
    }

    public int getHeartbeatTicks() {
        return heartbeatTicks;
    }
}
