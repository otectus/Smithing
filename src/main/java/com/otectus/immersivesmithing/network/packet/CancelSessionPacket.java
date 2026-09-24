package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.minigame.SessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The smith closed the screen before a result: refund and release, never mark Faulty. */
public record CancelSessionPacket(int sessionId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
    }

    public static CancelSessionPacket decode(FriendlyByteBuf buf) {
        return new CancelSessionPacket(buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) SessionManager.cancelFromClient(player, sessionId);
        });
        context.setPacketHandled(true);
    }
}
