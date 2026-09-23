package com.otectus.otessmithing.packtest;

import net.minecraftforge.fml.common.Mod;

/**
 * Test-only companion mod used by tools/packtest (full-modpack runs) and by runClient -PsmokeTest (dev). It does
 * nothing unless the system property {@code otes_smithing.packtest.output} names a directory for its results.
 */
@Mod(PackTestAgent.MOD_ID)
public class PackTestAgent {
    public static final String MOD_ID = "otes_smithing_packtest";
    public static final String OUTPUT_PROPERTY = "otes_smithing.packtest.output";

    public PackTestAgent() {
    }
}
