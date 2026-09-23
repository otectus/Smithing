package com.otectus.otessmithing.network.packet;

import com.otectus.otessmithing.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The server ended a session without a result (cancelled, walked away, tool changed...). */
public record CloseSessionPacket(int sessionId, Component reason) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeComponent(reason);
    }

    public static CloseSessionPacket decode(FriendlyByteBuf buf) {
        return new CloseSessionPacket(buf.readVarInt(), buf.readComponent());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.closeSession(this));
    }
}
