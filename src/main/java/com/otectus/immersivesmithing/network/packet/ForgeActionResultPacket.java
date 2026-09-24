package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The server's authoritative accuracy for one forming phase. */
public record ForgeActionResultPacket(int sessionId, int phase, float accuracy) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(phase);
        buf.writeFloat(accuracy);
    }

    public static ForgeActionResultPacket decode(FriendlyByteBuf buf) {
        return new ForgeActionResultPacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.forgeActionResult(this));
    }
}
