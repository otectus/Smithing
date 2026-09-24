package com.otectus.immersivesmithing.minigame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Deterministic rules of the Forge minigame, shared by client and server. The server generates the seed; both
 * sides derive identical phases from it, and the server re-scores inputs itself.
 */
public final class ForgeMinigame {

    /** One forming phase: a zone on the 0..1 track and the marker's sweep speed (track lengths per second). */
    public record Phase(float center, float halfWidth, float speed, float acceleration) {}

    public static List<Phase> phases(ForgePattern pattern, long seed, int metalUnits) {
        Random random = new Random(seed);
        int count = pattern.phaseCount(metalUnits);
        List<Phase> phases = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            float center = 0.2F + random.nextFloat() * 0.6F;
            float speed = pattern.minSpeed() + random.nextFloat() * (pattern.maxSpeed() - pattern.minSpeed());
            // Later phases sweep a little faster, so the sequence builds.
            speed *= 1.0F + 0.06F * i;
            phases.add(new Phase(center, pattern.halfWidth(), speed, pattern.acceleration()));
        }
        return List.copyOf(phases);
    }

    /** Marker position in [0,1] at {@code seconds} into a phase: ping-pong travel whose speed rises linearly. */
    public static float position(Phase phase, float seconds) {
        if (seconds <= 0) return 0F;
        double distance = phase.speed() * (seconds + phase.acceleration() * seconds * seconds / 2.0);
        double m = distance % 2.0;
        return (float) (m <= 1.0 ? m : 2.0 - m);
    }

    /** 1.0 near the centre, falling to 0.25 at the zone edge; 0 outside the zone (a miss). */
    public static float accuracy(Phase phase, float position) {
        float distance = Math.abs(position - phase.center());
        if (distance > phase.halfWidth()) return 0F;
        float perfect = phase.halfWidth() * 0.2F;
        if (distance <= perfect) return 1F;
        return 1F - (distance - perfect) / (phase.halfWidth() - perfect) * 0.75F;
    }

    private ForgeMinigame() {}
}
