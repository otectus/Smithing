package com.otectus.immersivesmithing.minigame;

import com.otectus.immersivesmithing.item.SmithingTier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import java.util.UUID;

public final class AnvilSession extends SmithingSession {
    AnvilPattern pattern;
    AnvilRun run;
    int lastSequence = -1;

    AnvilSession(int id, UUID playerId, ResourceKey<Level> dimension, BlockPos pos, InteractionHand hand, SmithingTier tier,
                 long seed, long now) {
        super(id, playerId, dimension, pos, hand, tier, seed, now);
    }

    @Override
    public Kind kind() {
        return Kind.ANVIL;
    }
}
