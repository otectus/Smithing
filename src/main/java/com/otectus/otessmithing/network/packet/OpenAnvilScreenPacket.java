package com.otectus.otessmithing.network.packet;

import com.otectus.otessmithing.client.ClientPacketHandler;
import com.otectus.otessmithing.minigame.AnvilPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Opens the Anvil minigame with the pattern and seed from which both sides derive the same targets. */
public record OpenAnvilScreenPacket(int sessionId, BlockPos pos, ItemStack target, AnvilPattern pattern, long seed,
                                    int allowedMs, int readyDelayMs) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeBlockPos(pos);
        buf.writeItem(target);
        pattern.write(buf);
        buf.writeLong(seed);
        buf.writeVarInt(allowedMs);
        buf.writeVarInt(readyDelayMs);
    }

    public static OpenAnvilScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenAnvilScreenPacket(buf.readVarInt(), buf.readBlockPos(), buf.readItem(), AnvilPattern.read(buf),
                buf.readLong(), buf.readVarInt(), buf.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.openAnvil(this));
    }
}
