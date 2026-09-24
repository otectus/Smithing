package com.otectus.immersivesmithing.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Maps an item to an anvil pattern id (for example {@code immersive_smithing:sword}), or null to defer. */
@FunctionalInterface
public interface IEquipmentClassifier {
    @Nullable
    ResourceLocation classify(ItemStack stack);
}
