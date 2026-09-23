package com.otectus.otessmithing.test;

import com.oblivioussp.spartanweaponry.entity.projectile.ThrowingWeaponEntity;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.minigame.EquipmentClassifier;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.recipe.SmithingData;
import com.otectus.otessmithing.recipe.SmithingRecipe;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Automatic detection against whatever content mods are loaded (see -PcompatMods). Always writes the
 * compatibility report to run-gametest/logs so the decisions can be reviewed.
 */
@GameTestHolder(OtesSmithing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CompatGameTests {

    @GameTest(template = "empty")
    public static void autoDetectionAgainstLoadedMods(GameTestHelper h) throws Exception {
        SmithingData data = SmithingData.current();
        data.report().write(Path.of("logs"));
        RecipeManager rm = h.getLevel().getServer().getRecipeManager();
        List<SmithingRecipe> auto = data.recipes().stream().filter(SmithingRecipe::isAuto).toList();
        for (SmithingRecipe r : auto) {
            h.assertTrue(r.sourceRecipe() != null, "Auto recipe records its source");
            h.assertTrue(rm.byKey(r.getId()).isPresent(), "Auto recipe is in the recipe manager: " + r.getId());
            h.assertTrue(rm.byKey(r.sourceRecipe()).isEmpty(), "Source crafting recipe is disabled: " + r.sourceRecipe());
            h.assertTrue(data.suppressedRecipes().containsKey(r.sourceRecipe()), "Suppression recorded: " + r.sourceRecipe());
            h.assertTrue(r.metalUnits() > 0 && data.materials().family(r.family()).isPresent(), "Valid family and units: " + r.getId());
            String path = BuiltInRegistries.ITEM.getKey(r.resultItem()).getPath();
            h.assertTrue(!path.startsWith("wooden_") && !path.startsWith("diamond_") && !path.startsWith("stone_") && !path.startsWith("leather_"),
                    "Non-metal equipment must not be detected: " + path);
        }
        OtesSmithing.LOGGER.info("Compat test: {} automatic recipes; {}", auto.size(), data.report().summary());
        h.succeed();
    }

    private static final List<String> SPARTAN_TYPES = List.of("battle_hammer", "battleaxe", "boomerang", "dagger", "flanged_mace",
            "glaive", "greatsword", "halberd", "heavy_crossbow", "javelin", "katana", "lance", "longbow", "longsword", "parrying_dagger",
            "pike", "quarterstaff", "rapier", "saber", "scythe", "spear", "throwing_knife", "tomahawk", "warhammer");
    private static final Map<String, String> SPARTAN_METALS = Map.of("iron", "minecraft:iron", "golden", "minecraft:gold",
            "copper", "minecraft:copper", "netherite", "minecraft:netherite");

    @GameTest(template = "empty")
    public static void spartanWeaponryIsSmithedByDefault(GameTestHelper h) {
        if (!ModList.get().isLoaded("spartanweaponry")) {
            h.succeed();
            return;
        }
        SmithingData data = SmithingData.current();
        RecipeManager rm = h.getLevel().getServer().getRecipeManager();
        RegistryAccess access = h.getLevel().registryAccess();
        for (String type : SPARTAN_TYPES) {
            for (Map.Entry<String, String> metal : SPARTAN_METALS.entrySet()) {
                String name = metal.getKey() + "_" + type;
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", name));
                h.assertTrue(item != null && item != Items.AIR, "Spartan item exists: " + name);
                List<SmithingRecipe> recipes = data.recipesProducing(item);
                h.assertTrue(recipes.size() == 1 && recipes.get(0).getId().equals(OtesSmithing.id("compat/spartanweaponry/" + name)),
                        "One dedicated recipe for " + name + ", got " + recipes.stream().map(SmithingRecipe::getId).toList());
                SmithingRecipe r = recipes.get(0);
                h.assertTrue(r.family().toString().equals(metal.getValue()) && r.metalUnits() > 0 && r.metalUnits() % 9 == 0, "Family and units of " + name);
                h.assertTrue(r.explicitAnvilPattern() != null && r.explicitAnvilPattern().equals(OtesSmithing.id(type)), "Dedicated anvil pattern for " + name);
                for (Recipe<?> other : rm.getRecipes()) {
                    if (other instanceof SmithingRecipe) continue;
                    ItemStack out;
                    try {
                        out = other.getResultItem(access);
                    } catch (RuntimeException e) {
                        continue;
                    }
                    h.assertTrue(!out.is(item), "Conventional recipe " + other.getId() + " for " + name + " must be disabled");
                }
            }
        }
        expect(h, data, "iron_longsword", 36, "spartanweaponry:handle", 1);
        expect(h, data, "netherite_longsword", 36, "spartanweaponry:handle", 1);
        expect(h, data, "iron_javelin", 9, "spartanweaponry:pole", 1);
        expect(h, data, "studded_club", 9, null, 0);
        expect(h, data, "studded_cestus", 9, null, 0);
        h.assertTrue(rm.byKey(new ResourceLocation("spartanweaponry", "iron_arrow")).isPresent(), "Spartan arrows stay craftable");
        Item longsword = ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "iron_longsword"));
        h.assertTrue(EquipmentClassifier.classify(new ItemStack(longsword)).equals(OtesSmithing.id("longsword")), "Longsword pattern by tag");
        h.assertTrue(near(SpartanChecks.thrownJavelinDamage(h, new QualityData(50, 100, false)), 4.6), "Masterwork javelin: 4.0 x 1.15");
        h.assertTrue(near(SpartanChecks.thrownJavelinDamage(h, null), 4.0), "Javelin without quality is untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spartanShieldsAreSmithed(GameTestHelper h) {
        if (!ModList.get().isLoaded("spartanshields")) {
            h.succeed();
            return;
        }
        SmithingData data = SmithingData.current();
        SmithingRecipe basic = only(h, data, "spartanshields:iron_basic_shield");
        h.assertTrue(basic.family().toString().equals("minecraft:iron") && basic.metalUnits() == 36 && basic.auxiliary().isEmpty(),
                "Iron basic shield: 36 iron, base shield dismissed, got " + basic.metalUnits() + " " + basic.auxiliary().size());
        SmithingRecipe tower = only(h, data, "spartanshields:iron_tower_shield");
        h.assertTrue(tower.metalUnits() == 54 && tower.auxiliary().isEmpty(), "Iron tower shield: 54 iron, got " + tower.metalUnits());
        SmithingRecipe netherite = only(h, data, "spartanshields:netherite_basic_shield");
        h.assertTrue(netherite.isAuto() && netherite.family().toString().equals("minecraft:netherite") && netherite.metalUnits() == 36
                        && netherite.auxiliary().isEmpty(), "Netherite basic shield upgrade: 36 netherite, got " + netherite.metalUnits());
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(netherite.sourceRecipe()).isEmpty(), "Smithing-table upgrade disabled");
        SmithingRecipe netheriteTower = only(h, data, "spartanshields:netherite_tower_shield");
        h.assertTrue(netheriteTower.metalUnits() == 54, "Netherite tower shield: 54 netherite, got " + netheriteTower.metalUnits());
        List<String> baseSkips = data.report().skippedEquipment().stream()
                .filter(s -> s.startsWith("spartanshields:") && s.contains("uses equipment as an ingredient")).toList();
        h.assertTrue(baseSkips.isEmpty(), "No Spartan Shields shield is skipped for its base shield: " + baseSkips);
        h.succeed();
    }

    private static SmithingRecipe only(GameTestHelper h, SmithingData data, String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        List<SmithingRecipe> recipes = data.recipesProducing(item);
        h.assertTrue(recipes.size() == 1, "One smithing recipe for " + id + ", got " + recipes.size());
        return recipes.get(0);
    }

    private static void expect(GameTestHelper h, SmithingData data, String name, int units, String auxItem, int auxCount) {
        SmithingRecipe r = only(h, data, "spartanweaponry:" + name);
        h.assertTrue(r.metalUnits() == units, name + " units " + r.metalUnits() + " != " + units);
        if (auxItem == null) {
            h.assertTrue(r.auxiliary().isEmpty(), name + " has no auxiliary ingredients");
        } else {
            Item aux = ForgeRegistries.ITEMS.getValue(new ResourceLocation(auxItem));
            h.assertTrue(r.auxiliary().stream().anyMatch(a -> a.count() == auxCount && a.ingredient().test(new ItemStack(aux))),
                    name + " needs " + auxCount + " " + auxItem);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }

    /** Spartan Weaponry types, loaded only after the mod-loaded check. */
    private static final class SpartanChecks {
        static double thrownJavelinDamage(GameTestHelper h, QualityData quality) {
            ItemStack javelin = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("spartanweaponry", "iron_javelin")));
            if (quality != null) quality.apply(javelin);
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("spartanweaponry", "javelin"));
            ThrowingWeaponEntity thrown = (ThrowingWeaponEntity) type.create(h.getLevel());
            thrown.setWeapon(javelin);
            thrown.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
            thrown.setBaseDamage(4.0); // as ThrowingWeaponItem#releaseUsing does, before the entity joins the level
            h.getLevel().addFreshEntity(thrown);
            double damage = thrown.getBaseDamage();
            thrown.discard();
            return damage;
        }
    }
}
