package com.otectus.immersivesmithing.event;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.api.ImmersiveSmithingAPI;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

/** Startup events (mod bus). */
@Mod.EventBusSubscriber(modid = ImmersiveSmithing.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    @SubscribeEvent
    public static void onProcessIMC(InterModProcessEvent event) {
        ImmersiveSmithingAPI.processIMC(event);
    }

    private ModEvents() {}
}
