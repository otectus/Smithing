package com.otectus.immersivesmithing.client;

import com.otectus.immersivesmithing.client.screen.GuideScreen;
import net.minecraft.client.Minecraft;

/** Entry points called from common code through DistExecutor. */
public final class ClientHooks {

    public static void openGuide(int chapter) {
        Minecraft.getInstance().setScreen(new GuideScreen(chapter));
    }

    private ClientHooks() {}
}
