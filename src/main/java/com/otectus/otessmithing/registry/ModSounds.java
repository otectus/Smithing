package com.otectus.otessmithing.registry;

import com.otectus.otessmithing.OtesSmithing;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OtesSmithing.MOD_ID);

    public static final RegistryObject<SoundEvent> FORGE_IGNITE = register("block.smiths_forge.ignite");
    public static final RegistryObject<SoundEvent> FORGE_ROAR = register("block.smiths_forge.roar");
    public static final RegistryObject<SoundEvent> FORGE_READY = register("block.smiths_forge.ready");
    public static final RegistryObject<SoundEvent> FORGE_DEPOSIT = register("block.smiths_forge.deposit");
    public static final RegistryObject<SoundEvent> FORGE_FUEL = register("block.smiths_forge.fuel");
    public static final RegistryObject<SoundEvent> FORGE_WORK = register("block.smiths_forge.work");
    public static final RegistryObject<SoundEvent> TONGS_GRAB = register("item.smithing_tongs.grab");
    public static final RegistryObject<SoundEvent> ANVIL_STRIKE = register("block.smiths_anvil.strike");
    public static final RegistryObject<SoundEvent> ANVIL_STRIKE_PERFECT = register("block.smiths_anvil.strike_perfect");
    public static final RegistryObject<SoundEvent> ANVIL_MISS = register("block.smiths_anvil.miss");
    public static final RegistryObject<SoundEvent> ANVIL_FINISH = register("block.smiths_anvil.finish");
    public static final RegistryObject<SoundEvent> QUENCH = register("block.smiths_trough.quench");
    public static final RegistryObject<SoundEvent> TROUGH_FILL = register("block.smiths_trough.fill");
    public static final RegistryObject<SoundEvent> GRINDSTONE_REFINE = register("block.smiths_grindstone.refine");
    public static final RegistryObject<SoundEvent> ACTION_FAIL = register("ui.smithing.fail");
    public static final RegistryObject<SoundEvent> TIMING_CUE = register("ui.smithing.timing_cue");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(OtesSmithing.id(name)));
    }

    private ModSounds() {}
}
