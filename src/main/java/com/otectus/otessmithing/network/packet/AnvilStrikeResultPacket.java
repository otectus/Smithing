package com.otectus.otessmithing.network.packet;

import com.otectus.otessmithing.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The server's verdict on one strike, plus its authoritative run state for correcting the client. */
public record AnvilStrikeResultPacket(int sessionId, int sequence, boolean ignored, boolean hit, float quality,
                                      int index, int attempt, int spawnMs, float sum, int samples) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(sequence);
        buf.writeBoolean(ignored);
        buf.writeBoolean(hit);
        buf.writeFloat(quality);
        buf.writeVarInt(index);
        buf.writeVarInt(attempt);
        buf.writeVarInt(spawnMs);
        buf.writeFloat(sum);
        buf.writeVarInt(samples);
    }

    public static AnvilStrikeResultPacket decode(FriendlyByteBuf buf) {
        return new AnvilStrikeResultPacket(buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(),
                buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.anvilStrikeResult(this));
    }
}
