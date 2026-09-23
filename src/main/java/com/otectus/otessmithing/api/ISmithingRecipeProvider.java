package com.otectus.otessmithing.api;

import com.otectus.otessmithing.material.MaterialRegistry;
import com.otectus.otessmithing.recipe.SmithingRecipe;
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
