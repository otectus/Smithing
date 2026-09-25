package com.otectus.immersivesmithing.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.material.MaterialFamily;
import com.otectus.immersivesmithing.material.UpgradePolicy;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.recipe.AuxInventory;
import com.otectus.immersivesmithing.recipe.AuxiliaryIngredient;
import com.otectus.immersivesmithing.recipe.SmithingData;
import com.otectus.immersivesmithing.recipe.SmithingRecipe;
import com.otectus.immersivesmithing.recipe.SmithingRecipeSerializer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * The rules added for modpack integration: consumed-equipment auxiliaries, material upgrade policies, upgrade
 * chains, the predominance rule and report hygiene. The test datapack lives in
 * {@code src/gameTest/resources/data/immersive_smithing_test}.
 */
@GameTestHolder(ImmersiveSmithing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class IntegrationGameTests {
    private static final ResourceLocation EMERALDINE = new ResourceLocation("immersive_smithing_test", "emeraldine");
    private static final ResourceLocation LAPISINE = new ResourceLocation("immersive_smithing_test", "lapisine");

    private static SmithingRecipe only(GameTestHelper h, net.minecraft.world.item.Item item) {
        List<SmithingRecipe> recipes = SmithingData.current().recipesProducing(item);
        h.assertTrue(recipes.size() == 1, "Exactly one smithing recipe for " + item + ", found " + recipes.size());
        return recipes.get(0);
    }

    private static boolean skipped(String item, String reason) {
        return SmithingData.current().report().skippedEquipment().stream().anyMatch(l -> l.startsWith(item + " ") && l.endsWith(reason));
    }

    @GameTest(template = "empty")
    public static void materialUpgradePolicyParses(GameTestHelper h) {
        MaterialFamily emeraldine = SmithingData.current().materials().family(EMERALDINE).orElseThrow();
        MaterialFamily lapisine = SmithingData.current().materials().family(LAPISINE).orElseThrow();
        MaterialFamily iron = SmithingData.current().materials().family(new ResourceLocation("minecraft", "iron")).orElseThrow();
        h.assertTrue(emeraldine.upgradePolicy() == UpgradePolicy.ADDITION, "Emeraldine uses the addition policy");
        h.assertTrue(lapisine.upgradePolicy() == UpgradePolicy.SHAPE, "Lapisine uses the shape policy");
        h.assertTrue(iron.upgradePolicy() == UpgradePolicy.SHAPE, "Shape is the default");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void consumedEquipmentAuxiliaryParsesAndMatches(GameTestHelper h) {
        JsonObject json = JsonParser.parseString("""
                {"type": "immersive_smithing:smithing", "material": "minecraft:iron", "metal_units": 9,
                 "auxiliary": [{"ingredient": {"item": "minecraft:netherite_helmet"}, "consume_equipment": true},
                               {"ingredient": {"item": "minecraft:stick"}, "count": 2}],
                 "result": {"item": "minecraft:iron_helmet"}}""").getAsJsonObject();
        SmithingRecipe recipe = new SmithingRecipeSerializer().fromJson(ImmersiveSmithing.id("test_consume"), json);
        h.assertTrue(recipe.auxiliary().size() == 2, "Two auxiliaries");
        AuxiliaryIngredient base = recipe.auxiliary().get(0);
        h.assertTrue(base.consumesEquipment() && base.count() == 1, "The helmet is a consumed piece of equipment");
        h.assertTrue(!recipe.auxiliary().get(1).consumesEquipment(), "Sticks are ordinary components");

        FakePlayer player = TestSupport.player(h);
        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        helmet.setDamageValue(40);
        new QualityData(80, 20, false).apply(helmet);
        player.getInventory().setItem(3, helmet);
        player.getInventory().setItem(5, new ItemStack(Items.STICK, 4));
        h.assertTrue(AuxInventory.hasAll(player, recipe.auxiliary(), 0), "A damaged, graded helmet still matches by item");
        List<ItemStack> taken = AuxInventory.take(player, recipe.auxiliary(), 0);
        h.assertTrue(taken != null && taken.size() == 2, "Both auxiliaries were taken");
        ItemStack escrowed = taken.stream().filter(s -> s.is(Items.NETHERITE_HELMET)).findFirst().orElseThrow();
        h.assertTrue(escrowed.getDamageValue() == 40 && QualityData.get(escrowed).orElseThrow().forgeScore() == 80,
                "The escrowed piece keeps its damage and quality so a cancel refunds it intact");
        h.assertTrue(player.getInventory().getItem(3).isEmpty() && player.getInventory().getItem(5).getCount() == 2, "Inventory debited");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void unpricedUpgradeBaseUsesAddition(GameTestHelper h) {
        SmithingRecipe recipe = only(h, Items.TURTLE_HELMET);
        h.assertTrue(recipe.isAuto() && recipe.family().equals(EMERALDINE) && recipe.metalUnits() == 9,
                "One emerald's worth of metal, got " + recipe.family() + " " + recipe.metalUnits());
        h.assertTrue(recipe.auxiliary().size() == 1 && recipe.auxiliary().get(0).consumesEquipment()
                && recipe.auxiliary().get(0).ingredient().test(new ItemStack(Items.CHAINMAIL_HELMET)), "The chainmail helmet is reworked");
        RecipeManager rm = h.getLevel().getServer().getRecipeManager();
        h.assertTrue(rm.byKey(new ResourceLocation("immersive_smithing_test", "unpriced_base_upgrade")).isEmpty(), "The table upgrade is redirected");
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "turtle_helmet")).isPresent(), "The scute recipe has no metal and stays");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingChainAdditionPolicy(GameTestHelper h) {
        SmithingRecipe recipe = only(h, Items.CHAINMAIL_CHESTPLATE);
        h.assertTrue(recipe.isAuto() && recipe.family().equals(EMERALDINE) && recipe.metalUnits() == 18,
                "Two emeralds' worth, got " + recipe.family() + " " + recipe.metalUnits());
        AuxiliaryIngredient base = recipe.auxiliary().stream().filter(AuxiliaryIngredient::consumesEquipment).findFirst().orElseThrow();
        h.assertTrue(base.ingredient().test(new ItemStack(Items.IRON_CHESTPLATE)), "The iron chestplate is reworked");
        AuxiliaryIngredient stick = recipe.auxiliary().stream().filter(a -> !a.consumesEquipment()).findFirst().orElseThrow();
        h.assertTrue(stick.count() == 1 && stick.ingredient().test(new ItemStack(Items.STICK)), "The stick stays a component");
        RecipeManager rm = h.getLevel().getServer().getRecipeManager();
        ResourceLocation source = new ResourceLocation("immersive_smithing_test", "chain_addition");
        h.assertTrue(source.equals(recipe.sourceRecipe()) && rm.byKey(source).isEmpty(), "The crafting chain is disabled");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingChainShapePolicy(GameTestHelper h) {
        SmithingRecipe recipe = only(h, Items.CHAINMAIL_LEGGINGS);
        h.assertTrue(recipe.family().equals(LAPISINE) && recipe.metalUnits() == 63, "The leggings' shape in lapisine, got " + recipe.metalUnits());
        h.assertTrue(recipe.auxiliary().stream().noneMatch(AuxiliaryIngredient::consumesEquipment), "Shape policy needs no base piece");
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(new ResourceLocation("immersive_smithing_test", "chain_shape")).isEmpty(),
                "The crafting chain is disabled");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void predominanceRuleSkipsTrinkets(GameTestHelper h) {
        h.assertTrue(SmithingData.current().recipesProducing(Items.LEATHER_HELMET).isEmpty(), "A nugget on leather is not smithing");
        h.assertTrue(skipped("minecraft:leather_helmet", "not predominantly metal"), "Skip reason is reported");
        h.assertTrue(h.getLevel().getServer().getRecipeManager().byKey(new ResourceLocation("immersive_smithing_test", "predominance")).isPresent(),
                "The crafting recipe is left alone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void reportIgnoresWorkWithoutMetal(GameTestHelper h) {
        h.assertTrue(SmithingData.current().recipesProducing(Items.STONE_SWORD).isEmpty(), "Stone stays stone");
        h.assertTrue(SmithingData.current().report().skippedEquipment().stream().noneMatch(l -> l.startsWith("minecraft:stone_sword ")),
                "Reworking a wooden sword with bone is not the forge's business");
        h.assertTrue(SmithingData.current().recipesProducing(Items.CHAINMAIL_BOOTS).isEmpty(), "A pickaxe cannot become boots");
        h.assertTrue(skipped("minecraft:chainmail_boots", "uses equipment as an ingredient"), "Equipment of another class is still reported");
        h.succeed();
    }
}
