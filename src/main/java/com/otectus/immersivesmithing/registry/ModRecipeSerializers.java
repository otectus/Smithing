package com.otectus.immersivesmithing.registry;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.recipe.SmithingRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ImmersiveSmithing.MOD_ID);

    public static final RegistryObject<SmithingRecipeSerializer> SMITHING = RECIPE_SERIALIZERS.register("smithing",
            SmithingRecipeSerializer::new);

    private ModRecipeSerializers() {}
}
