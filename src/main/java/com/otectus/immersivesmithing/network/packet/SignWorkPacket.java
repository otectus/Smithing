package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.quality.SigningService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The smith's title and inscription for a piece in their inventory. The server only accepts it for a piece the
 * sender forged, and sanitises every string itself; the client limits are a convenience.
 */
public record SignWorkPacket(int slot, String title, List<String> inscription) {
    private static final int WIRE_LIMIT = 256;

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeUtf(title, WIRE_LIMIT);
        buf.writeVarInt(Math.min(inscription.size(), 8));
        for (int i = 0; i < Math.min(inscription.size(), 8); i++) buf.writeUtf(inscription.get(i), WIRE_LIMIT);
    }

    public static SignWorkPacket decode(FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        String title = buf.readUtf(WIRE_LIMIT);
        int n = Math.min(buf.readVarInt(), 8);
        List<String> lines = new ArrayList<>(n);
        for (int i = 0; i < n; i++) lines.add(buf.readUtf(WIRE_LIMIT));
        return new SignWorkPacket(slot, title, lines);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) SigningService.sign(player, slot, title, inscription);
        });
        context.setPacketHandled(true);
    }
}
