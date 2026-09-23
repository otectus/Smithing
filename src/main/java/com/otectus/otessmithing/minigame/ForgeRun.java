package com.otectus.otessmithing.minigame;

import java.util.List;

/**
 * Progress through the Forge phases. Phase 0 starts at t = 0; each later phase starts {@code transitionMs}
 * after the previous input. Times are milliseconds on the session clock.
 */
public final class ForgeRun {
    private final List<ForgeMinigame.Phase> phases;
    private final int transitionMs;
    private final float[] results;
    private int index;
    private int phaseStartMs;
    private float total;

    public ForgeRun(List<ForgeMinigame.Phase> phases, int transitionMs) {
        this.phases = phases;
        this.transitionMs = transitionMs;
        this.results = new float[phases.size()];
    }

    /** Scores an input at {@code timeMs}. Returns the accuracy, or -1 if the input is ignored. */
    public float press(int timeMs) {
        if (isComplete()) return -1F;
        int local = timeMs - phaseStartMs;
        if (local < 0) return -1F;
        ForgeMinigame.Phase phase = phases.get(index);
        float accuracy = ForgeMinigame.accuracy(phase, ForgeMinigame.position(phase, local / 1000F));
        results[index] = accuracy;
        total += accuracy;
        index++;
        phaseStartMs = timeMs + transitionMs;
        return accuracy;
    }

    public boolean isComplete() {
        return index >= phases.size();
    }

    public int index() {
        return index;
    }

    public int phaseStartMs() {
        return phaseStartMs;
    }

    public List<ForgeMinigame.Phase> phases() {
        return phases;
    }

    public float result(int phase) {
        return results[phase];
    }

    /** 0..100, averaged over every phase (unplayed phases count as 0). */
    public int score() {
        return Math.round(total / phases.size() * 100F);
    }
}
