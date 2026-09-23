package com.otectus.otessmithing.minigame;

import org.jetbrains.annotations.Nullable;

/**
 * Progress through the anvil strikes. Every required strike must land; a target that expires counts as a 0
 * sample and reappears, so poor accuracy costs time and, eventually, the Faulty timeout.
 */
public final class AnvilRun {
    private final AnvilPattern pattern;
    private final long seed;
    private int index;
    private int attempt;
    private int spawnMs;
    private int lastStrikeMs = -100_000;
    private float sum;
    private int samples;

    public AnvilRun(AnvilPattern pattern, long seed) {
        this.pattern = pattern;
        this.seed = seed;
    }

    public record StrikeResult(boolean hit, float quality, int struckIndex) {}

    /** Applies expiries up to {@code timeMs}: each expired target is a 0 sample and respawns after the gap. */
    public void advanceTo(int timeMs) {
        while (!isComplete() && timeMs > spawnMs + pattern.lifetimeMs()) {
            samples++;
            attempt++;
            spawnMs = spawnMs + pattern.lifetimeMs() + pattern.gapMs();
        }
    }

    /** Resolves a strike. Returns null when the strike is ignored (between targets, or too soon after the last). */
    @Nullable
    public StrikeResult strike(int timeMs, float x, float y) {
        advanceTo(timeMs);
        if (isComplete() || timeMs < spawnMs) return null;
        if (timeMs - lastStrikeMs < AnvilMinigame.MIN_STRIKE_INTERVAL_MS) return null;
        lastStrikeMs = timeMs;
        float quality = AnvilMinigame.strike(pattern, currentTarget(), x, y, timeMs - spawnMs);
        samples++;
        if (quality < 0) return new StrikeResult(false, 0F, index);
        sum += quality;
        int struck = index;
        index++;
        attempt = 0;
        spawnMs = timeMs + pattern.gapMs();
        return new StrikeResult(true, quality, struck);
    }

    public AnvilMinigame.Target currentTarget() {
        return AnvilMinigame.target(pattern, seed, index, attempt);
    }

    /** Overwrites local state with the server's authoritative view. */
    public void correct(int index, int attempt, int spawnMs, float sum, int samples) {
        this.index = index;
        this.attempt = attempt;
        this.spawnMs = spawnMs;
        this.sum = sum;
        this.samples = samples;
    }

    public boolean isComplete() {
        return index >= pattern.strikes();
    }

    public int index() { return index; }
    public int attempt() { return attempt; }
    public int spawnMs() { return spawnMs; }
    public float sum() { return sum; }
    public int samples() { return samples; }
    public AnvilPattern pattern() { return pattern; }

    public int score() {
        return samples == 0 ? 0 : Math.round(sum / samples * 100F);
    }
}
