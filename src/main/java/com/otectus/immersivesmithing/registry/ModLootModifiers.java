package com.otectus.immersivesmithing.registry;

import com.mojang.serialization.Codec;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.loot.ForgedLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ImmersiveSmithing.MOD_ID);

    public static final RegistryObject<Codec<ForgedLootModifier>> FORGED_LOOT = LOOT_MODIFIERS.register("forged_loot", () -> ForgedLootModifier.CODEC);

    private ModLootModifiers() {}
}
