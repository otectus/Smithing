package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.client.ClientPacketHandler;
import com.otectus.immersivesmithing.minigame.SmithingSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Final server-computed score. {@code timedOut} means Forge score 0, or a Faulty workpiece on the anvil. */
public record SessionResultPacket(int sessionId, SmithingSession.Kind kind, int score, boolean timedOut) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeEnum(kind);
        buf.writeVarInt(score);
        buf.writeBoolean(timedOut);
    }

    public static SessionResultPacket decode(FriendlyByteBuf buf) {
        return new SessionResultPacket(buf.readVarInt(), buf.readEnum(SmithingSession.Kind.class), buf.readVarInt(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.sessionResult(this));
    }
}
