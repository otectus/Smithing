package com.otectus.immersivesmithing.network.packet;

import com.otectus.immersivesmithing.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Opens the Maker's Mark screen for the piece in the given inventory slot. The stack travels along for display
 * because the inventory update may arrive a tick later; the server re-validates everything on signing.
 */
public record OpenMakersMarkPacket(int slot, ItemStack piece) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeItem(piece);
    }

    public static OpenMakersMarkPacket decode(FriendlyByteBuf buf) {
        return new OpenMakersMarkPacket(buf.readVarInt(), buf.readItem());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openMakersMark(this));
    }
}
