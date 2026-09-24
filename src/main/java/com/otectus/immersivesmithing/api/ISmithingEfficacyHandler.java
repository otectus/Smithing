package com.otectus.immersivesmithing.api;

import com.otectus.immersivesmithing.quality.QualityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ItemAttributeModifierEvent;

/**
 * Replaces the default efficacy behaviour for items with unusual combat or mining logic. When a handler
 * claims a stack, the default attribute and mining-speed adjustments are skipped for it.
 */
public interface ISmithingEfficacyHandler {
    boolean handles(ItemStack stack);

    /** Add attribute differences for the given efficacy multiplier (1.0 = unchanged). */
    default void modifyAttributes(ItemAttributeModifierEvent event, QualityData quality, double efficacy) {}

    /** Return the adjusted mining speed. */
    default float modifyBreakSpeed(ItemStack stack, BlockState state, float speed, double efficacy) {
        return speed;
    }
}
