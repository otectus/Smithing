package com.otectus.otessmithing.api;

import com.otectus.otessmithing.material.RecyclingValue;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Consulted for equipment whose metal content no recipe or datapack override describes. */
@FunctionalInterface
public interface IRecyclingValueProvider {
    Optional<RecyclingValue> recyclingValue(ItemStack stack);
}
