package com.otectus.immersivesmithing.recipe;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.material.MaterialFamily;
import com.otectus.immersivesmithing.material.MaterialRegistry;
import com.otectus.immersivesmithing.material.MaterialUnits;
import com.otectus.immersivesmithing.material.UpgradePolicy;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import com.otectus.immersivesmithing.minigame.ForgePattern;
import com.otectus.immersivesmithing.registry.ModTags;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Conservative recipe analysis. A crafting recipe becomes a smithing recipe only when every ingredient is
 * either entirely one metal family or entirely non-metal, exactly one family is involved, and the result is
 * recognisable equipment. Anything ambiguous is skipped and reported, never guessed.
 */
public final class AutoRecipeDetector {

    public enum Status { OK, UPGRADE_CHAIN, NOT_METAL, MULTI_METAL, MIXED_INGREDIENT, EQUIPMENT_INGREDIENT, EMPTY_INGREDIENT, ERROR }

    /**
     * @param base for {@link Status#UPGRADE_CHAIN}: the one ingredient that is a finished piece of the same class as
     *             the result (a manasteel helmet reworked into a terrasteel helmet); null otherwise.
     */
    public record Analysis(ResourceLocation recipeId, ItemStack result, Status status, @Nullable ResourceLocation family,
                           int units, List<AuxiliaryIngredient> auxiliary, int metalSlots, int auxSlots, String detail,
                           @Nullable Ingredient base) {
        public Analysis(ResourceLocation recipeId, ItemStack result, Status status, @Nullable ResourceLocation family,
                        int units, List<AuxiliaryIngredient> auxiliary, int metalSlots, int auxSlots, String detail) {
            this(recipeId, result, status, family, units, auxiliary, metalSlots, auxSlots, detail, null);
        }

        /** Whether any ingredient was recognised as metal, which makes the recipe eligible for suppression. */
        public boolean hasMetal() {
            return metalSlots > 0;
        }
    }

    /**
     * Analyses every crafting recipe whose result is a single unstackable item, plus every recipe of an item that
     * already has an explicit smithing recipe (so its conventional recipe can be disabled even if it stacks).
     */
    public static Map<ResourceLocation, Analysis> analyzeCrafting(RecipeManager manager, RegistryAccess access, MaterialRegistry materials,
                                                                  Set<Item> alwaysAnalyse) {
        Map<ResourceLocation, Analysis> out = new LinkedHashMap<>();
        List<CraftingRecipe> recipes = new ArrayList<>(manager.getAllRecipesFor(RecipeType.CRAFTING));
        recipes.sort(Comparator.comparing(r -> r.getId().toString()));
        for (CraftingRecipe recipe : recipes) {
            try {
                if (recipe.isSpecial()) continue;
                ItemStack result = recipe.getResultItem(access);
                if (result.isEmpty() || (result.getMaxStackSize() != 1 && !alwaysAnalyse.contains(result.getItem()))) continue;
                out.put(recipe.getId(), analyze(recipe.getId(), recipe.getIngredients(), result, materials));
            } catch (RuntimeException e) {
                ImmersiveSmithing.LOGGER.debug("Could not analyse recipe {}: {}", recipe.getId(), e.toString());
            }
        }
        return out;
    }

    public static Analysis analyze(ResourceLocation id, List<Ingredient> ingredients, ItemStack result, MaterialRegistry materials) {
        boolean shieldResult = EquipmentClassifier.isShield(result);
        Map<ResourceLocation, Integer> families = new LinkedHashMap<>();
        Map<String, AuxBucket> aux = new LinkedHashMap<>();
        List<Ingredient> equipmentIngredients = new ArrayList<>();
        int metalSlots = 0;
        int auxSlots = 0;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) {
                return new Analysis(id, result, Status.EMPTY_INGREDIENT, null, 0, List.of(), metalSlots, auxSlots, "an ingredient matches no items");
            }
            Set<ResourceLocation> ingFamilies = new HashSet<>();
            Set<Integer> ingUnits = new HashSet<>();
            boolean nonMetal = false;
            boolean equipment = false;
            for (ItemStack s : items) {
                MaterialRegistry.Entry e = materials.lookup(s.getItem());
                if (e == null) {
                    nonMetal = true;
                    if (s.isDamageableItem()) equipment = true;
                } else {
                    ingFamilies.add(e.family());
                    ingUnits.add(e.units());
                }
            }
            if (ingFamilies.isEmpty()) {
                if (equipment) {
                    // A metal shield built on a base shield: the base is dismissed, not required at all.
                    if (shieldResult && isBaseShield(items)) continue;
                    equipmentIngredients.add(ingredient);
                    continue;
                }
                auxSlots++;
                String key = key(items);
                aux.computeIfAbsent(key, k -> new AuxBucket(ingredient)).count++;
            } else if (nonMetal || ingFamilies.size() > 1 || ingUnits.size() > 1) {
                metalSlots++;
                return new Analysis(id, result, Status.MIXED_INGREDIENT, null, 0, List.of(), metalSlots, auxSlots,
                        "an ingredient mixes metals, unit values or non-metal items");
            } else {
                metalSlots++;
                families.merge(ingFamilies.iterator().next(), ingUnits.iterator().next(), Integer::sum);
            }
        }
        if (families.isEmpty()) {
            // Bone, chitin or cloth work, whatever it is built on: not the forge's business, and not worth reporting.
            return new Analysis(id, result, Status.NOT_METAL, null, 0, List.of(), 0, auxSlots, "no metal ingredient");
        }
        if (families.size() > 1) {
            return new Analysis(id, result, Status.MULTI_METAL, null, 0, List.of(), metalSlots, auxSlots,
                    "uses several metal families " + families.keySet());
        }
        Map.Entry<ResourceLocation, Integer> only = families.entrySet().iterator().next();
        List<AuxiliaryIngredient> auxiliary = aux.values().stream().map(b -> new AuxiliaryIngredient(b.ingredient, b.count)).toList();
        if (!equipmentIngredients.isEmpty()) {
            if (equipmentIngredients.size() == 1 && isUpgradeBase(equipmentIngredients.get(0), result)) {
                return new Analysis(id, result, Status.UPGRADE_CHAIN, only.getKey(), only.getValue(), auxiliary, metalSlots, auxSlots,
                        "reworks a finished piece", equipmentIngredients.get(0));
            }
            return new Analysis(id, result, Status.EQUIPMENT_INGREDIENT, only.getKey(), only.getValue(), auxiliary, metalSlots, auxSlots,
                    "uses equipment as an ingredient");
        }
        return new Analysis(id, result, Status.OK, only.getKey(), only.getValue(), auxiliary, metalSlots, auxSlots, "");
    }

    /** Whether every item an ingredient accepts is candidate equipment of the same class as the result. */
    static boolean isUpgradeBase(Ingredient ingredient, ItemStack result) {
        ItemStack[] items = ingredient.getItems();
        if (items.length == 0 || !EquipmentClassifier.isCandidate(result)) return false;
        ResourceLocation pattern = EquipmentClassifier.classify(result);
        for (ItemStack s : items) {
            if (!EquipmentClassifier.isCandidate(s)) return false;
            if (EquipmentClassifier.armorSlot(s) != EquipmentClassifier.armorSlot(result)) return false;
            if (EquipmentClassifier.isShield(s) != EquipmentClassifier.isShield(result)) return false;
            if (!EquipmentClassifier.classify(s).equals(pattern)) return false;
        }
        return true;
    }

    /** Turns successful analyses of candidate equipment into automatic smithing recipes. */
    public static List<SmithingRecipe> generate(Collection<Analysis> analyses, Set<Item> explicitResults, CompatibilityReport report) {
        Set<String> recipeBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.RECIPE_BLACKLIST));
        Set<String> modBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.MOD_BLACKLIST));
        Map<Item, SmithingRecipe> generated = new LinkedHashMap<>();
        Set<Item> reported = new TreeSet<>(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));

        for (Analysis a : analyses) {
            Item item = a.result().getItem();
            if (!EquipmentClassifier.isCandidate(a.result())) continue;
            if (explicitResults.contains(item) || generated.containsKey(item)) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            String reason = null;
            if (recipeBlacklist.contains(a.recipeId().toString())) reason = "recipe blacklisted by config";
            else if (modBlacklist.contains(itemId.getNamespace())) reason = "mod blacklisted by config";
            else if (a.status() == Status.NOT_METAL) continue; // not metal equipment: nothing to report
            else if (a.status() == Status.UPGRADE_CHAIN) continue; // priced later, once the base's own recipe is known
            else if (a.status() != Status.OK) reason = a.detail();
            else if (a.result().getCount() != 1) reason = "recipe makes more than one item";
            else if (EquipmentClassifier.isShield(a.result()) && !a.result().is(ModTags.SMITHABLE_SHIELDS)
                    && a.metalSlots() < a.auxSlots()) reason = "shield is not predominantly metal";
            else if (!a.result().is(ModTags.SMITHABLE_EQUIPMENT) && !a.result().is(ModTags.SMITHABLE_SHIELDS)
                    && a.metalSlots() < a.auxSlots() && a.units() < MaterialUnits.INGOT) reason = "not predominantly metal";

            if (reason != null) {
                if (reported.add(item)) report.skippedEquipment(item, a.recipeId(), reason);
                continue;
            }
            ResourceLocation id = ImmersiveSmithing.id("auto/" + itemId.getNamespace() + "/" + itemId.getPath());
            SmithingRecipe recipe = new SmithingRecipe(id, a.family(), a.units(), a.auxiliary(), a.result().copy(),
                    ForgePattern.STANDARD, null, true, a.recipeId());
            generated.put(item, recipe);
            report.autoRecipe(recipe);
        }
        return new ArrayList<>(generated.values());
    }

    /** Whether every item an ingredient accepts is a shield (the base of a metal shield recipe). */
    static boolean isBaseShield(ItemStack[] items) {
        for (ItemStack s : items) {
            if (!EquipmentClassifier.isShield(s)) return false;
        }
        return items.length > 0;
    }

    // ------------------------------------------------------------------ smithing-table upgrades

    /** Lookups shared by every upgrade analysed in one rebuild. */
    public static final class UpgradeContext {
        final RegistryAccess access;
        final MaterialRegistry materials;
        final Map<Item, SmithingRecipe> smithable;
        final Map<Item, List<CraftingRecipe>> craftingByResult = new HashMap<>();
        final List<ItemStack> unstackables = new ArrayList<>();

        public UpgradeContext(RecipeManager manager, RegistryAccess access, MaterialRegistry materials, Map<Item, SmithingRecipe> smithable) {
            this.access = access;
            this.materials = materials;
            this.smithable = smithable;
            List<CraftingRecipe> crafting = new ArrayList<>(manager.getAllRecipesFor(RecipeType.CRAFTING));
            crafting.sort(Comparator.comparing(r -> r.getId().toString()));
            for (CraftingRecipe r : crafting) {
                try {
                    if (r.isSpecial()) continue;
                    ItemStack result = r.getResultItem(access);
                    if (!result.isEmpty()) craftingByResult.computeIfAbsent(result.getItem(), k -> new ArrayList<>()).add(r);
                } catch (RuntimeException ignored) {
                    // a broken third-party recipe cannot be a base
                }
            }
            for (Item item : BuiltInRegistries.ITEM) {
                ItemStack stack = new ItemStack(item);
                if (stack.getMaxStackSize() == 1) unstackables.add(stack);
            }
        }
    }

    /**
     * Redirects smithing-table upgrades (netherite and any other single-metal addition) into the forge. The
     * template is never required. The cost is the base item's shape in the addition's metal: the base's own
     * smithing recipe if it has one, otherwise its crafting recipe, where ingredients the base accepts as a repair
     * material (or gems/ingots) count as material slots.
     */
    public static List<SmithingRecipe> generateUpgrades(RecipeManager manager, UpgradeContext context, CompatibilityReport report) {
        Set<String> recipeBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.RECIPE_BLACKLIST));
        Set<String> modBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.MOD_BLACKLIST));
        List<SmithingTransformRecipe> upgrades = new ArrayList<>();
        for (var r : manager.getAllRecipesFor(RecipeType.SMITHING)) {
            if (r instanceof SmithingTransformRecipe t) upgrades.add(t);
        }
        upgrades.sort(Comparator.comparing(r -> r.getId().toString()));
        Map<Item, SmithingRecipe> generated = new LinkedHashMap<>();
        for (SmithingTransformRecipe upgrade : upgrades) {
            ItemStack result;
            try {
                result = upgrade.getResultItem(context.access);
            } catch (RuntimeException e) {
                continue;
            }
            if (result.isEmpty() || result.getCount() != 1 || !EquipmentClassifier.isCandidate(result)) continue;
            Item item = result.getItem();
            if (context.smithable.containsKey(item) || generated.containsKey(item)) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (recipeBlacklist.contains(upgrade.getId().toString()) || modBlacklist.contains(itemId.getNamespace())) continue;
            try {
                upgradeRecipe(upgrade, result, context, report).ifPresent(r -> {
                    generated.put(item, r);
                    report.autoRecipe(r);
                });
            } catch (RuntimeException e) {
                report.skippedEquipment(item, upgrade.getId(), "upgrade analysis failed: " + e);
            }
        }
        return new ArrayList<>(generated.values());
    }

    public static java.util.Optional<SmithingRecipe> upgradeRecipe(SmithingTransformRecipe upgrade, ItemStack result, UpgradeContext context,
                                                                   CompatibilityReport report) {
        Item item = result.getItem();
        // The addition must be exactly one metal family with one unit value (e.g. netherite ingot = 9).
        Set<ResourceLocation> families = new HashSet<>();
        Set<Integer> units = new HashSet<>();
        for (var family : context.materials.families()) {
            for (var source : family.sources()) {
                if (upgrade.isAdditionIngredient(new ItemStack(source.item()))) {
                    families.add(family.id());
                    units.add(source.units());
                }
            }
        }
        if (families.isEmpty()) return java.util.Optional.empty(); // not a metal upgrade
        if (families.size() != 1 || units.size() != 1) {
            report.skippedEquipment(item, upgrade.getId(), "upgrade addition is not a single metal family");
            return java.util.Optional.empty();
        }
        ResourceLocation family = families.iterator().next();
        int perAddition = units.iterator().next();

        List<ItemStack> bases = new ArrayList<>();
        for (ItemStack candidate : context.unstackables) {
            if (upgrade.isBaseIngredient(candidate)) bases.add(candidate);
        }
        if (bases.isEmpty()) {
            report.skippedEquipment(item, upgrade.getId(), "upgrade has no base item");
            return java.util.Optional.empty();
        }
        ItemStack base = bases.get(0);

        int metalUnits = -1;
        List<AuxiliaryIngredient> auxiliary = List.of();
        if (policy(context.materials, family) == UpgradePolicy.SHAPE) {
            SmithingRecipe baseRecipe = context.smithable.get(base.getItem());
            if (baseRecipe != null) {
                metalUnits = Math.max(1, Math.round(baseRecipe.metalUnits() * perAddition / 9F));
                auxiliary = baseRecipe.auxiliary();
            } else {
                for (CraftingRecipe crafting : context.craftingByResult.getOrDefault(base.getItem(), List.of())) {
                    Shape shape = baseShape(base, crafting.getIngredients(), EquipmentClassifier.isShield(result));
                    if (shape != null) {
                        metalUnits = shape.primarySlots * perAddition;
                        auxiliary = shape.auxiliary;
                        break;
                    }
                }
            }
        }
        if (metalUnits <= 0) {
            // Addition policy, or a base with no metal shape of its own (a loot weapon, a cloth robe): the smith
            // pays the addition and reworks the base piece itself.
            metalUnits = perAddition;
            auxiliary = List.of(new AuxiliaryIngredient(Ingredient.of(bases.toArray(ItemStack[]::new)), 1, true));
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        ResourceLocation id = ImmersiveSmithing.id("auto/" + itemId.getNamespace() + "/" + itemId.getPath());
        return java.util.Optional.of(new SmithingRecipe(id, family, metalUnits, auxiliary, result.copy(), ForgePattern.STANDARD, null, true, upgrade.getId()));
    }

    static UpgradePolicy policy(MaterialRegistry materials, ResourceLocation family) {
        return materials.family(family).map(MaterialFamily::upgradePolicy).orElse(UpgradePolicy.SHAPE);
    }

    // ------------------------------------------------------------------ crafting upgrade chains

    /**
     * Crafting recipes that rework a finished piece with more metal (Botania's terrasteel armour is made from
     * manasteel armour and terrasteel). Priced like a smithing-table upgrade: the base's shape in the new metal
     * under the {@code shape} policy, or the metal the recipe asks for plus the base piece itself under
     * {@code addition}. A base with no metal shape of its own always uses {@code addition}.
     */
    public static List<SmithingRecipe> generateChains(Collection<Analysis> analyses, UpgradeContext context, CompatibilityReport report) {
        Set<String> recipeBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.RECIPE_BLACKLIST));
        Set<String> modBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.MOD_BLACKLIST));
        Map<Item, SmithingRecipe> generated = new LinkedHashMap<>();
        for (Analysis a : analyses) {
            if (a.status() != Status.UPGRADE_CHAIN || a.base() == null) continue;
            Item item = a.result().getItem();
            if (!EquipmentClassifier.isCandidate(a.result())) continue;
            if (context.smithable.containsKey(item) || generated.containsKey(item)) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            if (recipeBlacklist.contains(a.recipeId().toString())) {
                report.skippedEquipment(item, a.recipeId(), "recipe blacklisted by config");
                continue;
            }
            if (modBlacklist.contains(itemId.getNamespace())) {
                report.skippedEquipment(item, a.recipeId(), "mod blacklisted by config");
                continue;
            }
            if (a.result().getCount() != 1) {
                report.skippedEquipment(item, a.recipeId(), "recipe makes more than one item");
                continue;
            }
            ItemStack[] bases = a.base().getItems();
            boolean smithableBase = false;
            for (ItemStack base : bases) {
                if (context.smithable.containsKey(base.getItem())) smithableBase = true;
            }
            if (!smithableBase) {
                report.skippedEquipment(item, a.recipeId(), "reworks a piece the forge does not make: "
                        + BuiltInRegistries.ITEM.getKey(bases[0].getItem()));
                continue;
            }
            int metalUnits = -1;
            List<AuxiliaryIngredient> auxiliary = List.of();
            if (policy(context.materials, a.family()) == UpgradePolicy.SHAPE) {
                for (ItemStack base : bases) {
                    SmithingRecipe baseRecipe = context.smithable.get(base.getItem());
                    if (baseRecipe != null) {
                        metalUnits = baseRecipe.metalUnits();
                        List<AuxiliaryIngredient> merged = new ArrayList<>(baseRecipe.auxiliary());
                        merged.addAll(a.auxiliary());
                        auxiliary = merged;
                        break;
                    }
                }
            }
            if (metalUnits <= 0) {
                metalUnits = a.units();
                List<AuxiliaryIngredient> merged = new ArrayList<>();
                merged.add(new AuxiliaryIngredient(a.base(), 1, true));
                merged.addAll(a.auxiliary());
                auxiliary = merged;
            }
            ResourceLocation id = ImmersiveSmithing.id("auto/" + itemId.getNamespace() + "/" + itemId.getPath());
            SmithingRecipe recipe = new SmithingRecipe(id, a.family(), metalUnits, auxiliary, a.result().copy(),
                    ForgePattern.STANDARD, null, true, a.recipeId());
            generated.put(item, recipe);
            context.smithable.put(item, recipe);
            report.autoRecipe(recipe);
        }
        return new ArrayList<>(generated.values());
    }

    private record Shape(int primarySlots, List<AuxiliaryIngredient> auxiliary) {}

    /** Splits a base item's crafting recipe into its material slots and everything else, or null if unusable. */
    private static Shape baseShape(ItemStack base, List<Ingredient> ingredients, boolean shieldResult) {
        int primary = 0;
        Map<String, AuxBucket> aux = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) return null;
            boolean material = false;
            boolean equipment = false;
            for (ItemStack s : items) {
                if (base.getItem().isValidRepairItem(base, s) || s.is(net.minecraftforge.common.Tags.Items.GEMS)
                        || s.is(net.minecraftforge.common.Tags.Items.INGOTS)) material = true;
                if (s.isDamageableItem()) equipment = true;
            }
            if (material) {
                primary++;
            } else if (equipment) {
                if (shieldResult && isBaseShield(items)) continue;
                return null;
            } else {
                aux.computeIfAbsent(key(items), k -> new AuxBucket(ingredient)).count++;
            }
        }
        if (primary == 0) return null;
        return new Shape(primary, aux.values().stream().map(b -> new AuxiliaryIngredient(b.ingredient, b.count)).toList());
    }

    private static String key(ItemStack[] items) {
        TreeSet<String> ids = new TreeSet<>();
        for (ItemStack s : items) {
            ids.add(BuiltInRegistries.ITEM.getKey(s.getItem()) + (s.hasTag() ? String.valueOf(s.getTag()) : ""));
        }
        return String.join(",", ids);
    }

    private static final class AuxBucket {
        final Ingredient ingredient;
        int count;

        AuxBucket(Ingredient ingredient) {
            this.ingredient = ingredient;
        }
    }

    private AutoRecipeDetector() {}
}
