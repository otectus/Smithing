package com.otectus.otessmithing.network.packet;

import com.otectus.otessmithing.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/** Opens the Forge screen in its recipe-selection phase with the recipes the server will accept. */
public record OpenForgeScreenPacket(int sessionId, BlockPos pos, Component familyName, int moltenUnits, int timeMs,
                                    List<Entry> entries) {

    public record Entry(ResourceLocation recipeId, ItemStack result, int metalUnits, List<ItemStack> auxiliary) {}

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeBlockPos(pos);
        buf.writeComponent(familyName);
        buf.writeVarInt(moltenUnits);
        buf.writeVarInt(timeMs);
        buf.writeCollection(entries, (b, e) -> {
            b.writeResourceLocation(e.recipeId());
            b.writeItem(e.result());
            b.writeVarInt(e.metalUnits());
            b.writeCollection(e.auxiliary(), FriendlyByteBuf::writeItem);
        });
    }

    public static OpenForgeScreenPacket decode(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        BlockPos pos = buf.readBlockPos();
        Component name = buf.readComponent();
        int molten = buf.readVarInt();
        int time = buf.readVarInt();
        List<Entry> entries = buf.readList(b -> new Entry(b.readResourceLocation(), b.readItem(), b.readVarInt(),
                b.readList(FriendlyByteBuf::readItem)));
        return new OpenForgeScreenPacket(id, pos, name, molten, time, entries);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openForge(this));
    }
}
