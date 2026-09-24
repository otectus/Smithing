package com.otectus.immersivesmithing.minigame;

import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.recipe.SmithingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public final class ForgeSession extends SmithingSession {
    /** Offered recipe ids; a selection outside this list is rejected. */
    List<ResourceLocation> offered = List.of();
    SmithingRecipe recipe;
    ForgePattern pattern;
    ForgeRun run;

    ForgeSession(int id, UUID playerId, ResourceKey<Level> dimension, BlockPos pos, InteractionHand hand, SmithingTier tier,
                 long seed, long now) {
        super(id, playerId, dimension, pos, hand, tier, seed, now);
    }

    @Override
    public Kind kind() {
        return Kind.FORGE;
    }

    public SmithingRecipe recipe() {
        return recipe;
    }
}
