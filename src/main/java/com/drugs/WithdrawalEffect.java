package com.drugs;

import org.bukkit.potion.PotionEffectType;

/**
 * Represents a withdrawal potion effect definition.
 */
public class WithdrawalEffect {

    private final PotionEffectType type;
    private final int amplifier;

    public WithdrawalEffect(PotionEffectType type, int amplifier) {
        this.type = type;
        this.amplifier = amplifier;
    }

    public PotionEffectType getType() {
        return type;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public static WithdrawalEffect from(String typeName, int amplifier) {
        PotionEffectType type = PotionEffectType.getByName(typeName.toUpperCase());
        if (type == null) return null;
        return new WithdrawalEffect(type, amplifier);
    }
}
