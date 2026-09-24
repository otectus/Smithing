package com.otectus.immersivesmithing.minigame;

import net.minecraft.util.Mth;

import java.util.Random;

/** Deterministic rules of the Anvil minigame, shared by client and server. */
public final class AnvilMinigame {
    public static final int MIN_STRIKE_INTERVAL_MS = 80;

    public record Target(float x, float y) {}

    /** Target position for a strike index and respawn attempt, jittered by the session seed. */
    public static Target target(AnvilPattern pattern, long seed, int index, int attempt) {
        float[] p = pattern.point(index);
        Random random = new Random(seed * 31L + index * 1_000_003L + attempt * 7_919L);
        float jx = (random.nextFloat() * 2F - 1F) * pattern.jitter();
        float jy = (random.nextFloat() * 2F - 1F) * pattern.jitter();
        return new Target(Mth.clamp(p[0] + jx, 0.08F, 0.92F), Mth.clamp(p[1] + jy, 0.08F, 0.92F));
    }

    /** The moment within a target's lifetime at which a strike is perfectly timed. */
    public static int idealMs(AnvilPattern pattern) {
        return Math.round(pattern.lifetimeMs() * pattern.idealFraction());
    }

    /**
     * Strike quality in 0..1 combining position and timing accuracy equally, or -1 if the strike lands outside
     * the target (a miss).
     */
    public static float strike(AnvilPattern pattern, Target target, float x, float y, int msIntoTarget) {
        float distance = (float) Math.hypot(x - target.x(), y - target.y());
        float radius = pattern.radius();
        if (distance > radius) return -1F;
        float positional = distance <= radius * 0.25F ? 1F : 1F - (distance - radius * 0.25F) / (radius * 0.75F) * 0.8F;
        float offset = Math.abs(msIntoTarget - idealMs(pattern));
        float timing = 1F - Mth.clamp((offset - 80F) / 700F, 0F, 1F);
        return 0.5F * positional + 0.5F * timing;
    }

    private AnvilMinigame() {}
}
