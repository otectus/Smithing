package com.otectus.otessmithing.quality;

import com.otectus.otessmithing.config.ServerConfig;
import net.minecraft.util.Mth;

/** Turns scores into stat multipliers. A score of 50 is the midpoint between the configured bounds. */
public final class QualityCalculator {

    public static double durabilityMultiplier(QualityData q) {
        if (q.faulty()) return ServerConfig.get(ServerConfig.FAULTY_DURABILITY_MULTIPLIER);
        return lerp(ServerConfig.get(ServerConfig.MINIMUM_DURABILITY_MULTIPLIER),
                ServerConfig.get(ServerConfig.MAXIMUM_DURABILITY_MULTIPLIER), q.forgeScore());
    }

    public static double efficacyMultiplier(QualityData q) {
        if (q.faulty()) return ServerConfig.get(ServerConfig.FAULTY_EFFICACY_MULTIPLIER);
        return lerp(ServerConfig.get(ServerConfig.MINIMUM_EFFICACY_MULTIPLIER),
                ServerConfig.get(ServerConfig.MAXIMUM_EFFICACY_MULTIPLIER), q.anvilScore());
    }

    /** Mining tools apply only part of the efficacy deviation to their attack damage. */
    public static double toolAttackMultiplier(double efficacy) {
        return 1.0 + (efficacy - 1.0) * ServerConfig.get(ServerConfig.TOOL_ATTACK_EFFICACY_FACTOR);
    }

    public static double lerp(double min, double max, int score) {
        return min + (max - min) * Mth.clamp(score, 0, 100) / 100.0;
    }

    private QualityCalculator() {}
}
