package com.otectus.otessmithing.minigame;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/** Server-side store of datapack patterns, with built-in fallbacks so a missing file never blocks smithing. */
public final class PatternRegistry {
    private static volatile Map<ResourceLocation, ForgePattern> forgePatterns = Map.of();
    private static volatile Map<ResourceLocation, AnvilPattern> anvilPatterns = Map.of();

    public static ForgePattern forge(@Nullable ResourceLocation id) {
        ForgePattern p = id == null ? null : forgePatterns.get(id);
        if (p == null) p = forgePatterns.get(ForgePattern.STANDARD);
        return p != null ? p : ForgePattern.DEFAULT;
    }

    public static AnvilPattern anvil(@Nullable ResourceLocation id) {
        AnvilPattern p = id == null ? null : anvilPatterns.get(id);
        if (p == null) p = anvilPatterns.get(EquipmentClassifier.GENERIC_EQUIPMENT);
        return p != null ? p : AnvilPattern.fallback(id != null ? id : EquipmentClassifier.GENERIC_EQUIPMENT);
    }

    public static boolean hasAnvilPattern(ResourceLocation id) {
        return anvilPatterns.containsKey(id);
    }

    /** The first pattern (by id) listing one of the stack's item tags as a category, if any. */
    @Nullable
    public static ResourceLocation anvilPatternForCategories(ItemStack stack) {
        for (AnvilPattern pattern : anvilPatterns.values()) {
            for (TagKey<Item> tag : pattern.categories()) {
                if (stack.is(tag)) return pattern.id();
            }
        }
        return null;
    }

    static void setForgePatterns(Map<ResourceLocation, ForgePattern> patterns) {
        forgePatterns = Map.copyOf(patterns);
    }

    static void setAnvilPatterns(Map<ResourceLocation, AnvilPattern> patterns) {
        anvilPatterns = java.util.Collections.unmodifiableMap(new TreeMap<>(patterns));
    }

    public static Map<ResourceLocation, AnvilPattern> anvilPatterns() {
        return anvilPatterns;
    }

    private PatternRegistry() {}
}
