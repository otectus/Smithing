package com.otectus.otessmithing.material;

import com.otectus.otessmithing.api.OtesSmithingAPI;
import com.otectus.otessmithing.api.IRecyclingValueProvider;
import com.otectus.otessmithing.config.ServerConfig;
import com.otectus.otessmithing.recipe.SmithingData;
import com.otectus.otessmithing.registry.ModTags;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Answers "what metal is this stack, and how much of it" for the Smith's Forge. */
public final class MaterialResolver {

    public enum Kind { SOURCE, RECYCLE }

    public record Resolved(MaterialFamily family, int unitsEach, Kind kind, String rule) {}

    public static Optional<Resolved> resolve(ItemStack stack) {
        if (stack.isEmpty()) return Optional.empty();
        SmithingData data = SmithingData.current();
        MaterialRegistry.Entry entry = data.materials().lookup(stack.getItem());
        if (entry != null) {
            return data.materials().family(entry.family())
                    .map(f -> new Resolved(f, entry.units(), Kind.SOURCE, entry.rule()));
        }
        return recyclingValue(stack).flatMap(value -> {
            int units = recycledUnits(value.units());
            if (units <= 0) return Optional.empty();
            return data.materials().family(value.family()).map(f -> new Resolved(f, units, Kind.RECYCLE, value.rule()));
        });
    }

    /** The unscaled metal content of a piece of equipment, if it can be determined safely. */
    public static Optional<RecyclingValue> recyclingValue(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModTags.NON_RECYCLABLE) || OtesSmithingAPI.isExcluded(stack)) return Optional.empty();
        SmithingData data = SmithingData.current();
        if (data.isRecyclingExcluded(stack.getItem())) return Optional.empty();
        RecyclingValue value = data.recycling(stack.getItem());
        if (value != null) return Optional.of(value);
        for (IRecyclingValueProvider provider : OtesSmithingAPI.recyclingProviders()) {
            Optional<RecyclingValue> provided = provider.recyclingValue(stack);
            if (provided.isPresent()) return provided;
        }
        return Optional.empty();
    }

    /** Applies recycling efficiency and never returns more than the recognized value. */
    public static int recycledUnits(int fullUnits) {
        double efficiency = ServerConfig.get(ServerConfig.RECYCLING_EFFICIENCY);
        return Math.min(fullUnits, (int) Math.floor(fullUnits * efficiency));
    }

    private MaterialResolver() {}
}
