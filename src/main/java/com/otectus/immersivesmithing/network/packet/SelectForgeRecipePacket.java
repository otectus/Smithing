package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.minigame.SessionManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The recipe the smith picked. Only ids the server offered for this session are accepted. */
public record SelectForgeRecipePacket(int sessionId, ResourceLocation recipeId) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeResourceLocation(recipeId);
    }

    public static SelectForgeRecipePacket decode(FriendlyByteBuf buf) {
        return new SelectForgeRecipePacket(buf.readVarInt(), buf.readResourceLocation());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer player = context.getSender();
        context.enqueueWork(() -> {
            if (player != null) SessionManager.selectRecipe(player, sessionId, recipeId);
        });
        context.setPacketHandled(true);
    }
}
