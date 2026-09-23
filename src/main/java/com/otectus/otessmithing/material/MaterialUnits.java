package com.otectus.otessmithing.material;

import net.minecraft.network.chat.Component;

/** Integer material accounting: nugget 1, ingot 9, block 81. */
public final class MaterialUnits {
    public static final int NUGGET = 1;
    public static final int INGOT = 9;
    public static final int BLOCK = 81;

    /** "2 ingots", "2 ingots + 4 nuggets", "5 nuggets". */
    public static Component describe(int units) {
        int ingots = units / INGOT;
        int nuggets = units % INGOT;
        if (ingots > 0 && nuggets > 0) {
            return Component.translatable("units.otes_smithing.ingots_nuggets", ingots, nuggets);
        }
        if (ingots > 0) {
            return Component.translatable(ingots == 1 ? "units.otes_smithing.ingot" : "units.otes_smithing.ingots", ingots);
        }
        return Component.translatable(nuggets == 1 ? "units.otes_smithing.nugget" : "units.otes_smithing.nuggets", nuggets);
    }

    private MaterialUnits() {}
}
