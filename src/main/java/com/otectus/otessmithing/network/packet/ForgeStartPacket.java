package com.otectus.otessmithing.network.packet;

import com.otectus.otessmithing.client.ClientPacketHandler;
import com.otectus.otessmithing.minigame.ForgePattern;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Recipe accepted and inputs escrowed: the seed and rules from which the client derives the same phases. */
public record ForgeStartPacket(int sessionId, long seed, ForgePattern pattern, int metalUnits, int allowedMs, int readyDelayMs,
                               ItemStack result) {

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(sessionId);
        buf.writeLong(seed);
        pattern.write(buf);
        buf.writeVarInt(metalUnits);
        buf.writeVarInt(allowedMs);
        buf.writeVarInt(readyDelayMs);
        buf.writeItem(result);
    }

    public static ForgeStartPacket decode(FriendlyByteBuf buf) {
        return new ForgeStartPacket(buf.readVarInt(), buf.readLong(), ForgePattern.read(buf), buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readItem());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.forgeStart(this));
    }
}
