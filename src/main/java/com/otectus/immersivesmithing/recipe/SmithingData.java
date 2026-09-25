package com.otectus.immersivesmithing.recipe;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.api.ISmithingMaterialProvider;
import com.otectus.immersivesmithing.api.ISmithingRecipeProvider;
import com.otectus.immersivesmithing.api.ImmersiveSmithingAPI;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.material.MaterialDefinition;
import com.otectus.immersivesmithing.material.MaterialDefinitionLoader;
import com.otectus.immersivesmithing.material.MaterialFamily;
import com.otectus.immersivesmithing.material.MaterialRegistry;
import com.otectus.immersivesmithing.material.RecyclingOverride;
import com.otectus.immersivesmithing.material.RecyclingOverrideLoader;
import com.otectus.immersivesmithing.material.RecyclingValue;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import com.otectus.immersivesmithing.minigame.PatternRegistry;
import com.otectus.immersivesmithing.registry.ModTags;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
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

/**
 * Immutable server-side snapshot of materials, active smithing recipes, recycling values and disabled
 * crafting recipes. Rebuilt after every datapack reload once tags are bound (never during gameplay ticks);
 * gameplay code only reads {@link #current()}.
 */
public final class SmithingData {
    private static final SmithingData EMPTY = new SmithingData(MaterialRegistry.EMPTY, List.of(), Map.of(), Set.of(), Map.of(), new CompatibilityReport());
    private static volatile SmithingData current = EMPTY;
    private static volatile boolean dirty = true;

    private final MaterialRegistry materials;
    private final Map<ResourceLocation, SmithingRecipe> byId;
    private final Map<ResourceLocation, List<SmithingRecipe>> byFamily;
    private final Map<Item, List<SmithingRecipe>> byResult;
    private final Map<Item, RecyclingValue> recycling;
    private final Set<Item> recyclingExcluded;
    private final Map<ResourceLocation, Item> suppressed;
    private final CompatibilityReport report;

    private SmithingData(MaterialRegistry materials, List<SmithingRecipe> recipes, Map<Item, RecyclingValue> recycling,
                         Set<Item> recyclingExcluded, Map<ResourceLocation, Item> suppressed, CompatibilityReport report) {
        this.materials = materials;
        Map<ResourceLocation, SmithingRecipe> ids = new LinkedHashMap<>();
        Map<ResourceLocation, List<SmithingRecipe>> families = new HashMap<>();
        Map<Item, List<SmithingRecipe>> results = new HashMap<>();
        for (SmithingRecipe r : recipes) {
            ids.put(r.getId(), r);
            families.computeIfAbsent(r.family(), k -> new ArrayList<>()).add(r);
            results.computeIfAbsent(r.resultItem(), k -> new ArrayList<>()).add(r);
        }
        families.replaceAll((k, v) -> List.copyOf(v));
        results.replaceAll((k, v) -> List.copyOf(v));
        this.byId = Map.copyOf(ids);
        this.byFamily = Map.copyOf(families);
        this.byResult = Map.copyOf(results);
        this.recycling = Map.copyOf(recycling);
        this.recyclingExcluded = Set.copyOf(recyclingExcluded);
        this.suppressed = Map.copyOf(suppressed);
        this.report = report;
    }

    public static SmithingData current() {
        return current;
    }

    public static void markDirty() {
        dirty = true;
    }

    public static boolean isDirty() {
        return dirty;
    }

    public MaterialRegistry materials() { return materials; }
    @Nullable public SmithingRecipe recipe(ResourceLocation id) { return byId.get(id); }
    public Collection<SmithingRecipe> recipes() { return byId.values(); }
    public List<SmithingRecipe> recipesFor(@Nullable ResourceLocation family) { return family == null ? List.of() : byFamily.getOrDefault(family, List.of()); }
    public List<SmithingRecipe> recipesProducing(Item item) { return byResult.getOrDefault(item, List.of()); }
    @Nullable public RecyclingValue recycling(Item item) { return recycling.get(item); }
    public boolean isRecyclingExcluded(Item item) { return recyclingExcluded.contains(item); }
    /** Disabled conventional recipe ids and the item each one produced. */
    public Map<ResourceLocation, Item> suppressedRecipes() { return suppressed; }
    public CompatibilityReport report() { return report; }

    /** Rebuilds everything from the server's current recipes and tags, then rewrites its recipe manager. */
    public static synchronized void rebuild(MinecraftServer server) {
        long start = System.nanoTime();
        RecipeManager manager = server.getRecipeManager();
        RegistryAccess access = server.registryAccess();
        CompatibilityReport report = new CompatibilityReport();

        // 1. Material families.
        List<MaterialDefinition> definitions = new ArrayList<>(MaterialDefinitionLoader.definitions());
        for (ISmithingMaterialProvider provider : ImmersiveSmithingAPI.materialProviders()) {
            try {
                definitions.addAll(provider.materials());
            } catch (RuntimeException e) {
                ImmersiveSmithing.LOGGER.error("Smithing material provider failed", e);
            }
        }
        Map<ResourceLocation, MaterialDefinition> mergedDefs = new LinkedHashMap<>();
        definitions.forEach(d -> mergedDefs.merge(d.family(), d, MaterialDefinition::merge));
        MaterialRegistry materials = MaterialRegistry.build(mergedDefs.values(), report);

        // 2. Explicit recipes (datapack), validated against the families that actually exist.
        List<Recipe<?>> all = new ArrayList<>(manager.getRecipes());
        List<SmithingRecipe> active = new ArrayList<>();
        Set<ResourceLocation> removed = new HashSet<>();
        Set<Item> explicitResults = new HashSet<>();
        List<SmithingRecipe> explicit = new ArrayList<>();
        for (Recipe<?> r : all) {
            if (r instanceof SmithingRecipe s) {
                if (s.isAuto()) removed.add(s.getId()); // stale generated recipe from an earlier pass
                else explicit.add(s);
            }
        }
        for (ISmithingRecipeProvider provider : ImmersiveSmithingAPI.recipeProviders()) {
            try {
                explicit.addAll(provider.recipes(materials, access));
            } catch (RuntimeException e) {
                ImmersiveSmithing.LOGGER.error("Smithing recipe provider failed", e);
            }
        }
        explicit.sort(Comparator.comparing(r -> r.getId().toString()));
        Set<ResourceLocation> seenIds = new HashSet<>();
        for (SmithingRecipe r : explicit) {
            String reason = null;
            if (!seenIds.add(r.getId())) reason = "duplicate recipe id";
            else if (isSpartanCompat(r) && !ServerConfig.get(ServerConfig.SPARTAN_WEAPONRY_INTEGRATION)) reason = "Spartan Weaponry integration is disabled in the config";
            else if (materials.family(r.family()).isEmpty()) reason = "unknown material family " + r.family();
            else if (r.result().is(ModTags.NON_SMITHABLE_EQUIPMENT) || ImmersiveSmithingAPI.isExcluded(r.result())) reason = "result is excluded from smithing";
            if (reason != null) {
                removed.add(r.getId());
                report.invalidRecipe(r.getId(), reason);
                continue;
            }
            active.add(r);
            explicitResults.add(r.resultItem());
            report.explicitRecipe(r);
        }

        // 3. Crafting analysis, used both for automatic recipes and for recycling values.
        Map<ResourceLocation, AutoRecipeDetector.Analysis> analyses = AutoRecipeDetector.analyzeCrafting(manager, access, materials, explicitResults);
        List<SmithingRecipe> auto = new ArrayList<>();
        if (ServerConfig.get(ServerConfig.AUTO_DETECT_EQUIPMENT)) {
            auto.addAll(AutoRecipeDetector.generate(analyses.values(), explicitResults, report));
            // Smithing-table upgrades (modded netherite and similar) are redirected into the forge as well.
            Map<Item, SmithingRecipe> smithable = new HashMap<>();
            active.forEach(r -> smithable.putIfAbsent(r.resultItem(), r));
            auto.forEach(r -> smithable.putIfAbsent(r.resultItem(), r));
            AutoRecipeDetector.UpgradeContext upgrades = new AutoRecipeDetector.UpgradeContext(manager, access, materials, smithable);
            List<SmithingRecipe> redirected = AutoRecipeDetector.generateUpgrades(manager, upgrades, report);
            redirected.forEach(r -> smithable.putIfAbsent(r.resultItem(), r));
            auto.addAll(redirected);
            // Crafting recipes that rework a finished piece with more metal, priced from the base's own recipe.
            auto.addAll(AutoRecipeDetector.generateChains(analyses.values(), upgrades, report));
        }
        active.addAll(auto);

        // 4. Presentation data travels with each recipe to the client.
        for (SmithingRecipe r : active) {
            MaterialFamily family = materials.family(r.family()).orElseThrow();
            ResourceLocation pattern = r.explicitAnvilPattern() != null ? r.explicitAnvilPattern() : EquipmentClassifier.classify(r.result());
            if (!PatternRegistry.hasAnvilPattern(pattern)) {
                report.note("Anvil pattern " + pattern + " for " + r.getId() + " is not defined; the generic pattern is used");
            }
            r.setDisplay(family.displayName(), SmithingRecipe.metalDisplay(family, r.metalUnits()), pattern);
        }

        // 5. Disable conventional recipes for smithable results.
        Set<Item> smithable = new HashSet<>();
        active.forEach(r -> smithable.add(r.resultItem()));
        Map<ResourceLocation, Item> suppressed = new HashMap<>();
        boolean replace = ServerConfig.get(ServerConfig.REPLACE_METAL_EQUIPMENT_RECIPES)
                && !ServerConfig.get(ServerConfig.ALLOW_VANILLA_CRAFTING_ALONGSIDE_SMITHING);
        if (replace) {
            Set<String> recipeBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.RECIPE_BLACKLIST));
            Set<String> modBlacklist = new HashSet<>(ServerConfig.list(ServerConfig.MOD_BLACKLIST));
            boolean smithingTable = ServerConfig.get(ServerConfig.SUPPRESS_SMITHING_TABLE_UPGRADES);
            for (Recipe<?> r : all) {
                if (recipeBlacklist.contains(r.getId().toString())) continue;
                ItemStack result;
                try {
                    result = r.getResultItem(access);
                } catch (RuntimeException e) {
                    continue;
                }
                if (result.isEmpty() || !smithable.contains(result.getItem())) continue;
                if (modBlacklist.contains(BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace())) continue;
                boolean disable = false;
                if (r.getType() == RecipeType.CRAFTING) {
                    AutoRecipeDetector.Analysis a = analyses.get(r.getId());
                    disable = a != null && a.hasMetal();
                } else if (r.getType() == RecipeType.SMITHING) {
                    // Only upgrade recipes. Trim recipes report a sample armor piece as their result.
                    disable = smithingTable && r instanceof SmithingTransformRecipe;
                }
                if (disable) {
                    suppressed.put(r.getId(), result.getItem());
                    report.suppressed(r.getId(), result.getItem());
                }
            }
        }

        // 6. Rewrite the recipe manager: remove disabled/invalid/stale entries, add the automatic recipes.
        List<Recipe<?>> next = new ArrayList<>(all.size() + auto.size());
        for (Recipe<?> r : all) {
            if (!removed.contains(r.getId()) && !suppressed.containsKey(r.getId())) next.add(r);
        }
        next.addAll(auto);
        manager.replaceRecipes(next);

        // 7. Recycling values. Exclusions first, then explicit recipes, datapack overrides, crafting analysis.
        Set<Item> excluded = new HashSet<>();
        Map<Item, RecyclingValue> recycling = new HashMap<>();
        List<RecyclingOverride> overrides = RecyclingOverrideLoader.overrides();
        for (RecyclingOverride o : overrides) {
            if (!o.recyclable()) excluded.addAll(itemsOf(o));
        }
        for (SmithingRecipe r : active) {
            if (!r.isAuto()) recycling.putIfAbsent(r.resultItem(), new RecyclingValue(r.family(), r.metalUnits(), "smithing recipe " + r.getId()));
        }
        for (RecyclingOverride o : overrides) {
            if (!o.recyclable() || o.family() == null) continue;
            if (materials.family(o.family()).isEmpty()) {
                report.note("Recycling override for " + (o.item() != null ? o.item() : "#" + o.tag().location()) + " names unknown family " + o.family());
                continue;
            }
            for (Item item : itemsOf(o)) recycling.putIfAbsent(item, new RecyclingValue(o.family(), o.units(), "recycling override"));
        }
        for (AutoRecipeDetector.Analysis a : analyses.values()) {
            if (a.status() != AutoRecipeDetector.Status.OK || a.result().getCount() != 1) continue;
            if (!EquipmentClassifier.isCandidate(a.result())) continue;
            recycling.putIfAbsent(a.result().getItem(), new RecyclingValue(a.family(), a.units(), "crafting recipe " + a.recipeId()));
        }
        for (SmithingRecipe r : auto) {
            recycling.putIfAbsent(r.resultItem(), new RecyclingValue(r.family(), r.metalUnits(), "automatic recipe " + r.getId()));
        }
        excluded.forEach(recycling::remove);

        SmithingData data = new SmithingData(materials, active, recycling, excluded, suppressed, report);
        current = data;
        dirty = false;
        ImmersiveSmithing.LOGGER.info("Immersive Smithing: {} ({} ms)", report.summary(), (System.nanoTime() - start) / 1_000_000);
    }

    /**
     * Built-in Spartan Weaponry recipes live under immersive_smithing:compat/spartanweaponry/ (and the Spartan Fire
     * dragonsteel weapons under compat/spartanfire/) and follow the Spartan Weaponry config toggle.
     */
    private static boolean isSpartanCompat(SmithingRecipe r) {
        return r.getId().getNamespace().equals(ImmersiveSmithing.MOD_ID)
                && (r.getId().getPath().startsWith("compat/spartanweaponry/") || r.getId().getPath().startsWith("compat/spartanfire/"));
    }

    private static List<Item> itemsOf(RecyclingOverride o) {
        if (o.item() != null) {
            return BuiltInRegistries.ITEM.containsKey(o.item()) ? List.of(BuiltInRegistries.ITEM.get(o.item())) : List.of();
        }
        return BuiltInRegistries.ITEM.getTag(o.tag()).map(s -> s.stream().map(Holder::value).toList()).orElse(List.of());
    }

    /** Clears state when the server stops, so a later world starts clean. */
    public static void reset() {
        current = EMPTY;
        dirty = true;
    }
}
