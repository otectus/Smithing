package com.otectus.immersivesmithing.recipe;

import com.otectus.immersivesmithing.material.MaterialFamily;
import com.otectus.immersivesmithing.material.MaterialUnits;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.registry.ModRecipeSerializers;
import com.otectus.immersivesmithing.registry.ModRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One smithable item: which material family and how many units it takes, which auxiliary ingredients, and
 * which forge and anvil patterns it uses. Explicit recipes come from datapacks; automatic ones are generated
 * from crafting recipes at reload and inserted into the recipe manager so clients (and JEI) receive them too.
 */
public class SmithingRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final ResourceLocation family;
    private final int metalUnits;
    private final List<AuxiliaryIngredient> auxiliary;
    private final ItemStack result;
    private final ResourceLocation forgePattern;
    @Nullable
    private final ResourceLocation anvilPattern;
    private final boolean auto;
    @Nullable
    private final ResourceLocation sourceRecipe;

    // Presentation data. Filled in on the server during rebuild and carried to clients over the network.
    private Component familyName;
    private List<List<ItemStack>> metalDisplay = List.of();
    @Nullable
    private ResourceLocation resolvedAnvilPattern;

    public SmithingRecipe(ResourceLocation id, ResourceLocation family, int metalUnits, List<AuxiliaryIngredient> auxiliary,
                          ItemStack result, ResourceLocation forgePattern, @Nullable ResourceLocation anvilPattern,
                          boolean auto, @Nullable ResourceLocation sourceRecipe) {
        this.id = id;
        this.family = family;
        this.metalUnits = metalUnits;
        this.auxiliary = List.copyOf(auxiliary);
        this.result = result;
        this.forgePattern = forgePattern;
        this.anvilPattern = anvilPattern;
        this.auto = auto;
        this.sourceRecipe = sourceRecipe;
        this.familyName = MaterialFamily.defaultName(family);
    }

    public ResourceLocation family() { return family; }
    public int metalUnits() { return metalUnits; }
    public List<AuxiliaryIngredient> auxiliary() { return auxiliary; }
    public ItemStack result() { return result; }
    public ResourceLocation forgePattern() { return forgePattern; }
    @Nullable public ResourceLocation explicitAnvilPattern() { return anvilPattern; }
    public boolean isAuto() { return auto; }
    @Nullable public ResourceLocation sourceRecipe() { return sourceRecipe; }
    public Component familyName() { return familyName; }
    public List<List<ItemStack>> metalDisplay() { return metalDisplay; }

    /** The anvil pattern: explicit if given, otherwise the closest pattern family for the result. */
    public ResourceLocation anvilPattern() {
        if (anvilPattern != null) return anvilPattern;
        if (resolvedAnvilPattern != null) return resolvedAnvilPattern;
        return EquipmentClassifier.classify(result);
    }

    public Item resultItem() {
        return result.getItem();
    }

    public void setDisplay(Component familyName, List<List<ItemStack>> metalDisplay, @Nullable ResourceLocation resolvedAnvilPattern) {
        this.familyName = familyName;
        this.metalDisplay = metalDisplay;
        this.resolvedAnvilPattern = resolvedAnvilPattern;
    }

    /** Ingots (and nuggets for the remainder) that represent {@code units} of a family, as JEI slot contents. */
    public static List<List<ItemStack>> metalDisplay(MaterialFamily family, int units) {
        List<List<ItemStack>> slots = new ArrayList<>();
        int ingots = units / MaterialUnits.INGOT;
        int nuggets = units % MaterialUnits.INGOT;
        List<Item> ingotItems = family.ingotItems();
        List<Item> nuggetItems = family.nuggetItems();
        if (ingots > 0 && !ingotItems.isEmpty()) {
            slots.add(ingotItems.stream().map(i -> new ItemStack(i, ingots)).toList());
        }
        if (nuggets > 0 && !nuggetItems.isEmpty()) {
            slots.add(nuggetItems.stream().map(i -> new ItemStack(i, nuggets)).toList());
        }
        if (slots.isEmpty()) {
            ItemStack rep = family.representative();
            if (!rep.isEmpty()) slots.add(List.of(rep));
        }
        return List.copyOf(slots);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        auxiliary.forEach(a -> list.add(a.ingredient()));
        return list;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.SMITHS_FORGE.get());
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.SMITHING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.SMITHING.get();
    }
}
