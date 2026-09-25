package com.otectus.immersivesmithing.packtest;

import com.google.gson.GsonBuilder;
import com.otectus.immersivesmithing.block.SmithsForgeBlock;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.immersivesmithing.client.screen.AnvilMinigameScreen;
import com.otectus.immersivesmithing.client.screen.ForgeMinigameScreen;
import com.otectus.immersivesmithing.client.screen.GuideScreen;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.minigame.AnvilMinigame;
import com.otectus.immersivesmithing.minigame.AnvilRun;
import com.otectus.immersivesmithing.minigame.ForgeMinigame;
import com.otectus.immersivesmithing.minigame.ForgeRun;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.registry.ModBlocks;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** Opt-in filming fixture, confined to the test agent. It uses actual station actions and server-scored inputs. */
final class ShowcaseCapture {
    private static final String OUTPUT = System.getProperty("immersive_smithing.showcase.output");
    private static final List<Map<String, Object>> MARKERS = new ArrayList<>();
    private static final Map<String, Object> EVIDENCE = new LinkedHashMap<>();
    private static CompletableFuture<Void> pending;
    private static int step;
    private static long entered;
    private static long started;
    private static int action;
    private static int baseY;
    private static BlockPos forge, anvil, trough, grind;
    private static int swordSlot;
    private static int beforeRefine;
    private static boolean finished;
    private static Process encoder;
    private static Thread writer;
    private static final java.util.concurrent.BlockingQueue<Frame> FRAMES = new java.util.concurrent.ArrayBlockingQueue<>(12);
    private record Frame(byte[] bytes, int copies) {}
    private static volatile RuntimeException captureFailure;
    private static long captureStart;
    private static int frameCount;

    static boolean enabled() { return OUTPUT != null; }

    static void tick(Minecraft mc) throws Exception {
        if (finished) return;
        if (captureFailure != null) throw captureFailure;
        if (started == 0) started = Util.getMillis();
        if (Util.getMillis() - started > 150_000) throw new IllegalStateException("Showcase stalled at step " + step);
        if (pending != null && !pending.isDone()) return;
        if (pending != null) { pending.join(); pending = null; }
        long age = Util.getMillis() - entered;
        switch (step) {
            case 0 -> {
                Files.createDirectories(Path.of(OUTPUT));
                baseY = mc.player.blockPosition().getY();
                forge = new BlockPos(-4, baseY, 0);
                anvil = new BlockPos(-1, baseY, 0);
                trough = new BlockPos(2, baseY, 0);
                grind = new BlockPos(5, baseY, 0);
                mc.options.guiScale().set(2);
                mc.resizeDisplay();
                mc.options.fov().set(62);
                mc.options.bobView().set(false);
                mc.options.enableVsync().set(false);
                mc.options.framerateLimit().set(60);
                mc.options.renderDistance().set(8);
                mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0D);
                mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(.8D);
                String audioDevice = mc.getSoundManager().getAvailableSoundDevices().stream()
                        .filter(name -> name.contains("SmithingShowcase")).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Dedicated showcase audio device is unavailable"));
                mc.options.soundDevice().set(audioDevice);
                mc.getSoundManager().reload();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.options.hideGui = true;
                ClientConfig.SHOW_NUMERIC_QUALITY_SCORES.set(true);
                server(mc, ShowcaseCapture::buildWorkshop);
                advance(1, "setup");
            }
            case 1 -> {
                if (age > 2500) {
                    Files.writeString(Path.of(OUTPUT, "ready"), "ready");
                    if (Files.exists(Path.of(OUTPUT, "recording-start-ms"))) advance(2, "workshop");
                }
            }
            case 2 -> {
                if (age > 5500) {
                    server(mc, (level, player) -> {
                        ((SmithsForgeBlockEntity) level.getBlockEntity(forge)).ignite();
                        camera(player, forge, -.8, 3.0, .2);
                    });
                    advance(3, "heat");
                }
            }
            case 3 -> {
                if (age > 6500 && mc.level.getBlockEntity(forge) instanceof SmithsForgeBlockEntity block && block.isReady()) {
                    mc.options.hideGui = false;
                    select(mc, 0);
                    use(mc, forge);
                    advance(4, "choose");
                }
            }
            case 4 -> {
                if (mc.screen instanceof ForgeMinigameScreen screen) {
                    if (action == 0 && age > 900) {
                        screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast)
                                .findFirst().orElseThrow().setValue("sword");
                        action++;
                    }
                    if (action == 1 && age > 2400) {
                        screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                        advance(5, "forge_countdown");
                    }
                }
            }
            case 5 -> {
                if (mc.screen instanceof ForgeMinigameScreen screen) {
                    String stage = field(screen, "stage").toString();
                    if (stage.equals("PLAYING")) {
                        markOnce("forge_play");
                        ForgeRun run = (ForgeRun) field(screen, "run");
                        int elapsed = (int) (Util.getMillis() - (long) field(screen, "clientStartMs"));
                        if (!run.isComplete()) {
                            var phase = run.phases().get(run.index());
                            int local = elapsed - run.phaseStartMs();
                            float accuracy = ForgeMinigame.accuracy(phase, ForgeMinigame.position(phase, local / 1000F));
                            if (local > 900 && accuracy >= .68F && accuracy <= .93F) screen.keyPressed(GLFW.GLFW_KEY_SPACE, 0, 0);
                        }
                    } else if (stage.equals("RESULT")) {
                        EVIDENCE.put("forge_score", field(screen, "resultScore"));
                        EVIDENCE.put("forge_timed_out", field(screen, "resultTimedOut"));
                        advance(6, "forge_result");
                    }
                }
            }
            case 6 -> {
                if (age > 1900) {
                    mc.screen.onClose();
                    if (!WorkpieceCodec.isHolding(mc.player.getMainHandItem())) throw new IllegalStateException("Forge did not produce a workpiece");
                    server(mc, (level, player) -> camera(player, anvil, -.8, 2.8, .15));
                    advance(7, "transfer");
                }
            }
            case 7 -> {
                if (action == 0 && age > 800) { use(mc, anvil); action++; }
                if (action == 1 && age > 1800) { select(mc, 1); action++; }
                if (action == 2 && age > 2500) { use(mc, anvil); advance(8, "anvil_countdown"); }
            }
            case 8 -> {
                if (mc.screen instanceof AnvilMinigameScreen screen) {
                    String stage = field(screen, "stage").toString();
                    if (stage.equals("PLAYING")) {
                        markOnce("anvil_play");
                        AnvilRun run = (AnvilRun) field(screen, "run");
                        int elapsed = (int) (Util.getMillis() - (long) field(screen, "clientStartMs"));
                        if (!run.isComplete() && elapsed - run.spawnMs() >= AnvilMinigame.idealMs(run.pattern()) - 40) {
                            var target = run.currentTarget();
                            int x = (int) field(screen, "left") + 12;
                            int y = (int) field(screen, "top") + 28;
                            screen.mouseClicked(x + target.x() * 176, y + target.y() * 176, 0);
                            mark("strike_" + run.index());
                        }
                    } else if (stage.equals("RESULT")) {
                        EVIDENCE.put("anvil_score", field(screen, "resultScore"));
                        EVIDENCE.put("anvil_faulty", field(screen, "resultFaulty"));
                        advance(9, "anvil_result");
                    }
                }
            }
            case 9 -> {
                if (age > 1800) {
                    mc.screen.onClose();
                    select(mc, 0);
                    advance(10, "pickup");
                }
            }
            case 10 -> {
                if (action == 0 && age > 300) { use(mc, anvil); action++; }
                if (action == 1 && age > 1600) {
                    if (!WorkpieceCodec.isHolding(mc.player.getMainHandItem())) throw new IllegalStateException("Anvil pickup failed");
                    server(mc, (level, player) -> camera(player, trough, -.6, 2.7, .15));
                    advance(11, "quench");
                }
            }
            case 11 -> {
                if (action == 0 && age > 1000) { use(mc, trough); action++; }
                if (age > 3500) {
                    swordSlot = -1;
                    for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).is(Items.IRON_SWORD)) swordSlot = i;
                    if (swordSlot < 0) throw new IllegalStateException("Quench did not produce the sword");
                    var quality = QualityData.get(mc.player.getInventory().getItem(swordSlot)).orElseThrow();
                    EVIDENCE.put("quenched_quality", quality);
                    beforeRefine = quality.forgeScore();
                    select(mc, swordSlot);
                    server(mc, (level, player) -> camera(player, grind, -.9, 2.8, .05));
                    advance(12, "refine");
                }
            }
            case 12 -> {
                if (action == 0 && age > 600) { use(mc, grind); action++; }
                if (action == 1 && age > 1300) { use(mc, grind); action++; }
                if (age > 3200) {
                    var quality = QualityData.get(mc.player.getInventory().getItem(swordSlot)).orElseThrow();
                    EVIDENCE.put("refined_quality", quality);
                    if (quality.forgeScore() != Math.min(100, beforeRefine + 5) || beforeRefine == 100)
                        throw new IllegalStateException("Real refinement did not improve the forged item");
                    mc.setScreen(new InventoryScreen(mc.player) {
                        @Override public void render(GuiGraphics g, int mx, int my, float partial) {
                            super.render(g, leftPos + 8 + swordSlot * 18 + 8, topPos + 142 + 8, partial);
                        }
                    });
                    advance(13, "quality");
                }
            }
            case 13 -> {
                if (age > 3300) { mc.setScreen(new GuideScreen(1)); advance(14, "guide"); }
            }
            case 14 -> {
                if (age > 4200) {
                    mc.setScreen(null);
                    mc.options.hideGui = true;
                    server(mc, (level, player) -> camera(player, new BlockPos(1, baseY, 0), 4.8, 7.4, 1.3));
                    advance(15, "outro");
                }
            }
            case 15 -> {
                if (age > 4200) {
                    mark("end");
                    EVIDENCE.put("pass", true);
                    EVIDENCE.put("capture", "Actual game client, scripted inputs, server-authoritative forging, shaping, quenching and refinement");
                    EVIDENCE.put("markers", MARKERS);
                    Files.writeString(Path.of(OUTPUT, "result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(EVIDENCE));
                    Files.writeString(Path.of(OUTPUT, "finished"), "success");
                    finished = true;
                    FRAMES.put(new Frame(new byte[0], 0));
                    writer.join(20_000);
                    if (writer.isAlive() || encoder.waitFor() != 0 || captureFailure != null)
                        throw new IllegalStateException("Framebuffer encoder failed", captureFailure);
                    mc.stop();
                }
            }
        }
    }

    /** Capture the rendered game framebuffer directly; Xwayland root captures can be black. */
    static void captureFrame(Minecraft mc) {
        if (step < 2 || finished || captureFailure != null) return;
        try {
            if (encoder == null) {
                int width = mc.getMainRenderTarget().width, height = mc.getMainRenderTarget().height;
                encoder = new ProcessBuilder("ffmpeg", "-hide_banner", "-loglevel", "warning", "-y",
                        "-f", "rawvideo", "-pixel_format", "rgba", "-video_size", width + "x" + height,
                        "-framerate", "30", "-i", "pipe:0", "-an", "-c:v", "libx264", "-preset", "ultrafast",
                        "-crf", "18", "-pix_fmt", "yuv420p", "-threads", "3", "-movflags", "+faststart",
                        Path.of(OUTPUT, "video.mp4").toString())
                        .redirectError(Path.of(OUTPUT, "video-encoder.log").toFile()).start();
                captureStart = System.currentTimeMillis();
                Files.writeString(Path.of(OUTPUT, "video-start-ms"), Long.toString(captureStart));
                writer = new Thread(() -> {
                    try (var stream = encoder.getOutputStream()) {
                        while (true) {
                            Frame frame = FRAMES.take();
                            if (frame.copies() == 0) break;
                            for (int i = 0; i < frame.copies(); i++) stream.write(frame.bytes());
                        }
                    } catch (Exception e) { captureFailure = new RuntimeException("Video writer failed", e); }
                }, "smithing-showcase-video");
                writer.start();
            }
            int targetFrames = 1 + (int) ((System.currentTimeMillis() - captureStart) * 30 / 1000);
            if (targetFrames <= frameCount) return;
            try (var frame = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                if (frameCount == 0) frame.writeToFile(Path.of(OUTPUT, "capture-preview.png"));
                int[] pixels = frame.getPixelsRGBA();
                var bytes = java.nio.ByteBuffer.allocate(pixels.length * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                bytes.asIntBuffer().put(pixels);
                if (!FRAMES.offer(new Frame(bytes.array(), targetFrames - frameCount))) throw new IllegalStateException("Video encoder queue is full");
                frameCount = targetFrames;
            }
        } catch (Exception e) { captureFailure = new RuntimeException("Framebuffer capture failed", e); }
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().selected = slot;
        server(mc, (level, player) -> { player.getInventory().selected = slot; player.inventoryMenu.broadcastChanges(); });
    }

    private static void use(Minecraft mc, BlockPos position) {
        // Exercise the real station entry point on the integrated server, including its normal packets.
        server(mc, (level, player) -> level.getBlockState(position).use(level, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false)));
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private static void server(Minecraft mc, BiConsumer<ServerLevel, ServerPlayer> work) {
        var server = mc.getSingleplayerServer();
        if (server == null) throw new IllegalStateException("Showcase needs an isolated integrated server");
        // Calls within one tick retain their ordering on the server executor.
        pending = CompletableFuture.runAsync(() -> work.accept(server.overworld(), server.getPlayerList().getPlayers().get(0)), server);
    }

    private static void camera(ServerPlayer player, BlockPos subject, double dx, double dz, double dy) {
        double x = subject.getX() + .5 + dx, y = baseY + dy, z = subject.getZ() + .5 + dz;
        double tx = subject.getX() + .5 - x, ty = baseY + .65 - (y + player.getEyeHeight()), tz = subject.getZ() + .5 - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-tx, tz));
        float pitch = (float) -Math.toDegrees(Math.atan2(ty, Math.hypot(tx, tz)));
        player.teleportTo(player.serverLevel(), x, y, z, yaw, pitch);
    }

    private static void buildWorkshop(ServerLevel level, ServerPlayer player) {
        level.setDayTime(4500);
        level.setWeatherParameters(60000, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, level.getServer());
        for (int x = -9; x <= 9; x++) for (int z = -5; z <= 10; z++) {
            level.setBlockAndUpdate(new BlockPos(x, baseY - 1, z), (Math.floorMod(x + z, 4) == 0 ? Blocks.STONE_BRICKS : Blocks.POLISHED_ANDESITE).defaultBlockState());
            for (int y = 0; y < 8; y++) level.setBlockAndUpdate(new BlockPos(x, baseY + y, z), Blocks.AIR.defaultBlockState());
        }
        for (int x = -8; x <= 8; x++) {
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(new BlockPos(x, baseY + y, -3),
                    (y < 1 ? Blocks.STONE_BRICKS : Math.floorMod(x, 4) == 0 ? Blocks.STRIPPED_SPRUCE_LOG : Blocks.SPRUCE_PLANKS).defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, baseY + 4, -3), Blocks.SPRUCE_SLAB.defaultBlockState());
        }
        for (int x : new int[]{-8, 8}) {
            for (int y = 0; y <= 3; y++) level.setBlockAndUpdate(new BlockPos(x, baseY + y, 2), Blocks.SPRUCE_LOG.defaultBlockState());
            for (int z = -3; z <= 2; z++) level.setBlockAndUpdate(new BlockPos(x, baseY + 4, z), Blocks.SPRUCE_LOG.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, baseY + 3, 1), Blocks.LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
        for (int x : new int[]{-6, 6, 7}) level.setBlockAndUpdate(new BlockPos(x, baseY, -2), Blocks.BARREL.defaultBlockState());
        for (int y = 0; y <= 6; y++) level.setBlockAndUpdate(new BlockPos(-4, baseY + y, -3), Blocks.STONE_BRICKS.defaultBlockState());
        level.setBlockAndUpdate(forge, ModBlocks.SMITHS_FORGE.get().defaultBlockState().setValue(SmithsForgeBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(anvil, ModBlocks.SMITHS_ANVIL.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(trough, ModBlocks.SMITHS_TROUGH.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(grind, ModBlocks.SMITHS_GRINDSTONE.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        var block = (SmithsForgeBlockEntity) level.getBlockEntity(forge);
        block.insertMetal(new ItemStack(Items.IRON_INGOT, 2), 2, false, null);
        block.insertFuel(new ItemStack(Items.COAL, 8), 8, false, null);
        ((SmithsTroughBlockEntity) level.getBlockEntity(trough)).addBucket();
        player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(ModItems.tongs(SmithingTier.IRON)));
        player.getInventory().setItem(1, new ItemStack(ModItems.hammer(SmithingTier.IRON)));
        player.getInventory().setItem(8, new ItemStack(ModItems.SMITHING_GUIDE.get()));
        player.getInventory().setItem(10, new ItemStack(Items.STICK, 1));
        player.giveExperienceLevels(40);
        player.getInventory().selected = 0;
        player.inventoryMenu.broadcastChanges();
        camera(player, new BlockPos(0, baseY, 0), 3.8, 8.8, .5);
    }

    private static Object field(Object target, String name) throws Exception {
        var field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void advance(int next, String marker) {
        step = next;
        action = 0;
        entered = Util.getMillis();
        mark(marker);
    }

    private static void markOnce(String name) {
        if (MARKERS.stream().noneMatch(m -> name.equals(m.get("name")))) mark(name);
    }

    private static void mark(String name) {
        MARKERS.add(Map.of("name", name, "wall_ms", System.currentTimeMillis()));
    }

    private ShowcaseCapture() {}
}
