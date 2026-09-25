package com.otectus.immersivesmithing.material;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One smithable metal. Resolved against the current item tags, so the source list is concrete items.
 * Deliberately carries no equipment statistics.
 */
public record MaterialFamily(
        ResourceLocation id,
        Component displayName,
        float meltMultiplier,
        boolean requiresIgnition,
        @Nullable TagKey<Item> requiredFuelTag,
        int tint,
        boolean auto,
        UpgradePolicy upgradePolicy,
        List<Source> sources) {

    public static final int DEFAULT_TINT = 0xFF8A3D;

    /** A concrete item accepted as this material, worth {@code units} material units. */
    public record Source(Item item, int units, String rule) {}

    /** Items worth exactly one ingot (9 units), used for display. */
    public List<Item> ingotItems() {
        return sources.stream().filter(s -> s.units() == MaterialUnits.INGOT).map(Source::item).distinct().toList();
    }

    /** Items worth exactly one nugget (1 unit), used for display. */
    public List<Item> nuggetItems() {
        return sources.stream().filter(s -> s.units() == MaterialUnits.NUGGET).map(Source::item).distinct().toList();
    }

    public ItemStack representative() {
        List<Item> ingots = ingotItems();
        if (!ingots.isEmpty()) return new ItemStack(ingots.get(0));
        return sources.isEmpty() ? ItemStack.EMPTY : new ItemStack(sources.get(0).item());
    }

    public static Component defaultName(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("_")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return Component.translatableWithFallback("material." + id.getNamespace() + "." + path, sb.toString());
    }
}
