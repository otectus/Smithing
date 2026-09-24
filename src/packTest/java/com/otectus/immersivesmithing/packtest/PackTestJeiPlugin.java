package com.otectus.immersivesmithing.packtest;

import com.otectus.immersivesmithing.client.compat.jei.SmithingRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Test-only JEI bridge that opens the real Immersive Smithing category for a framebuffer check. */
@JeiPlugin
public final class PackTestJeiPlugin implements IModPlugin {
    private static volatile IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(PackTestAgent.MOD_ID, "jei_bridge");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static IJeiRuntime runtime() {
        return runtime;
    }

    public static void openSmithingRecipes() {
        IJeiRuntime current = runtime;
        if (current != null) current.getRecipesGui().showTypes(List.of(SmithingRecipeCategory.TYPE));
    }
}
