package com.otectus.otessmithing.packtest;

import com.google.gson.GsonBuilder;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.block.SmithsAnvilBlock;
import com.otectus.otessmithing.block.SmithsForgeBlock;
import com.otectus.otessmithing.block.SmithsGrindstoneBlock;
import com.otectus.otessmithing.block.SmithsTroughBlock;
import com.otectus.otessmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.otessmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.otessmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.otessmithing.client.screen.AnvilMinigameScreen;
import com.otectus.otessmithing.client.screen.ForgeMinigameScreen;
import com.otectus.otessmithing.client.screen.GuideScreen;
import com.otectus.otessmithing.item.SmithingTier;
import com.otectus.otessmithing.minigame.ForgePattern;
import com.otectus.otessmithing.minigame.PatternRegistry;
import com.otectus.otessmithing.network.packet.ForgeStartPacket;
import com.otectus.otessmithing.network.packet.OpenAnvilScreenPacket;
import com.otectus.otessmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.recipe.SmithingData;
import com.otectus.otessmithing.registry.ModBlocks;
import com.otectus.otessmithing.registry.ModItems;
import com.otectus.otessmithing.workpiece.WorkpieceCodec;
import com.otectus.otessmithing.workpiece.WorkpieceData;
import com.otectus.otessmithing.workpiece.WorkpieceState;
import net.minecraft.Util;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Drives a client from the title screen into a fresh flat world, checks that Ote's Smithing built its data inside
 * whatever modpack is loaded, stages every station and screen for framebuffer screenshots, writes
 * {@code packtest-result.json} into the output directory and quits.
 */
@Mod.EventBusSubscriber(modid = PackTestAgent.MOD_ID, value = Dist.CLIENT)
public final class ClientPackTest {
    private static final String OUTPUT = System.getProperty(PackTestAgent.OUTPUT_PROPERTY);
    private static final boolean CREATE_WORLD = !"false".equals(System.getProperty("otes_smithing.packtest.world"));
    private static final Set<String> EXPECT = Set.of(System.getProperty("otes_smithing.packtest.expect", "").split(","));

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

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (OUTPUT == null || finished || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
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
            OtesSmithing.LOGGER.error("PACKTEST agent failed at world tick {}", worldTicks, e);
            checks.put("agent_exception", e.toString());
            finish(mc, false, "agent exception");
        }
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
                    PatternRegistry.anvil(OtesSmithing.id("sword")), 42L, 30_000, 0)));
            case 345 -> shot(mc, "packtest_anvil");
            case 350 -> mc.setScreen(new GuideScreen(2));
            case 370 -> shot(mc, "packtest_guide");
            case 375 -> mc.setScreen(new InventoryScreen(mc.player));
            case 410 -> shot(mc, "packtest_inventory");
            case 420 -> {
                mc.setScreen(null);
                boolean pass = checks.values().stream().noneMatch(v -> v instanceof Boolean b && !b);
                finish(mc, pass, pass ? "all checks passed" : "a check failed");
            }
            default -> {
            }
        }
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
        checks.put("vanilla_explicit_recipes", data.recipe(OtesSmithing.id("smithing/iron_sword")) != null);
        checks.put("material_families", data.materials().families().size());
        if (server != null) {
            checks.put("iron_sword_crafting_disabled", server.getRecipeManager().byKey(new ResourceLocation("minecraft", "iron_sword")).isEmpty());
        }
        if (EXPECT.contains("spartan")) {
            checks.put("spartan_iron_longsword", data.recipe(OtesSmithing.id("compat/spartanweaponry/iron_longsword")) != null);
            checks.put("spartan_netherite_longsword", data.recipe(OtesSmithing.id("compat/spartanweaponry/netherite_longsword")) != null);
            checks.put("spartan_iron_javelin", data.recipe(OtesSmithing.id("compat/spartanweaponry/iron_javelin")) != null);
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
            for (int x = -7; x <= 7; x++) {
                for (int z = -8; z <= 3; z++) {
                    level.setBlockAndUpdate(base.offset(x, -1, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
                    for (int y = 0; y <= 4; y++) level.setBlockAndUpdate(base.offset(x, y, z), Blocks.AIR.defaultBlockState());
                }
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
            player.teleportTo(level, base.getX() + 0.5, base.getY(), base.getZ() + 1.5, 180F, 20F);
        });
    }

    private static WorkpieceData workpiece(Item item, int units, WorkpieceState state, String pattern) {
        return new WorkpieceData(new ItemStack(item), OtesSmithing.id("packtest"), new ResourceLocation("minecraft", "iron"), units,
                60, state == WorkpieceState.SHAPED ? 88 : -1, false, state, OtesSmithing.id(pattern));
    }

    private static OpenForgeScreenPacket forgeOffer() {
        List<ItemStack> sticks = List.of(new ItemStack(Items.STICK, 2));
        return new OpenForgeScreenPacket(7, BlockPos.ZERO, Component.translatable("material.otes_smithing.iron"), 108, 30_000, List.of(
                new OpenForgeScreenPacket.Entry(OtesSmithing.id("smithing/iron_axe"), new ItemStack(Items.IRON_AXE), 27, sticks),
                new OpenForgeScreenPacket.Entry(OtesSmithing.id("smithing/iron_boots"), new ItemStack(Items.IRON_BOOTS), 36, List.of()),
                new OpenForgeScreenPacket.Entry(OtesSmithing.id("smithing/iron_pickaxe"), new ItemStack(Items.IRON_PICKAXE), 27, sticks),
                new OpenForgeScreenPacket.Entry(OtesSmithing.id("smithing/iron_sword"), new ItemStack(Items.IRON_SWORD), 18, List.of(new ItemStack(Items.STICK))),
                new OpenForgeScreenPacket.Entry(OtesSmithing.id("smithing/iron_helmet"), new ItemStack(Items.IRON_HELMET), 45, List.of()),
                new OpenForgeScreenPacket.Entry(OtesSmithing.id("smithing/shears"), new ItemStack(Items.SHEARS), 18, List.of())));
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
            OtesSmithing.LOGGER.error("PACKTEST could not write result", e);
        }
        log("finished: pass=" + pass + " (" + reason + ")");
        mc.stop();
    }

    private static void log(String message) {
        OtesSmithing.LOGGER.info("PACKTEST {}", message);
    }

    private ClientPackTest() {}
}
