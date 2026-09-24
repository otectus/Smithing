package com.otectus.immersivesmithing.network;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.network.packet.AnvilStrikePacket;
import com.otectus.immersivesmithing.network.packet.AnvilStrikeResultPacket;
import com.otectus.immersivesmithing.network.packet.CancelSessionPacket;
import com.otectus.immersivesmithing.network.packet.CloseSessionPacket;
import com.otectus.immersivesmithing.network.packet.ForgeActionPacket;
import com.otectus.immersivesmithing.network.packet.ForgeActionResultPacket;
import com.otectus.immersivesmithing.network.packet.ForgeStartPacket;
import com.otectus.immersivesmithing.network.packet.OpenAnvilScreenPacket;
import com.otectus.immersivesmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.immersivesmithing.network.packet.SelectForgeRecipePacket;
import com.otectus.immersivesmithing.network.packet.SessionResultPacket;
import com.otectus.immersivesmithing.network.packet.StationEffectPacket;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Small, purpose-specific packets. Clients report inputs only; the server owns every outcome. */
public final class ModNetwork {
    private static final String VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(ImmersiveSmithing.id("main"),
            () -> VERSION, VERSION::equals, VERSION::equals);

    public static void register() {
        int id = 0;
        // Server -> client
        CHANNEL.messageBuilder(OpenForgeScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenForgeScreenPacket::encode).decoder(OpenForgeScreenPacket::decode)
                .consumerMainThread(OpenForgeScreenPacket::handle).add();
        CHANNEL.messageBuilder(ForgeStartPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ForgeStartPacket::encode).decoder(ForgeStartPacket::decode)
                .consumerMainThread(ForgeStartPacket::handle).add();
        CHANNEL.messageBuilder(ForgeActionResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ForgeActionResultPacket::encode).decoder(ForgeActionResultPacket::decode)
                .consumerMainThread(ForgeActionResultPacket::handle).add();
        CHANNEL.messageBuilder(OpenAnvilScreenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenAnvilScreenPacket::encode).decoder(OpenAnvilScreenPacket::decode)
                .consumerMainThread(OpenAnvilScreenPacket::handle).add();
        CHANNEL.messageBuilder(AnvilStrikeResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(AnvilStrikeResultPacket::encode).decoder(AnvilStrikeResultPacket::decode)
                .consumerMainThread(AnvilStrikeResultPacket::handle).add();
        CHANNEL.messageBuilder(SessionResultPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SessionResultPacket::encode).decoder(SessionResultPacket::decode)
                .consumerMainThread(SessionResultPacket::handle).add();
        CHANNEL.messageBuilder(CloseSessionPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CloseSessionPacket::encode).decoder(CloseSessionPacket::decode)
                .consumerMainThread(CloseSessionPacket::handle).add();
        CHANNEL.messageBuilder(StationEffectPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(StationEffectPacket::encode).decoder(StationEffectPacket::decode)
                .consumerMainThread(StationEffectPacket::handle).add();
        // Client -> server
        CHANNEL.messageBuilder(SelectForgeRecipePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectForgeRecipePacket::encode).decoder(SelectForgeRecipePacket::decode)
                .consumerNetworkThread(SelectForgeRecipePacket::handle).add();
        CHANNEL.messageBuilder(ForgeActionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ForgeActionPacket::encode).decoder(ForgeActionPacket::decode)
                .consumerNetworkThread(ForgeActionPacket::handle).add();
        CHANNEL.messageBuilder(AnvilStrikePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(AnvilStrikePacket::encode).decoder(AnvilStrikePacket::decode)
                .consumerNetworkThread(AnvilStrikePacket::handle).add();
        CHANNEL.messageBuilder(CancelSessionPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CancelSessionPacket::encode).decoder(CancelSessionPacket::decode)
                .consumerNetworkThread(CancelSessionPacket::handle).add();
    }

    private ModNetwork() {}
}
