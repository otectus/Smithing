package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.minigame.SessionManager;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** One hammer strike: a sequence number, the normalised position, and the client session time. */
public record AnvilStrikePacket(int sessionId, int sequence, float x, float y, int clientMs) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(sequence);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeVarInt(Math.max(0, clientMs));
    }

    public static AnvilStrikePacket decode(FriendlyByteBuf buf) {
        return new AnvilStrikePacket(buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        long arrival = Util.getMillis();
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) SessionManager.anvilStrike(player, sessionId, sequence, x, y, clientMs, arrival);
        });
        context.setPacketHandled(true);
    }
}
