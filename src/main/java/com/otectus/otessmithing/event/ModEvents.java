package com.otectus.otessmithing.event;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.api.OtesSmithingAPI;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;

/** Startup events (mod bus). */
@Mod.EventBusSubscriber(modid = OtesSmithing.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    @SubscribeEvent
    public static void onProcessIMC(InterModProcessEvent event) {
        OtesSmithingAPI.processIMC(event);
    }

    private ModEvents() {}
}
