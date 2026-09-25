package com.otectus.immersivesmithing.test;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.api.event.ItemSmithedEvent;
import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.loot.ForgedLootModifier;
import com.otectus.immersivesmithing.quality.MakersMark;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.quality.RandomQuality;
import com.otectus.immersivesmithing.quality.SigningService;
import com.otectus.immersivesmithing.quality.SmithingQuality;
import com.otectus.immersivesmithing.registry.ModBlocks;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.registry.ModLootModifiers;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import com.otectus.immersivesmithing.workpiece.WorkpieceState;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Loot quality, quality inheritance, the maker's mark and the smithed event. */
@GameTestHolder(ImmersiveSmithing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ProgressionGameTests {

    private static ItemStack find(FakePlayer player, net.minecraft.world.item.Item item) {
        for (ItemStack s : player.getInventory().items) {
            if (s.is(item)) return s;
        }
        return ItemStack.EMPTY;
    }

    @GameTest(template = "empty")
    public static void lootGetsStandardQuality(GameTestHelper h) {
        h.assertTrue(ModLootModifiers.FORGED_LOOT.isPresent(), "Loot modifier codec registered");
        h.assertTrue(ServerConfig.get(ServerConfig.LOOT_QUALITY_MODE) == ServerConfig.LootQualityMode.STANDARD, "Standard is the default");
        LootParams params = new LootParams.Builder(h.getLevel()).create(LootContextParamSets.EMPTY);
        LootContext context = new LootContext.Builder(params).withQueriedLootTableId(new ResourceLocation("minecraft", "chests/simple_dungeon")).create(null);
        ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
        loot.add(new ItemStack(Items.IRON_SWORD));
        loot.add(new ItemStack(Items.DIAMOND_SWORD));
        loot.add(new ItemStack(Items.IRON_INGOT, 3));
        ItemStack graded = new ItemStack(Items.IRON_HELMET);
        new QualityData(95, 95, false).apply(graded);
        loot.add(graded);
        new ForgedLootModifier(new LootItemCondition[0]).apply(loot, context);
        QualityData sword = QualityData.get(loot.get(0)).orElse(null);
        h.assertTrue(sword != null && sword.quality() == SmithingQuality.STANDARD, "Found iron sword is graded Standard");
        h.assertTrue(sword.forgeScore() >= 35 && sword.forgeScore() < 70 && sword.anvilScore() >= 35 && sword.anvilScore() < 70, "Scores stay inside the Standard band");
        h.assertTrue(!QualityData.has(loot.get(1)), "Diamond is not forge work");
        h.assertTrue(!QualityData.has(loot.get(2)), "Ingots are not equipment");
        h.assertTrue(QualityData.get(loot.get(3)).orElseThrow().forgeScore() == 95, "Already graded loot is left alone");
        for (int i = 0; i < 200; i++) {
            QualityData q = RandomQuality.standard(RandomSource.create(i));
            h.assertTrue(q.quality() == SmithingQuality.STANDARD && !q.faulty(), "Standard roll never leaves the band");
        }
        h.assertTrue(!ForgedLootModifier.isExcluded(new ResourceLocation("minecraft", "chests/simple_dungeon")), "Nothing excluded by default");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingInheritsQualityAndMark(GameTestHelper h) {
        FakePlayer player = TestSupport.player(h);
        ItemStack base = new ItemStack(Items.IRON_SWORD);
        new QualityData(88, 91, false).apply(base);
        MakersMark.stamp(base, player);
        MakersMark.sign(base, "Oathkeeper", List.of("Sworn at the northern gate"));
        SimpleContainer grid = new SimpleContainer(4);
        grid.setItem(1, base);
        grid.setItem(2, new ItemStack(Items.BLAZE_POWDER));
        ItemStack result = new ItemStack(Items.GOLDEN_SWORD);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, result, grid));
        QualityData q = QualityData.get(result).orElse(null);
        h.assertTrue(q != null && q.forgeScore() == 88 && q.anvilScore() == 91, "The result keeps the scores");
        MakersMark mark = MakersMark.get(result).orElseThrow();
        h.assertTrue(mark.signed() && mark.smithId().equals(player.getUUID()), "The mark travels with the piece");
        h.assertTrue(result.getHoverName().getString().equals("Oathkeeper") && MakersMark.inscription(result).get(0).startsWith("Sworn"), "Title and inscription travel too");

        ItemStack second = new ItemStack(Items.IRON_SWORD);
        new QualityData(20, 20, false).apply(second);
        grid.setItem(3, second);
        ItemStack ambiguous = new ItemStack(Items.GOLDEN_SWORD);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, ambiguous, grid));
        h.assertTrue(!QualityData.has(ambiguous), "Two graded inputs: nothing is inherited");
        ItemStack notEquipment = new ItemStack(Items.IRON_INGOT);
        grid.setItem(3, ItemStack.EMPTY);
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, notEquipment, grid));
        h.assertTrue(!QualityData.has(notEquipment), "Only equipment inherits");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void signingIsValidatedAndSanitised(GameTestHelper h) {
        FakePlayer smith = TestSupport.player(h);
        FakePlayer other = TestSupport.player(h);
        ItemStack piece = new ItemStack(Items.IRON_PICKAXE);
        new QualityData(75, 80, false).apply(piece);
        MakersMark.stamp(piece, smith);
        smith.getInventory().setItem(4, piece);
        h.assertTrue(SigningService.canSign(piece, smith) && !SigningService.canSign(piece, other), "Only the smith may sign");

        SigningService.sign(other, 4, "Stolen", List.of());
        h.assertTrue(!MakersMark.get(smith.getInventory().getItem(4)).orElseThrow().signed(), "Another player's packet does nothing");

        String longTitle = "§lDelver's §kPick" + "x".repeat(100) + "\n\t";
        SigningService.sign(smith, 4, longTitle, List.of("  Dug  from   the deep  ", "", "§4a\u0007b", "four", "five"));
        ItemStack signed = smith.getInventory().getItem(4);
        MakersMark mark = MakersMark.get(signed).orElseThrow();
        h.assertTrue(mark.signed(), "Signed");
        String title = signed.getHoverName().getString();
        h.assertTrue(!title.contains("§") && title.length() <= ServerConfig.get(ServerConfig.MAX_TITLE_LENGTH) && title.startsWith("lDelver's kPick"),
                "Formatting codes stripped and length capped: " + title);
        List<String> lines = MakersMark.inscription(signed);
        h.assertTrue(lines.size() == 3 && lines.get(0).equals("Dug from the deep") && lines.get(1).equals("4ab") && lines.get(2).equals("four"),
                "Whitespace collapsed, empty lines dropped, line count capped: " + lines);
        h.assertTrue(!signed.getHoverName().getStyle().isItalic(), "Titles are not italic like anvil names");
        h.assertTrue(QualityData.get(signed).orElseThrow().anvilScore() == 80, "Quality untouched by signing");

        SigningService.sign(smith, 4, "", List.of());
        ItemStack cleared = smith.getInventory().getItem(4);
        h.assertTrue(!cleared.hasCustomHoverName() && MakersMark.inscription(cleared).isEmpty() && MakersMark.get(cleared).orElseThrow().signed(),
                "Re-signing with nothing restores the item name and clears the inscription");

        ItemStack faulty = new ItemStack(Items.IRON_AXE);
        new QualityData(10, 10, true).apply(faulty);
        MakersMark.stamp(faulty, smith);
        smith.getInventory().setItem(5, faulty);
        SigningService.sign(smith, 5, "Botched", List.of());
        h.assertTrue(!smith.getInventory().getItem(5).hasCustomHoverName(), "Faulty work is not signed");
        SigningService.sign(smith, 99, "Nowhere", List.of());
        SigningService.sign(smith, -1, "Nowhere", List.of());
        h.assertTrue(MakersMark.sanitise(null, 10).isEmpty() && MakersMark.sanitise("abcdefghijkl", 5).equals("abcde"), "Sanitiser edge cases");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void quenchStampsMarkAndPostsEvent(GameTestHelper h) {
        FakePlayer player = TestSupport.player(h);
        h.setBlock(TestSupport.STATION, ModBlocks.SMITHS_TROUGH.get().defaultBlockState());
        SmithsTroughBlockEntity trough = (SmithsTroughBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(TestSupport.STATION));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        TestSupport.use(h, player, 0.5);
        h.assertTrue(trough.water() == 1000, "Trough filled");

        AtomicInteger smithed = new AtomicInteger();
        AtomicInteger crafted = new AtomicInteger();
        Consumer<ItemSmithedEvent> onSmithed = e -> {
            smithed.incrementAndGet();
            h.assertTrue(e.getEntity() == player && e.getItem().is(Items.IRON_SWORD) && e.getQuality().forgeScore() == 80, "Event carries the piece");
            h.assertTrue(e.getFamily().toString().equals("minecraft:iron") && e.getMetalUnits() == 18 && e.getTroughPos().equals(h.absolutePos(TestSupport.STATION)), "Event carries the recipe facts");
            e.getItem().enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 1); // listeners may change the piece
        };
        Consumer<PlayerEvent.ItemCraftedEvent> onCrafted = e -> {
            if (e.getEntity() == player && e.getCrafting().is(Items.IRON_SWORD)) crafted.incrementAndGet();
        };
        MinecraftForge.EVENT_BUS.addListener(onSmithed);
        MinecraftForge.EVENT_BUS.addListener(onCrafted);
        try {
            ItemStack tongs = new ItemStack(ModItems.tongs(SmithingTier.IRON));
            WorkpieceCodec.setHeld(tongs, TestSupport.workpiece(new ItemStack(Items.IRON_SWORD), 80, 95, false, WorkpieceState.SHAPED));
            player.setItemInHand(InteractionHand.MAIN_HAND, tongs);
            TestSupport.use(h, player, 0.5);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(onSmithed);
            MinecraftForge.EVENT_BUS.unregister(onCrafted);
        }
        h.assertTrue(smithed.get() == 1, "ItemSmithedEvent posted exactly once, got " + smithed.get());
        h.assertTrue(crafted.get() == 1, "Vanilla ItemCraftedEvent fired once, got " + crafted.get());
        ItemStack result = find(player, Items.IRON_SWORD);
        MakersMark mark = MakersMark.get(result).orElseThrow();
        h.assertTrue(mark.smithId().equals(player.getUUID()) && mark.smithName().equals("SmithTest") && !mark.signed(), "The piece is stamped, not yet signed");
        h.assertTrue(result.getEnchantmentLevel(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS) == 1, "A listener's change reached the inventory");
        h.assertTrue(QualityData.get(result).orElseThrow().anvilScore() == 95, "Quality intact");
        h.succeed();
    }
}
