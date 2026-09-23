package com.otectus.otessmithing.material;

import net.minecraft.resources.ResourceLocation;

/** The full metal content of a piece of equipment, before recycling efficiency is applied. */
public record RecyclingValue(ResourceLocation family, int units, String rule) {}
