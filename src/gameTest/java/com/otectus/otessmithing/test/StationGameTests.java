package com.otectus.otessmithing.test;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.block.SmithsForgeBlock;
import com.otectus.otessmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.otessmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.otessmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.otessmithing.item.SmithingTier;
import com.otectus.otessmithing.minigame.AnvilMinigame;
import com.otectus.otessmithing.minigame.AnvilPattern;
import com.otectus.otessmithing.minigame.AnvilRun;
import com.otectus.otessmithing.minigame.ForgeMinigame;
import com.otectus.otessmithing.minigame.ForgePattern;
import com.otectus.otessmithing.minigame.ForgeRun;
import com.otectus.otessmithing.minigame.ForgeSession;
import com.otectus.otessmithing.minigame.PatternRegistry;
import com.otectus.otessmithing.minigame.SessionManager;
import com.otectus.otessmithing.minigame.SessionTiming;
import com.otectus.otessmithing.minigame.SmithingSession;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.recipe.SmithingData;
import com.otectus.otessmithing.recipe.SmithingRecipe;
import com.otectus.otessmithing.registry.ModBlocks;
import com.otectus.otessmithing.registry.ModItems;
import com.otectus.otessmithing.workpiece.WorkpieceCodec;
import com.otectus.otessmithing.workpiece.WorkpieceData;
import com.otectus.otessmithing.workpiece.WorkpieceState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.otectus.otessmithing.test.TestSupport.STATION;

/** Station behaviour and complete, server-validated minigame sessions driven with a fake player. */
@GameTestHolder(OtesSmithing.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StationGameTests {

    private static SmithsForgeBlockEntity forge(GameTestHelper h) {
        return forgeAt(h, STATION);
    }

    private static SmithsForgeBlockEntity forgeAt(GameTestHelper h, BlockPos pos) {
        h.setBlock(pos, ModBlocks.SMITHS_FORGE.get().defaultBlockState());
        return (SmithsForgeBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void forgeMeltsOneFamily(GameTestHelper h) {
        SmithsForgeBlockEntity forge = forge(h);
        SmithsForgeBlockEntity.Result[] err = new SmithsForgeBlockEntity.Result[1];
        h.assertTrue(forge.insertMetal(new ItemStack(Items.IRON_INGOT, 2), 2, false, err) == 2, "Two ingots accepted");
        h.assertTrue(forge.insertMetal(new ItemStack(Items.RAW_IRON), 1, false, err) == 1, "Raw iron joins the iron batch");
        h.assertTrue(forge.insertMetal(new ItemStack(Items.GOLD_INGOT), 1, false, err) == 0 && err[0] == SmithsForgeBlockEntity.Result.WRONG_FAMILY, "Gold is rejected");
        h.assertTrue(forge.insertFuel(new ItemStack(Items.IRON_INGOT), 1, false, err) == 0, "Metal is not fuel");
        h.assertTrue(forge.ignite() == SmithsForgeBlockEntity.Result.NO_FUEL, "Cannot ignite without fuel");
        h.assertTrue(forge.canExtractDeposits(), "Unlit deposits can be taken back");
        ItemStack back = forge.extractLastDeposit();
        h.assertTrue(back.is(Items.RAW_IRON) && forge.depositUnits() == 18, "Most recent deposit comes back first");
        h.assertTrue(forge.insertFuel(new ItemStack(Items.COAL, 2), 2, false, err) == 2, "Coal accepted");
        h.assertTrue(forge.ignite() == SmithsForgeBlockEntity.Result.OK, "Ignites");
        h.assertTrue(!forge.canExtractDeposits(), "Burning deposits cannot be taken back");
        h.succeedWhen(() -> {
            h.assertTrue(forge.isReady(), "Melted");
            h.assertTrue(forge.moltenUnits() == 18, "Exactly 18 units, got " + forge.moltenUnits());
            h.assertTrue(forge.getBlockState().getValue(SmithsForgeBlock.READY), "READY block state");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500)
    public static void netheriteNeedsLava(GameTestHelper h) {
        SmithsForgeBlockEntity forge = forge(h);
        SmithsForgeBlockEntity.Result[] err = new SmithsForgeBlockEntity.Result[1];
        forge.insertMetal(new ItemStack(Items.NETHERITE_INGOT), 1, false, err);
        forge.insertFuel(new ItemStack(Items.COAL, 4), 4, false, err);
        h.assertTrue(forge.ignite() == SmithsForgeBlockEntity.Result.WRONG_FUEL, "Coal cannot melt netherite");
        h.assertTrue(forge.addLavaBucket(false) == SmithsForgeBlockEntity.Result.SOLID_FUEL_PRESENT, "Lava needs an empty firebox");
        h.assertTrue(forge.extractFuel().getCount() == 4, "Unlit coal comes back");
        h.assertTrue(forge.addLavaBucket(false) == SmithsForgeBlockEntity.Result.OK, "Lava accepted");
        h.assertTrue(forge.ignite() == SmithsForgeBlockEntity.Result.LAVA_NEEDS_NO_IGNITION, "Lava needs no ignition");
        h.succeedWhen(() -> h.assertTrue(forge.isReady() && forge.moltenUnits() == 9, "Netherite melted with lava"));
    }

    @GameTest(template = "empty")
    public static void forgeCapacityAndRecycling(GameTestHelper h) {
        SmithsForgeBlockEntity forge = forge(h);
        SmithsForgeBlockEntity.Result[] err = new SmithsForgeBlockEntity.Result[1];
        int blocks = forge.insertMetal(new ItemStack(Items.IRON_BLOCK, 64), 64, false, err);
        h.assertTrue(blocks == forge.capacity() / 81, "Capacity limits blocks, got " + blocks);
        h.assertTrue(forge.insertMetal(new ItemStack(Items.IRON_BLOCK), 1, false, err) == 0 && err[0] == SmithsForgeBlockEntity.Result.FULL, "Full forge rejects more");

        SmithsForgeBlockEntity other = forgeAt(h, new BlockPos(0, 1, 0));
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS, 3);
        new QualityData(20, 20, true).apply(sword);
        h.assertTrue(other.insertMetal(sword, 1, false, err) == 1 && other.depositUnits() == 18, "Faulty enchanted sword is worth 18 units");
        WorkpieceData wp = TestSupport.workpiece(new ItemStack(Items.IRON_PICKAXE), 40, -1, false, WorkpieceState.FORGED);
        h.assertTrue(other.acceptWorkpiece(wp) == SmithsForgeBlockEntity.Result.OK && other.moltenUnits() == 27, "Workpiece melts back to 27 units");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilAndTroughTransfers(GameTestHelper h) {
        FakePlayer player = TestSupport.player(h);
        h.setBlock(STATION, ModBlocks.SMITHS_ANVIL.get().defaultBlockState());
        SmithsAnvilBlockEntity anvil = (SmithsAnvilBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(STATION));
        ItemStack tongs = new ItemStack(ModItems.tongs(SmithingTier.IRON));
        WorkpieceCodec.setHeld(tongs, TestSupport.workpiece(new ItemStack(Items.IRON_SWORD), 80, -1, false, WorkpieceState.FORGED));
        player.setItemInHand(InteractionHand.MAIN_HAND, tongs);
        TestSupport.use(h, player, 0.9);
        h.assertTrue(anvil.hasWorkpiece() && !WorkpieceCodec.isHolding(tongs), "Tongs place the workpiece on the anvil");
        TestSupport.use(h, player, 0.9);
        h.assertTrue(!anvil.hasWorkpiece() && WorkpieceCodec.isHolding(tongs), "Empty tongs pick it back up");

        h.setBlock(STATION, ModBlocks.SMITHS_TROUGH.get().defaultBlockState());
        SmithsTroughBlockEntity trough = (SmithsTroughBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(STATION));
        TestSupport.use(h, player, 0.5);
        h.assertTrue(WorkpieceCodec.isHolding(tongs), "An empty trough does not consume the workpiece");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        TestSupport.use(h, player, 0.5);
        h.assertTrue(trough.water() == 1000 && player.getMainHandItem().is(Items.BUCKET), "Water bucket fills the trough");
        player.setItemInHand(InteractionHand.MAIN_HAND, tongs);
        TestSupport.use(h, player, 0.5);
        h.assertTrue(WorkpieceCodec.isHolding(tongs) && trough.water() == 1000, "Unshaped workpieces cannot be quenched");

        WorkpieceCodec.setHeld(tongs, TestSupport.workpiece(new ItemStack(Items.IRON_SWORD), 80, 95, false, WorkpieceState.SHAPED));
        TestSupport.use(h, player, 0.5);
        h.assertTrue(!WorkpieceCodec.isHolding(tongs), "Quenching empties the tongs");
        h.assertTrue(trough.water() == 750 && trough.quenchesLeft() == 3, "One quench uses a quarter bucket");
        ItemStack result = find(player, Items.IRON_SWORD);
        QualityData q = QualityData.get(result).orElse(null);
        h.assertTrue(q != null && q.forgeScore() == 80 && q.anvilScore() == 95 && !q.faulty(), "Finished sword carries the scores");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeSessionEscrowAndScoring(GameTestHelper h) {
        SmithsForgeBlockEntity forge = forge(h);
        FakePlayer player = TestSupport.player(h);
        WorkpieceData molten = TestSupport.workpiece(new ItemStack(Items.IRON_PICKAXE), 0, -1, false, WorkpieceState.FORGED);
        forge.acceptWorkpiece(molten);
        forge.acceptWorkpiece(molten);
        h.assertTrue(forge.isReady() && forge.moltenUnits() == 54, "54 molten units ready");
        ItemStack tongs = new ItemStack(ModItems.tongs(SmithingTier.STONE));
        player.setItemInHand(InteractionHand.MAIN_HAND, tongs);
        player.getInventory().setItem(5, new ItemStack(Items.STICK, 3));
        SmithingRecipe pickaxe = SmithingData.current().recipesProducing(Items.IRON_PICKAXE).get(0);

        // Cancelling refunds the escrowed sticks and consumes no metal.
        SessionManager.openForge(player, forge, InteractionHand.MAIN_HAND, SmithingTier.STONE);
        SmithingSession session = SessionManager.byPlayer(player.getUUID()).orElseThrow();
        SessionManager.selectRecipe(player, session.id, pickaxe.getId());
        h.assertTrue(player.getInventory().countItem(Items.STICK) == 1 && forge.isLocked(), "Two sticks in escrow, forge locked");
        SessionManager.cancelFromClient(player, session.id);
        h.assertTrue(player.getInventory().countItem(Items.STICK) == 3 && !forge.isLocked() && forge.moltenUnits() == 54, "Cancel refunds and releases");

        // A forged selection of an unoffered recipe is rejected.
        SessionManager.openForge(player, forge, InteractionHand.MAIN_HAND, SmithingTier.STONE);
        session = SessionManager.byPlayer(player.getUUID()).orElseThrow();
        SessionManager.selectRecipe(player, session.id, OtesSmithing.id("invented"));
        h.assertTrue(SessionManager.byPlayer(player.getUUID()).isEmpty() && player.getInventory().countItem(Items.STICK) == 3, "Unknown recipe rejected");

        // A perfect run: every press at the zone centre, with the server's own clock.
        SessionManager.openForge(player, forge, InteractionHand.MAIN_HAND, SmithingTier.STONE);
        session = SessionManager.byPlayer(player.getUUID()).orElseThrow();
        SessionManager.selectRecipe(player, session.id, pickaxe.getId());
        ForgeSession fs = (ForgeSession) SessionManager.byPlayer(player.getUUID()).orElseThrow();
        ForgePattern pattern = PatternRegistry.forge(pickaxe.forgePattern());
        List<ForgeMinigame.Phase> phases = ForgeMinigame.phases(pattern, fs.seed, pickaxe.metalUnits());
        ForgeRun shadow = new ForgeRun(phases, pattern.transitionMs());
        // A duplicate or out-of-order phase index is ignored.
        SessionManager.forgeAction(player, fs.id, 3, 10, fs.startMs() + 10);
        for (int phase = 0; phase < phases.size(); phase++) {
            int t = shadow.phaseStartMs() + perfectMoment(phases.get(phase));
            shadow.press(t);
            SessionManager.forgeAction(player, fs.id, phase, t, fs.startMs() + t);
        }
        h.assertTrue(SessionManager.byPlayer(player.getUUID()).isEmpty(), "Session finished");
        WorkpieceData wp = WorkpieceCodec.getHeld(player.getMainHandItem()).orElse(null);
        h.assertTrue(wp != null && wp.forgeScore() == 100 && wp.state() == WorkpieceState.FORGED, "Perfect forge score on the tongs");
        h.assertTrue(wp.target().is(Items.IRON_PICKAXE), "Target is the selected item");
        h.assertTrue(forge.moltenUnits() == 27 && !forge.isLocked(), "27 units consumed, 27 remain");
        h.assertTrue(player.getInventory().countItem(Items.STICK) == 1, "Sticks consumed on completion only");
        h.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Tongs wear by one");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void forgeSessionSurvivesLagSpike(GameTestHelper h) {
        SmithsForgeBlockEntity forge = forge(h);
        FakePlayer player = TestSupport.player(h);
        forge.acceptWorkpiece(TestSupport.workpiece(new ItemStack(Items.IRON_PICKAXE), 0, -1, false, WorkpieceState.FORGED));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.tongs(SmithingTier.IRON)));
        player.getInventory().setItem(5, new ItemStack(Items.STICK, 2));
        SmithingRecipe pickaxe = SmithingData.current().recipesProducing(Items.IRON_PICKAXE).get(0);
        SessionManager.openForge(player, forge, InteractionHand.MAIN_HAND, SmithingTier.IRON);
        SessionManager.selectRecipe(player, SessionManager.byPlayer(player.getUUID()).orElseThrow().id, pickaxe.getId());
        ForgeSession fs = (ForgeSession) SessionManager.byPlayer(player.getUUID()).orElseThrow();
        // Phase 0 arrives two seconds late, so the server clamps it later than the client pressed it.
        SessionManager.forgeAction(player, fs.id, 0, 500, fs.startMs() + 2500);
        // Phase 1 was pressed right after the client's transition, inside the server's shifted transition.
        SessionManager.forgeAction(player, fs.id, 1, 1000, fs.startMs() + 1000);
        SessionManager.forgeAction(player, fs.id, 2, 4000, fs.startMs() + 4000);
        SessionManager.forgeAction(player, fs.id, 3, 6000, fs.startMs() + 6000);
        h.assertTrue(SessionManager.byPlayer(player.getUUID()).isEmpty(), "All four phases were processed; the session did not stall");
        h.assertTrue(WorkpieceCodec.isHolding(player.getMainHandItem()), "The workpiece was produced");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anvilSessionScoringTimeoutAndDisconnect(GameTestHelper h) {
        FakePlayer player = TestSupport.player(h);
        h.setBlock(STATION, ModBlocks.SMITHS_ANVIL.get().defaultBlockState());
        SmithsAnvilBlockEntity anvil = (SmithsAnvilBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(STATION));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.hammer(SmithingTier.STONE)));

        // Disconnecting releases the lock and never makes the workpiece Faulty.
        anvil.setWorkpiece(TestSupport.workpiece(new ItemStack(Items.IRON_SWORD), 70, -1, false, WorkpieceState.FORGED));
        SessionManager.openAnvil(player, anvil, InteractionHand.MAIN_HAND, SmithingTier.STONE);
        h.assertTrue(anvil.isLocked(), "Anvil locked while shaping");
        SessionManager.onLogout(player);
        h.assertTrue(!anvil.isLocked() && !anvil.workpiece().isShaped() && !anvil.workpiece().faulty(), "Disconnect: unlocked, still forged");

        // Perfect strikes at the target centre on the ideal beat.
        SessionManager.openAnvil(player, anvil, InteractionHand.MAIN_HAND, SmithingTier.STONE);
        SmithingSession session = SessionManager.byPlayer(player.getUUID()).orElseThrow();
        AnvilPattern pattern = PatternRegistry.anvil(anvil.workpiece().anvilPattern());
        AnvilRun shadow = new AnvilRun(pattern, session.seed);
        int seq = 0;
        SessionManager.anvilStrike(player, session.id, ++seq, 5F, 5F, 100, session.startMs() + 100); // impossible coordinates: ignored
        while (!shadow.isComplete()) {
            AnvilMinigame.Target target = shadow.currentTarget();
            int t = shadow.spawnMs() + AnvilMinigame.idealMs(pattern);
            shadow.strike(t, target.x(), target.y());
            SessionManager.anvilStrike(player, session.id, ++seq, target.x(), target.y(), t, session.startMs() + t);
        }
        WorkpieceData shaped = anvil.workpiece();
        h.assertTrue(shaped.isShaped() && !shaped.faulty() && shaped.anvilScore() == 100, "Perfect anvil score, got " + shaped.anvilScore());
        h.assertTrue(shaped.forgeScore() == 70, "Forge score preserved");
        h.assertTrue(player.getMainHandItem().getDamageValue() == 1, "Hammer wears by one");

        // Running out of time makes the workpiece Faulty but keeps it.
        anvil.setWorkpiece(TestSupport.workpiece(new ItemStack(Items.IRON_SWORD), 70, -1, false, WorkpieceState.FORGED));
        SessionManager.openAnvil(player, anvil, InteractionHand.MAIN_HAND, SmithingTier.STONE);
        session = SessionManager.byPlayer(player.getUUID()).orElseThrow();
        int late = session.allowedMs() + 5000;
        SessionManager.anvilStrike(player, session.id, 1, 0.5F, 0.5F, late, session.startMs() + late);
        h.assertTrue(anvil.hasWorkpiece() && anvil.workpiece().isShaped() && anvil.workpiece().faulty(), "Timeout produces a Faulty workpiece");
        h.assertTrue(!anvil.isLocked() && SessionManager.byPlayer(player.getUUID()).isEmpty(), "Session over");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void minigameRulesAndTiming(GameTestHelper h) {
        ForgePattern pattern = ForgePattern.DEFAULT;
        List<ForgeMinigame.Phase> phases = ForgeMinigame.phases(pattern, 1234L, 72);
        h.assertTrue(phases.equals(ForgeMinigame.phases(pattern, 1234L, 72)), "Phases are deterministic");
        h.assertTrue(phases.size() == 6, "A chestplate has 6 phases, got " + phases.size());
        ForgeRun early = new ForgeRun(phases, pattern.transitionMs());
        for (int i = 0; i < phases.size(); i++) early.press(early.phaseStartMs());
        h.assertTrue(early.score() == 0, "Pressing immediately misses every zone");

        AnvilPattern anvil = AnvilPattern.fallback(OtesSmithing.id("x"));
        AnvilRun run = new AnvilRun(anvil, 99L);
        run.advanceTo(anvil.lifetimeMs() + 1);
        h.assertTrue(run.index() == 0 && run.attempt() == 1 && run.samples() == 1, "An expired target counts and returns");
        AnvilMinigame.Target t = run.currentTarget();
        h.assertTrue(run.strike(run.spawnMs() - 1, t.x(), t.y()) == null, "Strikes between targets are ignored");
        AnvilRun.StrikeResult miss = run.strike(run.spawnMs() + 100, 0.99F, 0.99F);
        h.assertTrue(miss != null && !miss.hit(), "Far strikes miss");
        h.assertTrue(run.strike(run.spawnMs() + 120, t.x(), t.y()) == null, "Strikes faster than the rate limit are ignored");

        int tol = 120;
        h.assertTrue(SessionTiming.validate(5000, 1000, 50, 0) <= 1000 + tol, "Future timestamps are clamped");
        h.assertTrue(SessionTiming.validate(0, 1000, 0, 0) >= 1000 - tol, "Stale timestamps are clamped");
        h.assertTrue(SessionTiming.validate(900, 1000, 150, 0) == 900, "Honest latency-shifted timestamps are kept");
        h.assertTrue(SessionTiming.validate(100, 1000, 10_000, 0) >= 1000 - 400 - tol, "Latency compensation is bounded");
        h.succeed();
    }

    private static int perfectMoment(ForgeMinigame.Phase phase) {
        for (int ms = 0; ms < 20_000; ms++) {
            if (ForgeMinigame.accuracy(phase, ForgeMinigame.position(phase, ms / 1000F)) >= 1F) return ms;
        }
        throw new IllegalStateException("No perfect moment");
    }

    private static ItemStack find(FakePlayer player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(item)) return player.getInventory().getItem(i);
        }
        return ItemStack.EMPTY;
    }
}
