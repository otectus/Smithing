package com.otectus.otessmithing.registry;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.recipe.SmithingRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, OtesSmithing.MOD_ID);

    public static final RegistryObject<SmithingRecipeSerializer> SMITHING = RECIPE_SERIALIZERS.register("smithing",
            SmithingRecipeSerializer::new);

    private ModRecipeSerializers() {}
}
