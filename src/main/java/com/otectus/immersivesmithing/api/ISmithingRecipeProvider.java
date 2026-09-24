package com.otectus.immersivesmithing.api;

import com.otectus.immersivesmithing.material.MaterialRegistry;
import com.otectus.immersivesmithing.recipe.SmithingRecipe;
import net.minecraft.core.RegistryAccess;

import java.util.Collection;

/**
 * Supplies smithing recipes from code on every reload. They rank with explicit datapack recipes, above
 * automatic detection, and are synced to clients like any other recipe.
 */
@FunctionalInterface
public interface ISmithingRecipeProvider {
    Collection<SmithingRecipe> recipes(MaterialRegistry materials, RegistryAccess access);
}
