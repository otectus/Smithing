package com.otectus.immersivesmithing.api;

import com.otectus.immersivesmithing.material.RecyclingValue;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Consulted for equipment whose metal content no recipe or datapack override describes. */
@FunctionalInterface
public interface IRecyclingValueProvider {
    Optional<RecyclingValue> recyclingValue(ItemStack stack);
}
