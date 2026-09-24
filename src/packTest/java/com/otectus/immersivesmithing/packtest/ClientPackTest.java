package com.otectus.immersivesmithing.packtest;

import com.google.gson.GsonBuilder;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.block.SmithsAnvilBlock;
import com.otectus.immersivesmithing.block.SmithsForgeBlock;
import com.otectus.immersivesmithing.block.SmithsGrindstoneBlock;
import com.otectus.immersivesmithing.block.SmithsTroughBlock;
import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.immersivesmithing.client.screen.AnvilMinigameScreen;
import com.otectus.immersivesmithing.client.screen.ForgeMinigameScreen;
import com.otectus.immersivesmithing.client.screen.GuideScreen;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.minigame.ForgePattern;
import com.otectus.immersivesmithing.minigame.PatternRegistry;
import com.otectus.immersivesmithing.minigame.SmithingSession;
import com.otectus.immersivesmithing.network.packet.ForgeStartPacket;
import com.otectus.immersivesmithing.network.packet.OpenAnvilScreenPacket;
import com.otectus.immersivesmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.immersivesmithing.network.packet.SessionResultPacket;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.recipe.SmithingData;
import com.otectus.immersivesmithing.registry.ModBlocks;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.util.StationEffects;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import com.otectus.immersivesmithing.workpiece.WorkpieceState;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Drives a client from the title screen into a fresh flat world, checks that Immersive Smithing built its data inside
 * whatever modpack is loaded, stages every station and screen for framebuffer screenshots, writes
 * {@code packtest-result.json} into the output directory and quits.
 */
@Mod.EventBusSubscriber(modid = PackTestAgent.MOD_ID, value = Dist.CLIENT)
public final class ClientPackTest {
    private static final String OUTPUT = System.getProperty(PackTestAgent.OUTPUT_PROPERTY);
    private static final boolean CREATE_WORLD = !"false".equals(System.getProperty("immersive_smithing.packtest.world"));
    private static final Set<String> EXPECT = Set.of(System.getProperty("immersive_smithing.packtest.expect", "").split(","));
    private static final boolean MULTIPLAYER = Boolean.getBoolean("immersive_smithing.packtest.multiplayer");
    private static final String MULTIPLAYER_ROLE = System.getProperty("immersive_smithing.packtest.role", "");
    private static final String MULTIPLAYER_SERVER = System.getProperty("immersive_smithing.packtest.server", "");
    private static final Path MULTIPLAYER_SHARED = Path.of(System.getProperty("immersive_smithing.packtest.shared", "."));
    private static final boolean REQUIRE_EXACT_MINIMUM = Boolean.getBoolean("immersive_smithing.packtest.requireExactMinimum");

    private static final Map<String, Object> checks = new LinkedHashMap<>();
    private static long startMs = Util.getMillis();
    private static long titleMs = -1;
    private static long worldMs = -1;
    private static int titleTicks = -1;
    private static int worldTicks = -1;
    private static int settledAt = -1;
    private static int quietTicks;
    private static boolean worldRequested;
    private static boolean finished;
    private static int originalGuiScale;
    private static int originalWindowWidth;
    private static int originalWindowHeight;
    private static boolean originalReducedShake;
    private static boolean originalReducedFlashes;
    private static boolean originalHighContrast;
    private static boolean originalLargeTargets;
    private static boolean originalColorblind;
    private static ClientConfig.ParticleAmount originalParticles;
    private static boolean originalVsync;
    private static int originalFrameLimit;
    private static int originalRenderDistance;
    private static CompletableFuture<Void> resourceReload;
    private static int advancedPhase;
    private static int advancedAt;
    private static FrameMeasurement frameMeasurement;
    private static int multiplayerTicks;
    private static boolean multiplayerConnected;
    private static boolean remoteSwingObserved;
    private static boolean remoteHammerSwingObserved;
    private static int multiplayerJoinedAt = -1;
    private static int multiplayerHammerReadyAt = -1;
    private static boolean multiplayerReady;
    private static BlockPos galleryBase;
    private static BlockPos performanceBase;
    private static final Map<Direction, BlockPos> galleryCenters = new LinkedHashMap<>();
    private static int galleryCaptureIndex;
    private static int overlayClearedAt = -1;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (OUTPUT == null || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (MULTIPLAYER) {
                multiplayerStep(mc);
                return;
            }
            if (mc.player == null || mc.level == null) {
                if (mc.screen instanceof TitleScreen) {
                    if (titleMs < 0) {
                        titleMs = Util.getMillis() - startMs;
                        log("title screen reached after " + titleMs + " ms");
                    }
                    titleTicks++;
                    if (titleTicks == 60) {
                        if (!CREATE_WORLD) {
                            finish(mc, true, "title only");
                        } else if (!worldRequested) {
                            worldRequested = true;
                            createWorld(mc);
                        }
                    }
                }
                return;
            }
            worldTicks++;
            if (worldMs < 0) {
                worldMs = Util.getMillis() - startMs;
                log("world joined after " + worldMs + " ms");
            }
            if (settledAt < 0 && !settle(mc)) return;
            step(mc, worldTicks - settledAt + 40);
        } catch (Exception e) {
            ImmersiveSmithing.LOGGER.error("PACKTEST agent failed at world tick {}", worldTicks, e);
            checks.put("agent_exception", e.toString());
            finish(mc, false, "agent exception");
        }
    }

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (frameMeasurement == null) return;
        if (event.phase == TickEvent.Phase.START) frameMeasurement.beginFrame(System.nanoTime());
        else frameMeasurement.endFrame(System.nanoTime());
    }

    /** Waits until the pack's first-join screens are dealt with (2 s without a screen), so staging and shots see the world. */
    private static boolean settle(Minecraft mc) {
        if (worldTicks % 5 == 0) settlePackScreen(mc);
        quietTicks = mc.screen == null ? quietTicks + 1 : 0;
        boolean quiet = worldTicks >= 40 && quietTicks >= 40;
        if (quiet || worldTicks >= 1200) {
            settledAt = worldTicks;
            checks.put("pack_screens_settled_ticks", settledAt);
            log("pack screens " + (quiet ? "settled" : "still open") + " after " + worldTicks + " ticks");
            return true;
        }
        return false;
    }

    private static void step(Minecraft mc, int t) {
        if (t > 40 && t <= 226 && t % 5 == 1) settlePackScreen(mc);
        switch (t) {
            case 40 -> {
                originalGuiScale = mc.options.guiScale().get();
                originalWindowWidth = mc.getWindow().getWidth();
                originalWindowHeight = mc.getWindow().getHeight();
                originalReducedShake = ClientConfig.REDUCED_SCREEN_SHAKE.get();
                originalReducedFlashes = ClientConfig.REDUCED_FLASHES.get();
                originalHighContrast = ClientConfig.HIGH_CONTRAST_MINIGAMES.get();
                originalLargeTargets = ClientConfig.LARGE_MINIGAME_TARGETS.get();
                originalColorblind = ClientConfig.COLORBLIND_SAFE_TARGETS.get();
                originalParticles = ClientConfig.PARTICLE_AMOUNT.get();
                originalVsync = mc.options.enableVsync().get();
                originalFrameLimit = mc.options.framerateLimit().get();
                originalRenderDistance = mc.options.renderDistance().get();
                runChecks(mc);
                stageWorld(mc);
            }
            case 196 -> mc.options.hideGui = true;
            case 200 -> shot(mc, "packtest_world");
            case 201 -> camera(mc, 1.9, -2.4, 47F, true);
            case 222 -> shot(mc, "packtest_world_top");
            case 223 -> {
                mc.options.hideGui = false;
                camera(mc, 0, 0, 20F, false);
            }
            // Tongs stab. A swing lasts 6 ticks and a shot grabs the frame before this tick, so swing + 5 is mid-thrust.
            case 226 -> shot(mc, "packtest_tongs_first_person_idle");
            case 228 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 233 -> shot(mc, "packtest_tongs_first_person");
            case 234 -> {
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                mc.player.setXRot(5F);
            }
            case 237 -> shot(mc, "packtest_tongs_third_person_idle");
            case 238 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 243 -> shot(mc, "packtest_tongs_third_person");
            // Control: an ordinary sword swing in the same view shows what the pack's own animation mods do.
            case 244 -> mc.player.getInventory().selected = 1;
            case 246 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 251 -> shot(mc, "packtest_sword_third_person");
            case 252 -> {
                mc.player.getInventory().selected = 0;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            case 254 -> mc.player.swing(InteractionHand.MAIN_HAND);
            case 259 -> {
                shot(mc, "packtest_tongs_third_person_back");
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.player.setXRot(20F);
            }
            case 260 -> mc.setScreen(new ForgeMinigameScreen(forgeOffer()));
            case 285 -> shot(mc, "packtest_forge_select");
            case 290 -> {
                if (mc.screen instanceof ForgeMinigameScreen s) {
                    s.onStart(new ForgeStartPacket(7, 42L, ForgePattern.DEFAULT, 27, 30_000, 0, new ItemStack(Items.IRON_PICKAXE)));
                }
            }
            case 317 -> shot(mc, "packtest_forge_play");
            case 325 -> mc.setScreen(new AnvilMinigameScreen(new OpenAnvilScreenPacket(8, BlockPos.ZERO, new ItemStack(Items.IRON_SWORD),
                    PatternRegistry.anvil(ImmersiveSmithing.id("sword")), 42L, 30_000, 0)));
            case 345 -> shot(mc, "packtest_anvil");
            case 350 -> mc.setScreen(new GuideScreen(2));
            case 370 -> shot(mc, "packtest_guide");
            case 375 -> mc.setScreen(new InventoryScreen(mc.player));
            case 410 -> shot(mc, "packtest_inventory");
            case 420 -> mc.setScreen(null);
            case 430 -> {
                setGuiScale(mc, 1);
                mc.setScreen(new ForgeMinigameScreen(longForgeOffer()));
            }
            case 450 -> shot(mc, "packtest_forge_long_scale_1");
            case 455 -> setGuiScale(mc, 2);
            case 475 -> shot(mc, "packtest_forge_long_scale_2");
            case 480 -> setGuiScale(mc, 3);
            case 500 -> shot(mc, "packtest_forge_long_scale_3");
            case 505 -> {
                if (mc.screen instanceof ForgeMinigameScreen s) {
                    s.onStart(new ForgeStartPacket(7, 42L, ForgePattern.DEFAULT, 27, 30_000, 5_000,
                            named(Items.IRON_PICKAXE, "An Exceptionally Long Tempered Iron Pickaxe Name")));
                }
            }
            case 525 -> shot(mc, "packtest_forge_countdown");
            case 530 -> {
                if (mc.screen instanceof ForgeMinigameScreen s) {
                    s.onResult(new SessionResultPacket(7, SmithingSession.Kind.FORGE, 91, false));
                }
            }
            case 550 -> shot(mc, "packtest_forge_result");
            case 555 -> mc.setScreen(new AnvilMinigameScreen(new OpenAnvilScreenPacket(8, BlockPos.ZERO,
                    named(Items.IRON_SWORD, "An Exceptionally Long Tempered Iron Sword Name"),
                    PatternRegistry.anvil(ImmersiveSmithing.id("sword")), 42L, 30_000, 5_000)));
            case 575 -> shot(mc, "packtest_anvil_countdown");
            case 580 -> {
                if (mc.screen instanceof AnvilMinigameScreen s) {
                    s.onResult(new SessionResultPacket(8, SmithingSession.Kind.ANVIL, 87, false));
                }
            }
            case 600 -> shot(mc, "packtest_anvil_result");
            case 605 -> mc.setScreen(new GuideScreen(2));
            case 625 -> {
                shot(mc, "packtest_guide_scale_3");
                setGameMode(mc, GameType.SURVIVAL);
            }
            case 630 -> {
                setGuiScale(mc, 1);
                mc.setScreen(new FixedInventoryScreen(mc.player, false));
            }
            case 650 -> shot(mc, "packtest_inventory_tools_scale_1");
            case 655 -> setGuiScale(mc, 2);
            case 675 -> shot(mc, "packtest_inventory_tools_scale_2");
            case 680 -> {
                mc.getWindow().setWindowed(960, 720);
                setGuiScale(mc, 3);
                boolean exact = mc.getWindow().getGuiScaledWidth() == 320 && mc.getWindow().getGuiScaledHeight() == 240;
                Map<String, Object> minimum = new LinkedHashMap<>();
                minimum.put("requested_framebuffer", "960x720");
                minimum.put("logical_width", mc.getWindow().getGuiScaledWidth());
                minimum.put("logical_height", mc.getWindow().getGuiScaledHeight());
                minimum.put("exact_320x240", exact);
                checks.put("gui_minimum_probe", minimum);
                if (REQUIRE_EXACT_MINIMUM) checks.put("gui_exact_320x240_required", exact);
            }
            case 700 -> shot(mc, "packtest_inventory_tools_scale_3");
            case 701 -> mc.setScreen(new FixedInventoryScreen(mc.player, true));
            case 704 -> shot(mc, "packtest_inventory_quality_tooltip");
            case 705 -> {
                setGameMode(mc, GameType.CREATIVE);
                mc.setScreen(new ForgeMinigameScreen(longForgeOffer()));
            }
            case 725 -> shot(mc, "packtest_forge_long_320x240");
            case 730 -> mc.setScreen(new AnvilMinigameScreen(new OpenAnvilScreenPacket(8, BlockPos.ZERO,
                    named(Items.IRON_SWORD, "An Exceptionally Long Tempered Iron Sword Name"),
                    PatternRegistry.anvil(ImmersiveSmithing.id("sword")), 42L, 30_000, 5_000)));
            case 750 -> shot(mc, "packtest_anvil_320x240");
            case 755 -> mc.setScreen(new GuideScreen(2));
            case 775 -> shot(mc, "packtest_guide_320x240");
            case 780 -> {
                enableAccessibilityMode();
                mc.getWindow().setWindowed(originalWindowWidth, originalWindowHeight);
                setGuiScale(mc, 2);
                ForgeMinigameScreen screen = new ForgeMinigameScreen(forgeOffer());
                mc.setScreen(screen);
                screen.onStart(new ForgeStartPacket(7, 42L, ForgePattern.DEFAULT, 27, 30_000, 0,
                        new ItemStack(Items.IRON_PICKAXE)));
            }
            case 805 -> shot(mc, "packtest_forge_accessibility");
            case 810 -> mc.setScreen(new AnvilMinigameScreen(new OpenAnvilScreenPacket(8, BlockPos.ZERO,
                    new ItemStack(Items.IRON_SWORD), PatternRegistry.anvil(ImmersiveSmithing.id("sword")), 42L, 30_000, 0)));
            case 835 -> shot(mc, "packtest_anvil_accessibility");
            case 840 -> {
                restoreAccessibilityMode();
                openJei(mc);
            }
            case 870 -> {
                if (PackTestJeiPlugin.runtime() != null) shot(mc, "packtest_jei_smithing");
            }
            case 875 -> {
                mc.setScreen(null);
                resourceReload = mc.reloadResourcePacks();
            }
            case 895 -> advancedAt = t;
            default -> {
            }
        }
        if (t >= 895) advancedStep(mc, t);
    }

    private static void createWorld(Minecraft mc) {
        String name = "PackTest" + System.currentTimeMillis();
        log("creating flat world " + name);
        LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(),
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(20260923L, false, false),
                access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    private static void runChecks(Minecraft mc) {
        SmithingData data = SmithingData.current();
        IntegratedServer server = mc.getSingleplayerServer();
        checks.put("summary", data.report().summary());
        checks.put("smithing_data_built", data.recipes().size() >= 28);
        checks.put("vanilla_explicit_recipes", data.recipe(ImmersiveSmithing.id("smithing/iron_sword")) != null);
        checks.put("material_families", data.materials().families().size());
        if (server != null) {
            checks.put("iron_sword_crafting_disabled", server.getRecipeManager().byKey(new ResourceLocation("minecraft", "iron_sword")).isEmpty());
        }
        if (EXPECT.contains("spartan")) {
            checks.put("spartan_iron_longsword", data.recipe(ImmersiveSmithing.id("compat/spartanweaponry/iron_longsword")) != null);
            checks.put("spartan_netherite_longsword", data.recipe(ImmersiveSmithing.id("compat/spartanweaponry/netherite_longsword")) != null);
            checks.put("spartan_iron_javelin", data.recipe(ImmersiveSmithing.id("compat/spartanweaponry/iron_javelin")) != null);
        }
        if (EXPECT.contains("shields")) {
            checks.put("spartan_shields_iron_basic", !data.recipesProducing(item("spartanshields:iron_basic_shield")).isEmpty());
        }
        if (EXPECT.contains("upgrades")) {
            checks.put("spartan_shields_netherite_upgrade", !data.recipesProducing(item("spartanshields:netherite_basic_shield")).isEmpty());
        }
        checks.put("mods_loaded", ModList.get().size());
        try {
            data.report().write(Path.of(OUTPUT));
        } catch (IOException e) {
            checks.put("report_written", false);
        }
        log("checks: " + checks);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(new ResourceLocation(id));
    }

    /**
     * Packs open their own screens on first join, and the world shots need them gone. Origins (Ultima's heritage
     * choice) reopens its screen until an origin is chosen, so its Select button takes the offered origin as a player
     * would; any other screen is closed.
     */
    private static void settlePackScreen(Minecraft mc) {
        Screen screen = mc.screen;
        if (screen == null) return;
        if (screen.getClass().getName().contains(".origins.")) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof Button button && button.active && "Select".equals(button.getMessage().getString())) {
                    log("choosing the offered origin on " + screen.getClass().getName());
                    button.onPress();
                    return;
                }
            }
            return;
        }
        log("closing pack screen " + screen.getClass().getName());
        mc.setScreen(null);
    }

    private static void camera(Minecraft mc, double up, double back, float pitch, boolean spectator) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(spectator ? GameType.SPECTATOR : GameType.CREATIVE);
            if (spectator) player.teleportTo(server.overworld(), player.getX(), player.getY() + up, player.getZ() + back, 180F, pitch);
        });
    }

    private static void stageWorld(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerLevel level = server.overworld();
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            level.setDayTime(6000);
            BlockPos base = player.blockPosition();
            galleryBase = base.offset(0, 0, -14);
            performanceBase = base.offset(0, 0, 48);
            for (int x = -34; x <= 34; x++) for (int z = -27; z <= 62; z++) {
                level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                for (int y = 0; y <= 5; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
            BlockPos f1 = base.offset(-4, 0, -4);
            level.setBlockAndUpdate(f1, ModBlocks.SMITHS_FORGE.get().defaultBlockState().setValue(SmithsForgeBlock.FACING, Direction.SOUTH));
            SmithsForgeBlockEntity forge1 = (SmithsForgeBlockEntity) level.getBlockEntity(f1);
            forge1.insertMetal(new ItemStack(Items.IRON_INGOT, 12), 12, false, null);
            forge1.insertMetal(new ItemStack(Items.IRON_BLOCK, 2), 2, false, null);
            forge1.insertFuel(new ItemStack(Items.COAL, 24), 24, false, null);
            BlockPos f2 = base.offset(-2, 0, -4);
            level.setBlockAndUpdate(f2, ModBlocks.SMITHS_FORGE.get().defaultBlockState().setValue(SmithsForgeBlock.FACING, Direction.SOUTH));
            SmithsForgeBlockEntity forge2 = (SmithsForgeBlockEntity) level.getBlockEntity(f2);
            forge2.addLavaBucket(false);
            forge2.acceptWorkpiece(workpiece(Items.IRON_CHESTPLATE, 216, WorkpieceState.FORGED, "chestplate"));
            BlockPos a = base.offset(0, 0, -4);
            level.setBlockAndUpdate(a, ModBlocks.SMITHS_ANVIL.get().defaultBlockState().setValue(SmithsAnvilBlock.FACING, Direction.EAST));
            ((SmithsAnvilBlockEntity) level.getBlockEntity(a)).setWorkpiece(workpiece(Items.IRON_SWORD, 18, WorkpieceState.FORGED, "sword"));
            BlockPos t = base.offset(2, 0, -4);
            level.setBlockAndUpdate(t, ModBlocks.SMITHS_TROUGH.get().defaultBlockState().setValue(SmithsTroughBlock.FACING, Direction.SOUTH));
            SmithsTroughBlockEntity trough = (SmithsTroughBlockEntity) level.getBlockEntity(t);
            trough.addBucket();
            trough.addBucket();
            level.setBlockAndUpdate(base.offset(4, 0, -4), ModBlocks.SMITHS_GRINDSTONE.get().defaultBlockState().setValue(SmithsGrindstoneBlock.FACING, Direction.SOUTH));

            ItemStack tongs = new ItemStack(ModItems.tongs(SmithingTier.IRON));
            WorkpieceCodec.setHeld(tongs, workpiece(Items.IRON_AXE, 27, WorkpieceState.SHAPED, "axe"));
            player.setItemInHand(InteractionHand.MAIN_HAND, tongs);
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            new QualityData(94, 82, false).apply(sword);
            player.getInventory().setItem(1, sword);
            player.getInventory().setItem(2, new ItemStack(ModItems.hammer(SmithingTier.DIAMOND)));
            player.getInventory().setItem(3, new ItemStack(ModItems.SMITHING_GUIDE.get()));
            player.getInventory().setItem(4, new ItemStack(ModItems.tongs(SmithingTier.NETHERITE)));
            int slot = 9;
            for (SmithingTier tier : SmithingTier.values()) {
                player.getInventory().setItem(slot++, new ItemStack(ModItems.hammer(tier)));
                player.getInventory().setItem(slot++, new ItemStack(ModItems.tongs(tier)));
                ItemStack loaded = new ItemStack(ModItems.tongs(tier));
                WorkpieceCodec.setHeld(loaded, workpiece(Items.IRON_SWORD, 18, WorkpieceState.SHAPED, "sword"));
                player.getInventory().setItem(slot++, loaded);
            }
            int cluster = 0;
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                BlockPos center = galleryBase.offset(-24 + cluster * 16, 0, 0);
                galleryCenters.put(facing, center);
                stageStationCluster(level, center, facing);
                cluster++;
            }
            stagePerformance(level, false);
            checks.put("gallery_station_count", 52);
            checks.put("inventory_tool_variants", slot - 9);
            player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 1.5, 180F, 20F);
        });
    }

    private static void stageStationCluster(ServerLevel level, BlockPos center, Direction facing) {
        Direction right = facing.getClockWise();
        for (int r = -7; r <= 7; r++) for (int back = -1; back <= 8; back++) {
            BlockPos floor = local(center, right, facing, r, back).below();
            level.setBlockAndUpdate(floor, ((r + back) & 1) == 0 ? Blocks.POLISHED_ANDESITE.defaultBlockState()
                    : Blocks.SMOOTH_STONE.defaultBlockState());
        }
        BlockPos[] forges = new BlockPos[4];
        int index = 0;
        for (int r : new int[]{-5, -2, 1, 4}) {
            BlockPos pos = local(center, right, facing, r, 1);
            forges[index++] = pos;
            level.setBlockAndUpdate(pos, ModBlocks.SMITHS_FORGE.get().defaultBlockState().setValue(SmithsForgeBlock.FACING, facing));
        }
        SmithsForgeBlockEntity loaded = (SmithsForgeBlockEntity) level.getBlockEntity(forges[1]);
        loaded.insertMetal(new ItemStack(Items.IRON_INGOT, 12), 12, false, null);
        SmithsForgeBlockEntity burning = (SmithsForgeBlockEntity) level.getBlockEntity(forges[2]);
        burning.insertMetal(new ItemStack(Items.IRON_INGOT, 12), 12, false, null);
        burning.insertFuel(new ItemStack(Items.COAL, 16), 16, false, null);
        burning.ignite();
        SmithsForgeBlockEntity ready = (SmithsForgeBlockEntity) level.getBlockEntity(forges[3]);
        ready.acceptWorkpiece(workpiece(Items.IRON_PICKAXE, 27, WorkpieceState.FORGED, "pickaxe"));

        BlockPos forged = local(center, right, facing, -4, 4);
        level.setBlockAndUpdate(forged, ModBlocks.SMITHS_ANVIL.get().defaultBlockState().setValue(SmithsAnvilBlock.FACING, facing));
        ((SmithsAnvilBlockEntity) level.getBlockEntity(forged)).setWorkpiece(
                workpiece(Items.IRON_SWORD, 18, WorkpieceState.FORGED, "sword"));
        BlockPos shaped = local(center, right, facing, 0, 4);
        level.setBlockAndUpdate(shaped, ModBlocks.SMITHS_ANVIL.get().defaultBlockState().setValue(SmithsAnvilBlock.FACING, facing));
        ((SmithsAnvilBlockEntity) level.getBlockEntity(shaped)).setWorkpiece(
                workpiece(Items.IRON_AXE, 27, WorkpieceState.SHAPED, "axe"));
        level.setBlockAndUpdate(local(center, right, facing, 4, 4),
                ModBlocks.SMITHS_GRINDSTONE.get().defaultBlockState().setValue(SmithsGrindstoneBlock.FACING, facing));

        index = 0;
        for (int r : new int[]{-5, -2, 1, 4}) {
            BlockPos pos = local(center, right, facing, r, 7);
            level.setBlockAndUpdate(pos, ModBlocks.SMITHS_TROUGH.get().defaultBlockState().setValue(SmithsTroughBlock.FACING, facing));
            SmithsTroughBlockEntity trough = (SmithsTroughBlockEntity) level.getBlockEntity(pos);
            for (int bucket = 0; bucket < index; bucket++) trough.addBucket();
            index++;
        }
    }

    private static BlockPos local(BlockPos center, Direction right, Direction facing, int across, int back) {
        Direction rear = facing.getOpposite();
        return center.offset(right.getStepX() * across + rear.getStepX() * back, 0,
                right.getStepZ() * across + rear.getStepZ() * back);
    }

    private static void advancedStep(Minecraft mc, int t) {
        if (t < advancedAt) return;
        switch (advancedPhase) {
            case 0 -> {
                if (resourceReload == null || !resourceReload.isDone()) return;
                if (mc.getOverlay() != null) {
                    overlayClearedAt = -1;
                    return;
                }
                if (overlayClearedAt < 0) {
                    overlayClearedAt = t;
                    return;
                }
                if (t - overlayClearedAt < 20) return;
                resourceReload.join();
                checks.put("resource_reload", true);
                checks.put("jei_runtime_available", PackTestJeiPlugin.runtime() != null);
                setGuiScale(mc, originalGuiScale);
                Direction facing = galleryCenters.keySet().iterator().next();
                setDayAndCamera(mc, 6000, galleryCenters.get(facing), facing);
                advance(t, 25);
            }
            case 1 -> {
                List<Direction> directions = List.copyOf(galleryCenters.keySet());
                Direction facing = directions.get(galleryCaptureIndex);
                shot(mc, "packtest_stations_" + facing.getName() + "_day");
                galleryCaptureIndex++;
                if (galleryCaptureIndex < directions.size()) {
                    Direction next = directions.get(galleryCaptureIndex);
                    setDayAndCamera(mc, 6000, galleryCenters.get(next), next);
                    advancedAt = t + 25;
                } else {
                    Direction nightFacing = directions.get(0);
                    setDayAndCamera(mc, 18000, galleryCenters.get(nightFacing), nightFacing);
                    advance(t, 30);
                }
            }
            case 2 -> {
                shot(mc, "packtest_stations_night");
                prepareEffectCamera(mc, "quench");
                advance(t, 10);
            }
            case 3 -> {
                fireEffect(mc, "quench");
                advance(t, 3);
            }
            case 4 -> {
                shot(mc, "packtest_effect_quench_real_action");
                prepareEffectCamera(mc, "grind");
                advance(t, 10);
            }
            case 5 -> {
                fireEffect(mc, "grind");
                advance(t, 3);
            }
            case 6 -> {
                shot(mc, "packtest_effect_grind_real_action");
                prepareEffectCamera(mc, "anvil_packet");
                advance(t, 10);
            }
            case 7 -> {
                fireEffect(mc, "anvil_packet");
                advance(t, 2);
            }
            case 8 -> {
                shot(mc, "packtest_effect_anvil_server_packet");
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(260);
                mc.options.renderDistance().set(6);
                Map<String, Object> settings = new LinkedHashMap<>();
                settings.put("vsync", false);
                settings.put("frame_limit", "260 (vanilla maximum/unlimited)");
                settings.put("render_distance", 6);
                settings.put("camera", "fixed spectator");
                settings.put("gl_vendor", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VENDOR));
                settings.put("gl_renderer", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_RENDERER));
                settings.put("gl_version", org.lwjgl.opengl.GL11.glGetString(org.lwjgl.opengl.GL11.GL_VERSION));
                checks.put("frame_measurement_settings", settings);
                stagePerformance(mc, false);
                performanceCamera(mc);
                advance(t, 80);
            }
            case 9 -> {
                frameMeasurement = new FrameMeasurement("empty", 60, 180, 32);
                advance(t, 20);
            }
            case 10 -> {
                if (!frameMeasurement.done()) return;
                checks.put("frame_times_empty", frameMeasurement.result());
                stagePerformance(mc, true);
                advance(t, 80);
            }
            case 11 -> {
                frameMeasurement = new FrameMeasurement("active", 60, 180, 32);
                advance(t, 20);
            }
            case 12 -> {
                if (!frameMeasurement.done()) return;
                checks.put("frame_times_32_active", frameMeasurement.result());
                checks.put("frame_measurement_real_render_frames", true);
                shot(mc, "packtest_performance_32_active");
                advance(t, 10);
            }
            case 13 -> {
                mc.options.enableVsync().set(originalVsync);
                mc.options.framerateLimit().set(originalFrameLimit);
                mc.options.renderDistance().set(originalRenderDistance);
                boolean pass = checks.values().stream().noneMatch(v -> v instanceof Boolean b && !b);
                finish(mc, pass, pass ? "all visual, data and runtime checks passed" : "a check failed");
            }
            default -> {
            }
        }
    }

    private static void advance(int tick, int delay) {
        advancedPhase++;
        advancedAt = tick + delay;
    }

    private static void setDayAndCamera(Minecraft mc, long dayTime, BlockPos center, Direction facing) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null) return;
        server.execute(() -> {
            ServerLevel level = server.overworld();
            level.setDayTime(dayTime);
            level.setWeatherParameters(60_000, 0, false, false);
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            restageBurningForge(level, center, facing);
            player.setGameMode(GameType.SPECTATOR);
            double x = center.getX() + .5 + facing.getStepX() * 13;
            double y = center.getY() + 6;
            double z = center.getZ() + .5 + facing.getStepZ() * 13;
            teleportLookAt(player, level, x, y, z, center.getX() + .5, center.getY() + 1, center.getZ() + .5);
        });
    }

    private static void restageBurningForge(ServerLevel level, BlockPos center, Direction facing) {
        Direction right = facing.getClockWise();
        BlockPos pos = local(center, right, facing, 1, 1);
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(pos, ModBlocks.SMITHS_FORGE.get().defaultBlockState().setValue(SmithsForgeBlock.FACING, facing));
        SmithsForgeBlockEntity forge = (SmithsForgeBlockEntity) level.getBlockEntity(pos);
        forge.insertMetal(new ItemStack(Items.IRON_INGOT, 12), 12, false, null);
        forge.insertFuel(new ItemStack(Items.COAL, 16), 16, false, null);
        forge.ignite();
    }

    private static void prepareEffectCamera(Minecraft mc, String effect) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || galleryBase == null) return;
        server.execute(() -> {
            ServerLevel level = server.overworld();
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            BlockPos originalBase = galleryBase.offset(0, 0, 14);
            BlockPos pos = "quench".equals(effect) ? originalBase.offset(2, 0, -4)
                    : "grind".equals(effect) ? originalBase.offset(4, 0, -4) : originalBase.offset(0, 0, -4);
            player.setGameMode(GameType.SPECTATOR);
            teleportLookAt(player, level, pos.getX() + .5, pos.getY() + 2.8, pos.getZ() + 5.5,
                    pos.getX() + .5, pos.getY() + .7, pos.getZ() + .5);
        });
    }

    private static void fireEffect(Minecraft mc, String effect) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || galleryBase == null) return;
        server.execute(() -> {
            ServerLevel level = server.overworld();
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            BlockPos originalBase = galleryBase.offset(0, 0, 14);
            if ("quench".equals(effect)) {
                BlockPos pos = originalBase.offset(2, 0, -4);
                ItemStack tongs = new ItemStack(ModItems.tongs(SmithingTier.DIAMOND));
                WorkpieceCodec.setHeld(tongs, workpiece(Items.IRON_SWORD, 18, WorkpieceState.SHAPED, "sword"));
                player.setItemInHand(InteractionHand.MAIN_HAND, tongs);
                useStation(level, player, pos);
                checks.put("quench_effect_source", "real SmithsTroughBlock.use server action");
            } else if ("grind".equals(effect)) {
                BlockPos pos = originalBase.offset(4, 0, -4);
                ItemStack sword = new ItemStack(Items.IRON_SWORD);
                new QualityData(80, 80, false).apply(sword);
                player.setItemInHand(InteractionHand.MAIN_HAND, sword);
                useStation(level, player, pos);
                useStation(level, player, pos);
                checks.put("grind_effect_source", "real SmithsGrindstoneBlock.use confirmation action");
            } else {
                BlockPos pos = originalBase.offset(0, 0, -4);
                StationEffects.send(level, pos, StationEffects.SPARKS, 1.0F);
                checks.put("anvil_effect_source", "server StationEffectPacket injection; minigame strike remains server-authoritative");
            }
        });
    }

    private static void useStation(ServerLevel level, ServerPlayer player, BlockPos pos) {
        player.setGameMode(GameType.CREATIVE);
        level.getBlockState(pos).use(level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
    }

    private static void performanceCamera(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server == null || performanceBase == null) return;
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayers().get(0);
            player.setGameMode(GameType.SPECTATOR);
            teleportLookAt(player, server.overworld(), performanceBase.getX() + .5, performanceBase.getY() + 11,
                    performanceBase.getZ() + 19.5, performanceBase.getX() + .5, performanceBase.getY() + .5,
                    performanceBase.getZ() + 3.5);
        });
    }

    private static void teleportLookAt(ServerPlayer player, ServerLevel level, double x, double y, double z,
                                       double targetX, double targetY, double targetZ) {
        double dx = targetX - x;
        double dy = targetY - y;
        double dz = targetZ - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        player.teleportTo(level, x, y, z, yaw, pitch);
    }

    private static void stagePerformance(Minecraft mc, boolean active) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) server.execute(() -> stagePerformance(server.overworld(), active));
    }

    private static void stagePerformance(ServerLevel level, boolean active) {
        if (performanceBase == null) return;
        int count = 0;
        for (int row = 0; row < 4; row++) for (int column = 0; column < 8; column++) {
            BlockPos pos = performanceBase.offset((column - 4) * 2, 0, row * 2);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, ModBlocks.SMITHS_FORGE.get().defaultBlockState()
                    .setValue(SmithsForgeBlock.FACING, Direction.SOUTH));
            if (active) {
                SmithsForgeBlockEntity forge = (SmithsForgeBlockEntity) level.getBlockEntity(pos);
                forge.insertMetal(new ItemStack(Items.IRON_INGOT, 16), 16, false, null);
                forge.insertFuel(new ItemStack(Items.COAL, 16), 16, false, null);
                forge.ignite();
            }
            count++;
        }
        checks.put(active ? "active_station_count" : "empty_station_count", count);
    }

    private static void setGuiScale(Minecraft mc, int scale) {
        mc.options.guiScale().set(scale);
        mc.resizeDisplay();
        Map<String, Object> dimensions = new LinkedHashMap<>();
        dimensions.put("scale", scale);
        dimensions.put("width", mc.getWindow().getGuiScaledWidth());
        dimensions.put("height", mc.getWindow().getGuiScaledHeight());
        checks.put("gui_scale_" + scale, dimensions);
        if (scale == 3) checks.put("gui_minimum_320x240", mc.getWindow().getGuiScaledWidth() >= 320
                && mc.getWindow().getGuiScaledHeight() >= 240);
    }

    private static void setGameMode(Minecraft mc, GameType mode) {
        IntegratedServer server = mc.getSingleplayerServer();
        if (server != null) server.execute(() -> server.getPlayerList().getPlayers().get(0).setGameMode(mode));
    }

    /** Makes inventory screenshots deterministic even when the host compositor ignores cursor warps. */
    private static final class FixedInventoryScreen extends InventoryScreen {
        private final boolean qualityTooltip;

        private FixedInventoryScreen(net.minecraft.world.entity.player.Player player, boolean qualityTooltip) {
            super(player);
            this.qualityTooltip = qualityTooltip;
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int fixedX = qualityTooltip ? leftPos + 8 + 18 + 8 : -100;
            int fixedY = qualityTooltip ? topPos + 142 + 8 : -100;
            super.render(graphics, fixedX, fixedY, partialTick);
            if (qualityTooltip) checks.put("inventory_quality_tooltip_rendered", true);
        }
    }

    private static void enableAccessibilityMode() {
        ClientConfig.REDUCED_SCREEN_SHAKE.set(true);
        ClientConfig.REDUCED_FLASHES.set(true);
        ClientConfig.HIGH_CONTRAST_MINIGAMES.set(true);
        ClientConfig.LARGE_MINIGAME_TARGETS.set(true);
        ClientConfig.COLORBLIND_SAFE_TARGETS.set(true);
        ClientConfig.PARTICLE_AMOUNT.set(ClientConfig.ParticleAmount.MINIMAL);
        checks.put("accessibility_mode", "high contrast, colorblind-safe, large targets, reduced flash/shake, minimal particles");
    }

    private static void restoreAccessibilityMode() {
        ClientConfig.REDUCED_SCREEN_SHAKE.set(originalReducedShake);
        ClientConfig.REDUCED_FLASHES.set(originalReducedFlashes);
        ClientConfig.HIGH_CONTRAST_MINIGAMES.set(originalHighContrast);
        ClientConfig.LARGE_MINIGAME_TARGETS.set(originalLargeTargets);
        ClientConfig.COLORBLIND_SAFE_TARGETS.set(originalColorblind);
        ClientConfig.PARTICLE_AMOUNT.set(originalParticles);
        checks.put("accessibility_settings_restored", true);
    }

    private static void openJei(Minecraft mc) {
        if (PackTestJeiPlugin.runtime() == null) {
            checks.put("jei_status", "runtime unavailable");
            return;
        }
        PackTestJeiPlugin.openSmithingRecipes();
        checks.put("jei_status", "smithing category opened");
    }

    private static ItemStack named(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(name));
        return stack;
    }

    private static void multiplayerStep(Minecraft mc) throws IOException {
        multiplayerTicks++;
        if (!multiplayerConnected && mc.screen instanceof TitleScreen && multiplayerTicks >= 60) {
            multiplayerConnected = true;
            ConnectScreen.startConnecting(mc.screen, mc, ServerAddress.parseString(MULTIPLAYER_SERVER),
                    new ServerData("Immersive Smithing visual multiplayer", MULTIPLAYER_SERVER, false), false);
            return;
        }
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        if (multiplayerJoinedAt < 0) multiplayerJoinedAt = multiplayerTicks;
        int joinedTicks = multiplayerTicks - multiplayerJoinedAt;
        // The observer exits immediately after acknowledging the hammer. Consume that acknowledgement
        // before requiring its entity to remain in the level, or disconnect timing can strand the leader.
        if ("leader".equals(MULTIPLAYER_ROLE) && multiplayerReady
                && Files.exists(MULTIPLAYER_SHARED.resolve("hammer-observed"))
                && checks.containsKey("leader_first_person_hammer_captured")) {
            checks.put("leader_remote_tongs_and_hammer_observed", true);
            finish(mc, true, "leader produced real loaded-tongs and hammer multiplayer swings");
            return;
        }
        if (mc.level.players().size() < 2 || !Files.exists(MULTIPLAYER_SHARED.resolve("server-ready"))) {
            if (joinedTicks > 1200) throw new IllegalStateException("two clients did not become visible to each other");
            return;
        }
        Files.createDirectories(MULTIPLAYER_SHARED);
        if ("leader".equals(MULTIPLAYER_ROLE)) {
            if (!multiplayerReady && joinedTicks >= 160) {
                multiplayerReady = true;
                checks.put("leader_loaded_tongs", WorkpieceCodec.isHolding(mc.player.getMainHandItem()));
                shot(mc, "packtest_multiplayer_leader_loaded_tongs");
                marker("leader-ready");
            }
            if (multiplayerReady && Files.exists(MULTIPLAYER_SHARED.resolve("peer-ready"))) {
                boolean hammerReady = Files.exists(MULTIPLAYER_SHARED.resolve("hammer-ready"));
                if (hammerReady) {
                    if (multiplayerHammerReadyAt < 0) multiplayerHammerReadyAt = joinedTicks;
                    // Vanilla keeps the previous held model until its equip transition completes.
                    if (joinedTicks - multiplayerHammerReadyAt < 30) return;
                }
                if (!hammerReady && mc.player.swingTime == 3
                        && !checks.containsKey("leader_first_person_tongs_captured")) {
                    shot(mc, "packtest_multiplayer_first_person_tongs");
                    checks.put("leader_first_person_tongs_captured", true);
                }
                if (hammerReady && mc.player.getMainHandItem().is(ModItems.hammer(SmithingTier.NETHERITE))
                        && mc.player.swingTime == 3 && !checks.containsKey("leader_first_person_hammer_captured")) {
                    shot(mc, "packtest_multiplayer_first_person_hammer");
                    checks.put("leader_first_person_hammer_captured", true);
                }
                if (joinedTicks % 8 == 0) {
                    mc.player.swing(InteractionHand.MAIN_HAND);
                    marker(hammerReady ? "leader-hammer-swinging" : "leader-tongs-swinging");
                }
                if (Files.exists(MULTIPLAYER_SHARED.resolve("hammer-observed"))
                        && checks.containsKey("leader_first_person_hammer_captured")) {
                    checks.put("leader_remote_tongs_and_hammer_observed", true);
                    finish(mc, true, "leader produced real loaded-tongs and hammer multiplayer swings");
                }
            }
        } else if ("peer".equals(MULTIPLAYER_ROLE)) {
            net.minecraft.world.entity.player.Player remote = mc.level.players().stream()
                    .filter(player -> player != mc.player).findFirst().orElse(null);
            if (remote == null) return;
            if (!multiplayerReady && joinedTicks >= 160) {
                multiplayerReady = true;
                checks.put("remote_sky_light", mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, remote.blockPosition()));
                checks.put("remote_block_light", mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, remote.blockPosition()));
                checks.put("fixture_lighting", "vanilla night vision for pose inspection; station lighting tested separately");
                checks.put("remote_loaded_tongs", WorkpieceCodec.isHolding(remote.getMainHandItem()));
                shot(mc, "packtest_multiplayer_remote_idle");
                marker("peer-ready");
            }
            if (multiplayerReady && !remoteSwingObserved
                    && Files.exists(MULTIPLAYER_SHARED.resolve("leader-tongs-swinging"))
                    && remote.swingTime >= 3 && remote.swingTime <= 4) {
                remoteSwingObserved = true;
                checks.put("remote_loaded_tongs_swing_observed", true);
                checks.put("remote_tongs_capture_swing_tick", remote.swingTime);
                shot(mc, "packtest_multiplayer_remote_tongs_swing");
                marker("tongs-observed");
            }
            if (remoteSwingObserved && !remoteHammerSwingObserved
                    && Files.exists(MULTIPLAYER_SHARED.resolve("leader-hammer-swinging"))
                    && remote.getMainHandItem().is(ModItems.hammer(SmithingTier.NETHERITE))
                    && remote.swingTime >= 3 && remote.swingTime <= 4) {
                remoteHammerSwingObserved = true;
                checks.put("remote_hammer_swing_observed", true);
                checks.put("remote_hammer_capture_swing_tick", remote.swingTime);
                shot(mc, "packtest_multiplayer_remote_hammer_swing");
                marker("hammer-observed");
            }
            if (remoteHammerSwingObserved) {
                finish(mc, true, "remote loaded-tongs and hammer swings synchronized and rendered");
            } else if (joinedTicks >= 1200) {
                checks.putIfAbsent("remote_loaded_tongs_swing_observed", remoteSwingObserved);
                checks.putIfAbsent("remote_hammer_swing_observed", false);
                finish(mc, false, "remote loaded-tongs or hammer swing was not observed");
            }
        } else {
            throw new IllegalStateException("unknown multiplayer role " + MULTIPLAYER_ROLE);
        }
    }

    private static void marker(String name) throws IOException {
        Files.writeString(MULTIPLAYER_SHARED.resolve(name), MULTIPLAYER_ROLE + "\n", StandardCharsets.UTF_8);
    }

    private static final class FrameMeasurement {
        private final String label;
        private final int warmupFrames;
        private final int sampleFrames;
        private final int stationCount;
        private final List<Double> intervals = new ArrayList<>();
        private final List<Double> durations = new ArrayList<>();
        private long frameStarted;
        private long previousFrameStarted;
        private long sampleStarted;
        private long sampleEnded;
        private int seen;

        private FrameMeasurement(String label, int warmupFrames, int sampleFrames, int stationCount) {
            this.label = label;
            this.warmupFrames = warmupFrames;
            this.sampleFrames = sampleFrames;
            this.stationCount = stationCount;
        }

        private void beginFrame(long now) {
            frameStarted = now;
        }

        private void endFrame(long now) {
            if (frameStarted == 0) return;
            if (seen >= warmupFrames && durations.size() < sampleFrames) {
                if (sampleStarted == 0) sampleStarted = frameStarted;
                durations.add((now - frameStarted) / 1_000_000.0);
                if (previousFrameStarted != 0) intervals.add((frameStarted - previousFrameStarted) / 1_000_000.0);
                sampleEnded = now;
            } else if (seen >= warmupFrames) {
                durations.add((now - frameStarted) / 1_000_000.0);
                intervals.add((frameStarted - previousFrameStarted) / 1_000_000.0);
                sampleEnded = now;
            }
            previousFrameStarted = frameStarted;
            frameStarted = 0;
            seen++;
        }

        private boolean done() {
            return durations.size() >= sampleFrames && sampleStarted != 0 && sampleEnded - sampleStarted >= 3_000_000_000L;
        }

        private Map<String, Object> result() {
            List<Double> sortedIntervals = intervals.stream().sorted().toList();
            List<Double> sortedDurations = durations.stream().sorted().toList();
            double average = intervals.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double averageDuration = durations.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("label", label);
            result.put("station_count", stationCount);
            result.put("warmup_frames", warmupFrames);
            result.put("minimum_sample_frames", sampleFrames);
            result.put("sample_frames", durations.size());
            result.put("elapsed_sample_ms", round((sampleEnded - sampleStarted) / 1_000_000.0));
            result.put("average_interval_ms", round(average));
            result.put("p50_interval_ms", round(percentile(sortedIntervals, .50)));
            result.put("p95_interval_ms", round(percentile(sortedIntervals, .95)));
            result.put("average_render_tick_ms", round(averageDuration));
            result.put("p50_render_tick_ms", round(percentile(sortedDurations, .50)));
            result.put("p95_render_tick_ms", round(percentile(sortedDurations, .95)));
            result.put("average_fps", round(average == 0 ? 0 : 1000 / average));
            return result;
        }

        private static double percentile(List<Double> values, double quantile) {
            if (values.isEmpty()) return 0;
            return values.get(Math.min(values.size() - 1, (int) Math.ceil(values.size() * quantile) - 1));
        }

        private static double round(double value) {
            return Math.round(value * 100.0) / 100.0;
        }
    }

    private static WorkpieceData workpiece(Item item, int units, WorkpieceState state, String pattern) {
        return new WorkpieceData(new ItemStack(item), ImmersiveSmithing.id("packtest"), new ResourceLocation("minecraft", "iron"), units,
                60, state == WorkpieceState.SHAPED ? 88 : -1, false, state, ImmersiveSmithing.id(pattern));
    }

    private static OpenForgeScreenPacket forgeOffer() {
        List<ItemStack> sticks = List.of(new ItemStack(Items.STICK, 2));
        return new OpenForgeScreenPacket(7, BlockPos.ZERO, Component.translatable("material.immersive_smithing.iron"), 108, 30_000, List.of(
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("smithing/iron_axe"), new ItemStack(Items.IRON_AXE), 27, sticks),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("smithing/iron_boots"), new ItemStack(Items.IRON_BOOTS), 36, List.of()),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("smithing/iron_pickaxe"), new ItemStack(Items.IRON_PICKAXE), 27, sticks),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("smithing/iron_sword"), new ItemStack(Items.IRON_SWORD), 18, List.of(new ItemStack(Items.STICK))),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("smithing/iron_helmet"), new ItemStack(Items.IRON_HELMET), 45, List.of()),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("smithing/shears"), new ItemStack(Items.SHEARS), 18, List.of())));
    }

    private static OpenForgeScreenPacket longForgeOffer() {
        List<ItemStack> sticks = List.of(named(Items.STICK, "Hand Carved Ash Wood Handle with a Very Long Name"));
        return new OpenForgeScreenPacket(7, BlockPos.ZERO,
                Component.literal("A Very Long Localized Tempered Iron Material Family Name"), 108, 30_000, List.of(
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("packtest/long_pickaxe"),
                        named(Items.IRON_PICKAXE, "An Exceptionally Long Tempered Iron Pickaxe Name"), 27, sticks),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("packtest/long_chestplate"),
                        named(Items.IRON_CHESTPLATE, "Reinforced Ceremonial Tempered Iron Chestplate"), 72, List.of()),
                new OpenForgeScreenPacket.Entry(ImmersiveSmithing.id("packtest/long_sword"),
                        named(Items.IRON_SWORD, "Master Smith's Exceptionally Long Tempered Iron Sword"), 18, sticks)));
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> log("screenshot " + name));
    }

    private static void finish(Minecraft mc, boolean pass, String reason) {
        finished = true;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("pass", pass);
        result.put("reason", reason);
        result.put("title_ms", titleMs);
        result.put("world_ms", worldMs);
        result.put("checks", checks);
        try {
            Files.createDirectories(Path.of(OUTPUT));
            Files.writeString(Path.of(OUTPUT, "packtest-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            ImmersiveSmithing.LOGGER.error("PACKTEST could not write result", e);
        }
        log("finished: pass=" + pass + " (" + reason + ")");
        mc.stop();
    }

    private static void log(String message) {
        ImmersiveSmithing.LOGGER.info("PACKTEST {}", message);
    }

    private ClientPackTest() {}
}
