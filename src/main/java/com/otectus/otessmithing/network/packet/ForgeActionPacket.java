package com.otectus.otessmithing.network.packet;

import com.otectus.otessmithing.minigame.SessionManager;
import net.minecraft.Util;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * One forming input: which phase, and when (ms on the client's session clock). The arrival time is captured on
 * the network thread, before the tick queue adds up to 50 ms of delay.
 */
public record ForgeActionPacket(int sessionId, int phase, int clientMs) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeVarInt(phase);
        buf.writeVarInt(Math.max(0, clientMs));
    }

    public static ForgeActionPacket decode(FriendlyByteBuf buf) {
        return new ForgeActionPacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        long arrival = Util.getMillis();
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) SessionManager.forgeAction(player, sessionId, phase, clientMs, arrival);
        });
        context.setPacketHandled(true);
    }
}
