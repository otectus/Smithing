package com.otectus.immersivesmithing.test;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.material.MaterialFamily;
import com.otectus.immersivesmithing.material.MaterialResolver;
import com.otectus.immersivesmithing.material.RecyclingValue;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import com.otectus.immersivesmithing.quality.QualityCalculator;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.quality.SmithingQuality;
import com.otectus.immersivesmithing.recipe.AutoRecipeDetector;
import com.otectus.immersivesmithing.recipe.SmithingData;
import com.otectus.immersivesmithing.recipe.SmithingRecipe;
import com.otectus.immersivesmithing.registry.ModTags;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

/** Materials, recipe replacement, auto-detection and quality maths against the real loaded data. */
@GameTestHolder(ImmersiveSmithing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataGameTests {
    private static final ResourceLocation IRON = new ResourceLocation("minecraft", "iron");

    private static void resolves(GameTestHelper h, ItemStack stack, String family, int units) {
        Optional<MaterialResolver.Resolved> r = MaterialResolver.resolve(stack);
        h.assertTrue(r.isPresent(), stack + " should be a smithing material");
        h.assertTrue(r.get().family().id().toString().equals(family), stack + " family " + r.get().family().id() + " != " + family);
        h.assertTrue(r.get().unitsEach() == units, stack + " units " + r.get().unitsEach() + " != " + units);
    }

    @GameTest(template = "empty")
    public static void materialSourcesResolve(GameTestHelper h) {
        resolves(h, new ItemStack(Items.IRON_INGOT), "minecraft:iron", 9);
        resolves(h, new ItemStack(Items.IRON_NUGGET), "minecraft:iron", 1);
        resolves(h, new ItemStack(Items.IRON_BLOCK), "minecraft:iron", 81);
        resolves(h, new ItemStack(Items.RAW_IRON), "minecraft:iron", 9);
        resolves(h, new ItemStack(Items.RAW_IRON_BLOCK), "minecraft:iron", 81);
        resolves(h, new ItemStack(Items.GOLD_INGOT), "minecraft:gold", 9);
        resolves(h, new ItemStack(Items.NETHERITE_INGOT), "minecraft:netherite", 9);
        resolves(h, new ItemStack(Items.COPPER_INGOT), "minecraft:copper", 9);
        h.assertTrue(MaterialResolver.resolve(new ItemStack(Items.DIAMOND)).isEmpty(), "Diamond is not a metal");
        h.assertTrue(MaterialResolver.resolve(new ItemStack(Items.BRICK)).isEmpty(), "forge:ingots/brick is excluded");
        MaterialFamily netherite = SmithingData.current().materials().family(new ResourceLocation("minecraft", "netherite")).orElseThrow();
        h.assertTrue(ModTags.NETHERITE_FUELS.equals(netherite.requiredFuelTag()), "Netherite requires the netherite fuel tag");
        h.assertTrue(!netherite.requiresIgnition(), "Netherite needs no ignition");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recyclingValuesNeverExceedRecipes(GameTestHelper h) {
        RecyclingValue sword = MaterialResolver.recyclingValue(new ItemStack(Items.IRON_SWORD)).orElseThrow();
        h.assertTrue(sword.family().equals(IRON) && sword.units() == 18, "Iron sword recycles to 18 iron units");
        RecyclingValue chest = MaterialResolver.recyclingValue(new ItemStack(Items.IRON_CHESTPLATE)).orElseThrow();
        h.assertTrue(chest.units() == 72, "Iron chestplate recycles to 72 units");
        h.assertTrue(MaterialResolver.recyclingValue(new ItemStack(Items.NETHERITE_SWORD)).orElseThrow().units() == 18, "Netherite sword");
        h.assertTrue(MaterialResolver.recyclingValue(new ItemStack(Items.DIAMOND_SWORD)).isEmpty(), "Diamond sword is not metal");
        h.assertTrue(MaterialResolver.recyclingValue(new ItemStack(Items.WOODEN_SWORD)).isEmpty(), "Wooden sword is not metal");
        ItemStack enchanted = new ItemStack(Items.IRON_SWORD);
        new QualityData(90, 90, false).apply(enchanted);
        h.assertTrue(MaterialResolver.resolve(enchanted).orElseThrow().unitsEach() == 18, "Quality does not change recycled units");
        h.assertTrue(MaterialResolver.recycledUnits(18) == 18, "Default efficiency recovers everything");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void conventionalRecipesReplaced(GameTestHelper h) {
        RecipeManager rm = h.getLevel().getServer().getRecipeManager();
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "iron_sword")).isEmpty(), "Iron sword crafting is disabled");
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "golden_chestplate")).isEmpty(), "Golden chestplate crafting is disabled");
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "netherite_sword_smithing")).isEmpty(), "Netherite upgrade is disabled");
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "diamond_sword")).isPresent(), "Diamond sword crafting is untouched");
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "shield")).isPresent(), "Wooden shield crafting is untouched");
        h.assertTrue(rm.byKey(new ResourceLocation("minecraft", "iron_ingot_from_iron_block")).isPresent(), "Material recipes are untouched");
        h.assertTrue(SmithingData.current().suppressedRecipes().containsKey(new ResourceLocation("minecraft", "iron_sword")), "Suppression is recorded");
        long trims = rm.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.SMITHING).stream()
                .filter(r -> r instanceof net.minecraft.world.item.crafting.SmithingTrimRecipe).count();
        h.assertTrue(trims == 16, "Armor trim recipes are untouched, found " + trims);
        long vanilla = SmithingData.current().suppressedRecipes().keySet().stream().filter(k -> k.getNamespace().equals("minecraft")).count();
        h.assertTrue(vanilla == 28, "18 tool/armor + shears + 9 upgrades disabled, got " + vanilla);

        List<SmithingRecipe> sword = SmithingData.current().recipesProducing(Items.IRON_SWORD);
        h.assertTrue(sword.size() == 1 && !sword.get(0).isAuto(), "Exactly one explicit iron sword recipe");
        SmithingRecipe r = sword.get(0);
        h.assertTrue(r.metalUnits() == 18 && r.auxiliary().size() == 1 && r.auxiliary().get(0).count() == 1, "18 units and one stick");
        h.assertTrue(rm.byKey(r.getId()).isPresent(), "Smithing recipes are in the recipe manager (synced to clients)");
        h.assertTrue(SmithingData.current().recipesProducing(Items.DIAMOND_SWORD).isEmpty(), "No smithing for diamond");
        h.assertTrue(SmithingData.current().recipesProducing(Items.SHIELD).isEmpty(), "Vanilla shield is not redirected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeAnalysisIsConservative(GameTestHelper h) {
        var materials = SmithingData.current().materials();
        ResourceLocation id = ImmersiveSmithing.id("analysis");
        ItemStack out = new ItemStack(Items.IRON_SWORD);
        Ingredient iron = Ingredient.of(Items.IRON_INGOT);
        Ingredient stick = Ingredient.of(Items.STICK);

        var ok = AutoRecipeDetector.analyze(id, List.of(iron, iron, stick, Ingredient.EMPTY), out, materials);
        h.assertTrue(ok.status() == AutoRecipeDetector.Status.OK && ok.units() == 18 && ok.family().equals(IRON), "Metal plus stick");
        h.assertTrue(ok.auxiliary().size() == 1 && ok.auxiliary().get(0).count() == 1, "Stick becomes one auxiliary ingredient");
        var multi = AutoRecipeDetector.analyze(id, List.of(iron, Ingredient.of(Items.GOLD_INGOT)), out, materials);
        h.assertTrue(multi.status() == AutoRecipeDetector.Status.MULTI_METAL && multi.hasMetal(), "Two metals are skipped");
        var mixed = AutoRecipeDetector.analyze(id, List.of(Ingredient.of(Items.IRON_INGOT, Items.STICK)), out, materials);
        h.assertTrue(mixed.status() == AutoRecipeDetector.Status.MIXED_INGREDIENT, "Ingredient mixing metal and wood is skipped");
        var none = AutoRecipeDetector.analyze(id, List.of(stick, Ingredient.of(Items.DIAMOND)), out, materials);
        h.assertTrue(none.status() == AutoRecipeDetector.Status.NOT_METAL, "No metal, nothing generated");
        var equipment = AutoRecipeDetector.analyze(id, List.of(iron, Ingredient.of(Items.DIAMOND_SWORD)), out, materials);
        h.assertTrue(equipment.status() == AutoRecipeDetector.Status.EQUIPMENT_INGREDIENT, "Equipment ingredients are skipped");

        h.assertTrue(EquipmentClassifier.isCandidate(new ItemStack(Items.IRON_PICKAXE)), "Pickaxe is candidate equipment");
        h.assertTrue(!EquipmentClassifier.isCandidate(new ItemStack(Items.CROSSBOW)), "Crossbow is not");
        h.assertTrue(!EquipmentClassifier.isCandidate(new ItemStack(Items.SHIELD)), "Vanilla shield is excluded by tag");
        h.assertTrue(EquipmentClassifier.classify(new ItemStack(Items.IRON_AXE)).equals(EquipmentClassifier.AXE), "Axe pattern");
        h.assertTrue(EquipmentClassifier.classify(new ItemStack(Items.IRON_LEGGINGS)).equals(EquipmentClassifier.LEGGINGS), "Leggings pattern");
        h.assertTrue(EquipmentClassifier.classify(new ItemStack(Items.SHEARS)).equals(EquipmentClassifier.GENERIC_TOOL), "Shears pattern");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void qualityBandsAndMultipliers(GameTestHelper h) {
        int[][] bands = {{0, 1}, {34, 1}, {35, 2}, {50, 2}, {69, 2}, {70, 3}, {89, 3}, {90, 4}, {100, 4}};
        for (int[] b : bands) {
            h.assertTrue(SmithingQuality.fromScore(b[0]).ordinal() == b[1], "Score " + b[0] + " band");
        }
        h.assertTrue(SmithingQuality.overall(100, 100, true) == SmithingQuality.FAULTY, "Faulty overrides");
        h.assertTrue(near(QualityCalculator.durabilityMultiplier(new QualityData(0, 0, false)), 0.75), "Durability at 0");
        h.assertTrue(near(QualityCalculator.durabilityMultiplier(new QualityData(50, 0, false)), 1.0), "Durability at 50");
        h.assertTrue(near(QualityCalculator.durabilityMultiplier(new QualityData(100, 0, false)), 1.25), "Durability at 100");
        h.assertTrue(near(QualityCalculator.durabilityMultiplier(new QualityData(100, 100, true)), 0.5), "Faulty durability");
        h.assertTrue(near(QualityCalculator.efficacyMultiplier(new QualityData(0, 0, false)), 0.85), "Efficacy at 0");
        h.assertTrue(near(QualityCalculator.efficacyMultiplier(new QualityData(0, 50, false)), 1.0), "Efficacy at 50");
        h.assertTrue(near(QualityCalculator.efficacyMultiplier(new QualityData(0, 100, false)), 1.15), "Efficacy at 100");
        h.assertTrue(near(QualityCalculator.efficacyMultiplier(new QualityData(100, 100, true)), 0.7), "Faulty efficacy");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void durabilityIsConvertedPerDamage(GameTestHelper h) {
        RandomSource random = RandomSource.create(1);
        ItemStack master = new ItemStack(Items.IRON_SWORD);
        new QualityData(100, 50, false).apply(master);
        for (int i = 0; i < 100; i++) master.hurt(1, random, null);
        h.assertTrue(master.getDamageValue() == 80, "125% durability: 100 hits cost 80, got " + master.getDamageValue());

        ItemStack faulty = new ItemStack(Items.IRON_SWORD);
        new QualityData(100, 100, true).apply(faulty);
        for (int i = 0; i < 100; i++) faulty.hurt(1, random, null);
        h.assertTrue(faulty.getDamageValue() == 200, "Faulty: 100 hits cost 200, got " + faulty.getDamageValue());

        ItemStack standard = new ItemStack(Items.IRON_SWORD);
        new QualityData(50, 50, false).apply(standard);
        for (int i = 0; i < 37; i++) standard.hurt(1, random, null);
        h.assertTrue(standard.getDamageValue() == 37, "Score 50 behaves like an ordinary item");

        ItemStack plain = new ItemStack(Items.IRON_SWORD);
        for (int i = 0; i < 10; i++) plain.hurt(1, random, null);
        h.assertTrue(plain.getDamageValue() == 10, "Items without quality are untouched");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void efficacyAddsAttributeDifferences(GameTestHelper h) {
        h.assertTrue(near(attack(new QualityData(50, 50, false), Items.IRON_SWORD), 5.0), "Score 50 sword is vanilla");
        h.assertTrue(near(attack(new QualityData(50, 100, false), Items.IRON_SWORD), 5.9), "Masterwork sword +15% of 6 damage");
        h.assertTrue(near(attack(new QualityData(50, 0, false), Items.IRON_SWORD), 4.1), "Crude sword -15%");
        h.assertTrue(near(attack(new QualityData(50, 100, false), Items.IRON_PICKAXE), 3.3), "Tools apply half the deviation to damage");
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        new QualityData(50, 100, false).apply(chest);
        double armor = chest.getAttributeModifiers(EquipmentSlot.CHEST).get(Attributes.ARMOR).stream().mapToDouble(AttributeModifier::getAmount).sum();
        h.assertTrue(near(armor, 6.9), "Armor 6 x 1.15, got " + armor);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void metalShieldBaseIsDismissed(GameTestHelper h) {
        var materials = SmithingData.current().materials();
        Ingredient iron = Ingredient.of(Items.IRON_INGOT);
        Ingredient base = Ingredient.of(Items.SHIELD);
        var shield = AutoRecipeDetector.analyze(ImmersiveSmithing.id("shield_analysis"), List.of(iron, iron, base, iron, iron), new ItemStack(Items.SHIELD), materials);
        h.assertTrue(shield.status() == AutoRecipeDetector.Status.OK && shield.units() == 36, "Base shield dismissed: " + shield.status() + " " + shield.units());
        h.assertTrue(shield.auxiliary().isEmpty(), "The base shield is not an auxiliary ingredient");
        var sword = AutoRecipeDetector.analyze(ImmersiveSmithing.id("sword_analysis"), List.of(iron, base), new ItemStack(Items.IRON_SWORD), materials);
        h.assertTrue(sword.status() == AutoRecipeDetector.Status.EQUIPMENT_INGREDIENT, "Only shields dismiss a base shield");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void smithingTableUpgradesBecomeForgeRecipes(GameTestHelper h) {
        var server = h.getLevel().getServer();
        SmithingData data = SmithingData.current();
        var report = new com.otectus.immersivesmithing.recipe.CompatibilityReport();
        // Base without a smithing recipe: the diamond sword's crafting shape in netherite (2 diamonds -> 18 units).
        var context = new AutoRecipeDetector.UpgradeContext(server.getRecipeManager(), server.registryAccess(), data.materials(), java.util.Map.of());
        var netherite = new SmithingTransformRecipe(ImmersiveSmithing.id("test_netherite_upgrade"), Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                Ingredient.of(Items.DIAMOND_SWORD), Ingredient.of(Items.NETHERITE_INGOT), new ItemStack(Items.NETHERITE_SWORD));
        SmithingRecipe up = AutoRecipeDetector.upgradeRecipe(netherite, new ItemStack(Items.NETHERITE_SWORD), context, report).orElseThrow();
        h.assertTrue(up.family().toString().equals("minecraft:netherite") && up.metalUnits() == 18, "Netherite 18 units, got " + up.family() + " " + up.metalUnits());
        h.assertTrue(up.auxiliary().size() == 1 && up.auxiliary().get(0).count() == 1 && up.auxiliary().get(0).ingredient().test(new ItemStack(Items.STICK)), "One stick");
        h.assertTrue(up.isAuto() && netherite.getId().equals(up.sourceRecipe()), "Automatic, sourced from the upgrade recipe");
        // Base with a smithing recipe: its shape in the addition's metal (iron sword 18 + stick -> 18 gold + stick).
        SmithingRecipe ironSword = data.recipesProducing(Items.IRON_SWORD).get(0);
        var smithable = new AutoRecipeDetector.UpgradeContext(server.getRecipeManager(), server.registryAccess(), data.materials(), java.util.Map.of(Items.IRON_SWORD, ironSword));
        var gilded = new SmithingTransformRecipe(ImmersiveSmithing.id("test_gold_upgrade"), Ingredient.EMPTY,
                Ingredient.of(Items.IRON_SWORD), Ingredient.of(Items.GOLD_INGOT), new ItemStack(Items.GOLDEN_SWORD));
        SmithingRecipe gold = AutoRecipeDetector.upgradeRecipe(gilded, new ItemStack(Items.GOLDEN_SWORD), smithable, report).orElseThrow();
        h.assertTrue(gold.family().toString().equals("minecraft:gold") && gold.metalUnits() == 18 && gold.auxiliary().size() == 1, "Gold 18 units and the stick");
        var diamond = new SmithingTransformRecipe(ImmersiveSmithing.id("test_diamond_upgrade"), Ingredient.EMPTY,
                Ingredient.of(Items.IRON_SWORD), Ingredient.of(Items.DIAMOND), new ItemStack(Items.DIAMOND_SWORD));
        h.assertTrue(AutoRecipeDetector.upgradeRecipe(diamond, new ItemStack(Items.DIAMOND_SWORD), smithable, report).isEmpty(), "Non-metal additions stay on the table");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void spartanRecipesInertWithoutSpartan(GameTestHelper h) {
        if (!ModList.get().isLoaded("spartanweaponry")) {
            RecipeManager rm = h.getLevel().getServer().getRecipeManager();
            h.assertTrue(rm.byKey(ImmersiveSmithing.id("compat/spartanweaponry/iron_longsword")).isEmpty(), "Conditioned out without Spartan Weaponry");
            long compat = SmithingData.current().recipes().stream().filter(r -> r.getId().getPath().startsWith("compat/")).count();
            h.assertTrue(compat == 0, "No compat recipes without their mod, found " + compat);
            h.assertTrue(SmithingData.current().materials().family(new ResourceLocation("forge", "steel")).isEmpty(), "Modded metal families need their ingots");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void arrowsCarryBowQuality(GameTestHelper h) {
        var player = TestSupport.player(h);
        h.assertTrue(near(shoot(h, player, new QualityData(50, 100, false)), 2.3), "Masterwork bow: 2.0 x 1.15");
        h.assertTrue(near(shoot(h, player, new QualityData(50, 0, false)), 1.7), "Crude bow: 2.0 x 0.85");
        h.assertTrue(near(shoot(h, player, null), 2.0), "Bow without quality is untouched");
        h.succeed();
    }

    private static double shoot(GameTestHelper h, net.minecraftforge.common.util.FakePlayer player, QualityData quality) {
        ItemStack bow = new ItemStack(Items.BOW);
        if (quality != null) quality.apply(bow);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, bow);
        Arrow arrow = new Arrow(h.getLevel(), player);
        h.getLevel().addFreshEntity(arrow);
        double damage = arrow.getBaseDamage();
        arrow.discard();
        return damage;
    }

    private static double attack(QualityData quality, net.minecraft.world.item.Item item) {
        ItemStack stack = new ItemStack(item);
        quality.apply(stack);
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(AttributeModifier::getAmount).sum();
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-4;
    }
}
