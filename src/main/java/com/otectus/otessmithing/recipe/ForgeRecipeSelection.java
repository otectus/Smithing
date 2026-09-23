package com.otectus.otessmithing.recipe;

import com.otectus.otessmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.otessmithing.material.MaterialFamily;
import com.otectus.otessmithing.material.MaterialUnits;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Which recipes the Forge screen may offer: right family, enough molten metal, auxiliary items on hand. */
public final class ForgeRecipeSelection {

    /** The inventory slot holding the tongs, which must never be consumed as an ingredient. */
    public static int protectedSlot(Player player, InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
    }

    public static List<SmithingRecipe> eligible(Player player, SmithsForgeBlockEntity forge, int protectedSlot) {
        return SmithingData.current().recipesFor(forge.family()).stream()
                .filter(r -> r.metalUnits() <= forge.moltenUnits())
                .filter(r -> AuxInventory.hasAll(player, r.auxiliary(), protectedSlot))
                .sorted(Comparator.comparing(r -> BuiltInRegistries.ITEM.getKey(r.resultItem()).toString()))
                .toList();
    }

    /** A short reason why no recipe can be offered. */
    public static Component whyNone(Player player, SmithsForgeBlockEntity forge, int protectedSlot) {
        Component family = forge.familyData().map(MaterialFamily::displayName).orElse(Component.literal("?"));
        List<SmithingRecipe> all = SmithingData.current().recipesFor(forge.family());
        if (all.isEmpty()) {
            return Component.translatable("message.otes_smithing.material_unsupported", family);
        }
        int cheapest = all.stream().mapToInt(SmithingRecipe::metalUnits).min().orElse(0);
        if (cheapest > forge.moltenUnits()) {
            return Component.translatable("message.otes_smithing.not_enough_metal", family,
                    MaterialUnits.describe(forge.moltenUnits()), MaterialUnits.describe(cheapest));
        }
        for (SmithingRecipe r : all) {
            if (r.metalUnits() > forge.moltenUnits()) continue;
            Optional<AuxiliaryIngredient> missing = AuxInventory.firstMissing(player, r.auxiliary(), protectedSlot);
            if (missing.isPresent()) {
                return Component.translatable("message.otes_smithing.missing_auxiliary", missing.get().count(),
                        missing.get().displayStack().getHoverName(), r.result().getHoverName());
            }
        }
        return Component.translatable("message.otes_smithing.no_recipes");
    }

    private ForgeRecipeSelection() {}
}
