package com.otectus.immersivesmithing.workpiece;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * A hot, unfinished piece of equipment. It lives in the NBT of Smithing Tongs, on a Smith's Anvil, or in a
 * dropped Hot Workpiece item, and is only turned into the usable target stack by quenching.
 */
public record WorkpieceData(
        ItemStack target,
        ResourceLocation recipeId,
        ResourceLocation family,
        int metalUnits,
        int forgeScore,
        int anvilScore,
        boolean faulty,
        WorkpieceState state,
        ResourceLocation anvilPattern) {

    public WorkpieceData shaped(int score, boolean faulty) {
        return new WorkpieceData(target, recipeId, family, metalUnits, forgeScore, Math.max(0, Math.min(100, score)), faulty,
                WorkpieceState.SHAPED, anvilPattern);
    }

    public boolean isShaped() {
        return state == WorkpieceState.SHAPED;
    }
}
