package com.otectus.otessmithing.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Presentation settings. None of these change scoring windows. */
public final class ClientConfig {
    public static final ForgeConfigSpec SPEC;

    public enum ParticleAmount { NORMAL, REDUCED, MINIMAL }

    public static final ForgeConfigSpec.BooleanValue SHOW_NUMERIC_QUALITY_SCORES;
    public static final ForgeConfigSpec.BooleanValue REDUCED_SCREEN_SHAKE;
    public static final ForgeConfigSpec.BooleanValue REDUCED_FLASHES;
    public static final ForgeConfigSpec.EnumValue<ParticleAmount> PARTICLE_AMOUNT;
    public static final ForgeConfigSpec.BooleanValue HIGH_CONTRAST_MINIGAMES;
    public static final ForgeConfigSpec.BooleanValue LARGE_MINIGAME_TARGETS;
    public static final ForgeConfigSpec.BooleanValue COLORBLIND_SAFE_TARGETS;
    public static final ForgeConfigSpec.BooleanValue TIMING_CUE_SOUNDS;
    public static final ForgeConfigSpec.DoubleValue SOUND_CUE_VOLUME;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("tooltips");
        SHOW_NUMERIC_QUALITY_SCORES = b.comment("Show raw 0-100 scores in the detailed (Shift) quality tooltip.")
                .define("showNumericQualityScores", false);
        b.pop();
        b.comment("Accessibility. These options only change presentation; they never widen scoring windows.").push("accessibility");
        REDUCED_SCREEN_SHAKE = b.define("reducedScreenShake", false);
        REDUCED_FLASHES = b.define("reducedFlashes", false);
        PARTICLE_AMOUNT = b.defineEnum("particleAmount", ParticleAmount.NORMAL);
        HIGH_CONTRAST_MINIGAMES = b.define("highContrastMinigames", false);
        LARGE_MINIGAME_TARGETS = b.comment("Draw minigame markers larger. The scoring area is unchanged.")
                .define("largeMinigameTargets", false);
        COLORBLIND_SAFE_TARGETS = b.comment("Use a blue/orange palette instead of green/red.")
                .define("colorblindSafeTargets", false);
        TIMING_CUE_SOUNDS = b.define("timingCueSounds", true);
        SOUND_CUE_VOLUME = b.defineInRange("soundCueVolume", 0.8, 0.0, 1.0);
        b.pop();
        SPEC = b.build();
    }

    public static boolean get(ForgeConfigSpec.BooleanValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static double get(ForgeConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static ParticleAmount particleAmount() {
        return SPEC.isLoaded() ? PARTICLE_AMOUNT.get() : PARTICLE_AMOUNT.getDefault();
    }

    /** Scales a particle count by the configured amount, keeping at least one when count > 0 unless minimal. */
    public static int scaleParticles(int count) {
        return switch (particleAmount()) {
            case NORMAL -> count;
            case REDUCED -> Math.max(count > 0 ? 1 : 0, count / 2);
            case MINIMAL -> count > 0 ? Math.min(1, count) : 0;
        };
    }

    private ClientConfig() {}
}
