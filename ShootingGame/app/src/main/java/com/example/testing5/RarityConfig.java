package com.example.testing5;

import java.util.Random;

public class RarityConfig {
    
    // Configurable drop rates (can be easily changed)
    public static final double COMMON_DROP_RATE = 0.50;    // 90%
    public static final double UNCOMMON_DROP_RATE = 0.30;  // 8%
    public static final double RARE_DROP_RATE = 0.20;     // 2%
    
    // Validate that rates sum to 1.0
    static {
        double total = COMMON_DROP_RATE + UNCOMMON_DROP_RATE + RARE_DROP_RATE;
        if (Math.abs(total - 1.0) > 0.001) {
            throw new IllegalStateException("Drop rates must sum to 1.0, current sum: " + total);
        }
    }
    
    /**
     * Determines skin rarity based on configurable drop rates
     * @param random Random number generator
     * @return Skin.Rarity based on drop rates
     */
    public static Skin.Rarity determineRarity(Random random) {
        double roll = random.nextDouble();
        
        if (roll < COMMON_DROP_RATE) {
            return Skin.Rarity.COMMON;
        } else if (roll < COMMON_DROP_RATE + UNCOMMON_DROP_RATE) {
            return Skin.Rarity.UNCOMMON;
        } else {
            return Skin.Rarity.RARE;
        }
    }
    
    /**
     * Get the drop rate for a specific rarity
     * @param rarity The rarity to get the drop rate for
     * @return Drop rate as a percentage (0.0 to 1.0)
     */
    public static double getDropRate(Skin.Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return COMMON_DROP_RATE;
            case UNCOMMON:
                return UNCOMMON_DROP_RATE;
            case RARE:
                return RARE_DROP_RATE;
            default:
                return 0.0;
        }
    }
    
    /**
     * Get drop rate as percentage string for display
     * @param rarity The rarity to get the drop rate for
     * @return Drop rate as percentage string (e.g., "90%")
     */
    public static String getDropRatePercentage(Skin.Rarity rarity) {
        return String.format("%.0f%%", getDropRate(rarity) * 100);
    }
}

