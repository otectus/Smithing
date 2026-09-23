package com.otectus.otessmithing.client;

import com.otectus.otessmithing.client.screen.AnvilMinigameScreen;
import com.otectus.otessmithing.client.screen.ForgeMinigameScreen;
import com.otectus.otessmithing.network.packet.AnvilStrikeResultPacket;
import com.otectus.otessmithing.network.packet.CloseSessionPacket;
import com.otectus.otessmithing.network.packet.ForgeActionResultPacket;
import com.otectus.otessmithing.network.packet.ForgeStartPacket;
import com.otectus.otessmithing.network.packet.OpenAnvilScreenPacket;
import com.otectus.otessmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.otessmithing.network.packet.SessionResultPacket;
import com.otectus.otessmithing.network.packet.StationEffectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Client handlers for server packets. Runs on the client main thread. */
public final class ClientPacketHandler {

    public static void openForge(OpenForgeScreenPacket packet) {
        Minecraft.getInstance().setScreen(new ForgeMinigameScreen(packet));
    }

    public static void forgeStart(ForgeStartPacket packet) {
        if (Minecraft.getInstance().screen instanceof ForgeMinigameScreen screen) screen.onStart(packet);
    }

    public static void forgeActionResult(ForgeActionResultPacket packet) {
        if (Minecraft.getInstance().screen instanceof ForgeMinigameScreen screen) screen.onActionResult(packet);
    }

    public static void openAnvil(OpenAnvilScreenPacket packet) {
        Minecraft.getInstance().setScreen(new AnvilMinigameScreen(packet));
    }

    public static void anvilStrikeResult(AnvilStrikeResultPacket packet) {
        if (Minecraft.getInstance().screen instanceof AnvilMinigameScreen screen) screen.onStrikeResult(packet);
    }

    public static void sessionResult(SessionResultPacket packet) {
        Screen screen = Minecraft.getInstance().screen;
        if (screen instanceof ForgeMinigameScreen forge) forge.onResult(packet);
        else if (screen instanceof AnvilMinigameScreen anvil) anvil.onResult(packet);
    }

    public static void closeSession(CloseSessionPacket packet) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof ForgeMinigameScreen forge && forge.sessionId() == packet.sessionId()) forge.closeByServer();
        else if (mc.screen instanceof AnvilMinigameScreen anvil && anvil.sessionId() == packet.sessionId()) anvil.closeByServer();
        if (mc.player != null) mc.player.displayClientMessage(packet.reason(), true);
    }

    public static void stationEffect(StationEffectPacket packet) {
        ClientEffects.spawn(packet.pos(), packet.type(), packet.strength());
    }

    private ClientPacketHandler() {}
}
