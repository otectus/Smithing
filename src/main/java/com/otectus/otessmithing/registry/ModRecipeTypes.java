package com.otectus.otessmithing.registry;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.recipe.SmithingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, OtesSmithing.MOD_ID);

    public static final RegistryObject<RecipeType<SmithingRecipe>> SMITHING = RECIPE_TYPES.register("smithing",
            () -> RecipeType.simple(OtesSmithing.id("smithing")));

    private ModRecipeTypes() {}
}
