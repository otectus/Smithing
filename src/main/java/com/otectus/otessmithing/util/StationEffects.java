package com.otectus.otessmithing.util;

import com.otectus.otessmithing.network.ModNetwork;
import com.otectus.otessmithing.network.packet.StationEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.PacketDistributor;

/**
 * Visual effects are sent as small packets to players tracking the chunk and spawned client-side, so each
 * client can honour its own particle settings.
 */
public final class StationEffects {
    public static final int SPARKS = 0;
    public static final int STEAM = 1;
    public static final int FORGE_WORK = 2;
    public static final int GRIND = 3;
    public static final int MISS = 4;
    public static final int IGNITE = 5;

    public static void send(ServerLevel level, BlockPos pos, int type, float strength) {
        ModNetwork.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> level.getChunkAt(pos)),
                new StationEffectPacket(pos, type, strength));
    }

    private StationEffects() {}
}
