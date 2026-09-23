package com.otectus.otessmithing.api;

import com.otectus.otessmithing.quality.QualityData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;

/**
 * Shields are efficacy-neutral by default: quality only changes their durability. Shield mods that expose a
 * meaningful stat can apply efficacy through this hook.
 */
public interface IShieldSmithingHandler {
    boolean handles(ItemStack stack);

    void applyShieldEfficacy(ItemAttributeModifierEvent event, QualityData quality, double efficacy);
}
