package com.otectus.immersivesmithing.minigame;

import com.otectus.immersivesmithing.advancement.ModAdvancements;
import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.item.SmithingHammerItem;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.item.SmithingToolItem;
import com.otectus.immersivesmithing.item.SmithingTongsItem;
import com.otectus.immersivesmithing.network.ModNetwork;
import com.otectus.immersivesmithing.network.packet.AnvilStrikeResultPacket;
import com.otectus.immersivesmithing.network.packet.CloseSessionPacket;
import com.otectus.immersivesmithing.network.packet.ForgeActionResultPacket;
import com.otectus.immersivesmithing.network.packet.ForgeStartPacket;
import com.otectus.immersivesmithing.network.packet.OpenAnvilScreenPacket;
import com.otectus.immersivesmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.immersivesmithing.network.packet.SessionResultPacket;
import com.otectus.immersivesmithing.recipe.AuxInventory;
import com.otectus.immersivesmithing.recipe.ForgeRecipeSelection;
import com.otectus.immersivesmithing.recipe.SmithingData;
import com.otectus.immersivesmithing.recipe.SmithingRecipe;
import com.otectus.immersivesmithing.registry.ModSounds;
import com.otectus.immersivesmithing.util.Feedback;
import com.otectus.immersivesmithing.util.StationEffects;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import com.otectus.immersivesmithing.workpiece.WorkpieceState;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Owns every active minigame session. The server generates seeds, keeps its own copy of the minigame state,
 * validates every input (session, player, order, rate, timing, position, distance, tool) and computes scores
 * itself. Clients can only report input events; they never submit a score.
 *
 * <p>Item safety: auxiliary ingredients leave the player's inventory only into the forge's persisted escrow,
 * and leave the escrow exactly once - consumed on completion, or refunded on cancellation. On chunk unload
 * the escrow stays in the saved block entity and is dropped at the forge when it next loads.</p>
 */
public final class SessionManager {
    private static final Map<UUID, SmithingSession> BY_PLAYER = new HashMap<>();
    private static final Map<GlobalPos, SmithingSession> BY_STATION = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final long SELECTION_TIMEOUT_MS = 120_000L;
    private static final long EARLY_INPUT_MS = 250L;
    private static int nextId = new Random().nextInt(1 << 24);
    private static int tickCounter;

    public static Optional<SmithingSession> byPlayer(UUID player) {
        return Optional.ofNullable(BY_PLAYER.get(player));
    }

    @Nullable
    public static SmithingSession atStation(Level level, BlockPos pos) {
        return BY_STATION.get(GlobalPos.of(level.dimension(), pos));
    }

    public static Collection<SmithingSession> all() {
        return List.copyOf(BY_PLAYER.values());
    }

    // ------------------------------------------------------------------ forge

    public static void openForge(ServerPlayer player, SmithsForgeBlockEntity forge, InteractionHand hand, SmithingTier tier) {
        cancelExisting(player);
        int protectedSlot = ForgeRecipeSelection.protectedSlot(player, hand);
        List<SmithingRecipe> eligible = ForgeRecipeSelection.eligible(player, forge, protectedSlot);
        if (eligible.isEmpty()) {
            Feedback.fail(player, ForgeRecipeSelection.whyNone(player, forge, protectedSlot));
            return;
        }
        ForgeSession session = new ForgeSession(nextId(), player.getUUID(), player.level().dimension(), forge.getBlockPos(),
                hand, tier, RANDOM.nextLong(), Util.getMillis());
        session.offered = eligible.stream().map(SmithingRecipe::getId).toList();
        register(session);
        forge.lockForSelection(player.getUUID());

        List<OpenForgeScreenPacket.Entry> entries = new ArrayList<>();
        for (SmithingRecipe r : eligible) {
            entries.add(new OpenForgeScreenPacket.Entry(r.getId(), r.result().copy(), r.metalUnits(),
                    r.auxiliary().stream().map(a -> a.displayStack()).toList()));
        }
        Component familyName = forge.familyData().map(f -> f.displayName()).orElse(Component.literal("?"));
        send(player, new OpenForgeScreenPacket(session.id, forge.getBlockPos(), familyName, forge.moltenUnits(), tier.forgeTimeMs(), entries));
        player.level().playSound(null, forge.getBlockPos(), ModSounds.TONGS_GRAB.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    public static void selectRecipe(ServerPlayer player, int sessionId, ResourceLocation recipeId) {
        ForgeSession session = session(player, sessionId, ForgeSession.class);
        if (session == null || session.isPlaying()) return;
        SmithsForgeBlockEntity forge = forgeAt(player.serverLevel(), session);
        if (forge == null) {
            cancel(session, player, true, message("station_missing"));
            return;
        }
        SmithingRecipe recipe = SmithingData.current().recipe(recipeId);
        if (recipe == null || !session.offered.contains(recipeId)) {
            cancel(session, player, true, message("recipe_unavailable"));
            return;
        }
        if (!toolValid(player, session)) {
            cancel(session, player, true, message("tool_changed"));
            return;
        }
        if (!forge.isReady() || !recipe.family().equals(forge.family()) || forge.moltenUnits() < recipe.metalUnits()) {
            cancel(session, player, true, message("not_enough_metal_now"));
            return;
        }
        int protectedSlot = ForgeRecipeSelection.protectedSlot(player, session.hand);
        List<ItemStack> escrow = AuxInventory.take(player, recipe.auxiliary(), protectedSlot);
        if (escrow == null) {
            cancel(session, player, true, message("missing_auxiliary_now"));
            return;
        }
        forge.beginSession(player.getUUID(), escrow);
        session.recipe = recipe;
        session.pattern = PatternRegistry.forge(recipe.forgePattern());
        session.run = new ForgeRun(ForgeMinigame.phases(session.pattern, session.seed, recipe.metalUnits()), session.pattern.transitionMs());
        session.allowedMs = session.tier.forgeTimeMs();
        int ready = ServerConfig.get(ServerConfig.READY_DELAY_MS);
        session.startMs = Util.getMillis() + ready;
        send(player, new ForgeStartPacket(session.id, session.seed, session.pattern, recipe.metalUnits(), session.allowedMs, ready, recipe.result().copy()));
    }

    public static void forgeAction(ServerPlayer player, int sessionId, int phase, int clientMs, long arrivalMs) {
        ForgeSession session = session(player, sessionId, ForgeSession.class);
        if (session == null || !session.isPlaying() || session.run.isComplete()) return;
        if (phase != session.run.index()) return; // duplicate or out of order
        long elapsed = arrivalMs - session.startMs;
        if (elapsed < -EARLY_INPUT_MS) return;
        int time = SessionTiming.validate(clientMs, elapsed, player.latency, session.lastValidMs);
        if (time > session.allowedMs) {
            finishForge(player, session, true);
            return;
        }
        // Never ignore an in-order input: if a lag spike put it inside the server's transition window, it is
        // scored at the start of the phase (a miss) so client and server stay on the same phase.
        float accuracy = session.run.press(Math.max(time, session.run.phaseStartMs()));
        if (accuracy < 0) return;
        session.lastValidMs = time;

        ServerLevel level = player.serverLevel();
        level.playSound(player, session.pos, ModSounds.FORGE_WORK.get(), SoundSource.BLOCKS, 0.8F, 0.9F + accuracy * 0.3F);
        StationEffects.send(level, session.pos, StationEffects.FORGE_WORK, accuracy);
        send(player, new ForgeActionResultPacket(session.id, phase, accuracy));
        if (session.run.isComplete()) finishForge(player, session, false);
    }

    private static void finishForge(ServerPlayer player, ForgeSession session, boolean timedOut) {
        SmithsForgeBlockEntity forge = forgeAt(player.serverLevel(), session);
        if (forge == null || !toolValid(player, session)) {
            cancel(session, player, true, message("tool_changed"));
            return;
        }
        int score = timedOut ? 0 : session.run.score();
        SmithingRecipe recipe = session.recipe;
        unregister(session);
        forge.completeSession(recipe.metalUnits());

        ItemStack tongs = player.getItemInHand(session.hand);
        WorkpieceData workpiece = new WorkpieceData(recipe.result().copy(), recipe.getId(), recipe.family(), recipe.metalUnits(),
                score, -1, false, WorkpieceState.FORGED, recipe.anvilPattern());
        WorkpieceCodec.setHeld(tongs, workpiece);
        if (!player.getAbilities().instabuild) SmithingToolItem.wearWithoutBreaking(tongs);

        player.level().playSound(null, session.pos, ModSounds.TONGS_GRAB.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        StationEffects.send(player.serverLevel(), session.pos, StationEffects.SPARKS, 0.6F);
        ModAdvancements.award(player, ModAdvancements.INTO_SHAPE);
        send(player, new SessionResultPacket(session.id, SmithingSession.Kind.FORGE, score, timedOut));
    }

    // ------------------------------------------------------------------ anvil

    public static void openAnvil(ServerPlayer player, SmithsAnvilBlockEntity anvil, InteractionHand hand, SmithingTier tier) {
        WorkpieceData workpiece = anvil.workpiece();
        if (workpiece == null || workpiece.isShaped()) return;
        if (anvil.isLockedFor(player.getUUID())) {
            Feedback.fail(player, "station_in_use");
            return;
        }
        cancelExisting(player);
        AnvilSession session = new AnvilSession(nextId(), player.getUUID(), player.level().dimension(), anvil.getBlockPos(),
                hand, tier, RANDOM.nextLong(), Util.getMillis());
        session.pattern = PatternRegistry.anvil(workpiece.anvilPattern());
        session.run = new AnvilRun(session.pattern, session.seed);
        session.allowedMs = Math.round(tier.anvilTimeMs() * session.pattern.timeMultiplier());
        int ready = ServerConfig.get(ServerConfig.READY_DELAY_MS);
        session.startMs = Util.getMillis() + ready;
        register(session);
        anvil.lock(player.getUUID());
        send(player, new OpenAnvilScreenPacket(session.id, anvil.getBlockPos(), workpiece.target().copy(), session.pattern,
                session.seed, session.allowedMs, ready));
    }

    public static void anvilStrike(ServerPlayer player, int sessionId, int sequence, float x, float y, int clientMs, long arrivalMs) {
        AnvilSession session = session(player, sessionId, AnvilSession.class);
        if (session == null || !session.isPlaying() || session.run.isComplete()) return;
        if (sequence <= session.lastSequence) return; // duplicate or replayed packet
        session.lastSequence = sequence;
        if (!Float.isFinite(x) || !Float.isFinite(y) || x < 0F || x > 1F || y < 0F || y > 1F) return;
        long elapsed = arrivalMs - session.startMs;
        if (elapsed < -EARLY_INPUT_MS) return;
        int time = SessionTiming.validate(clientMs, elapsed, player.latency, session.lastValidMs);
        if (time > session.allowedMs) {
            finishAnvil(player, session, true);
            return;
        }
        AnvilRun.StrikeResult result = session.run.strike(time, x, y);
        session.lastValidMs = time;
        ServerLevel level = player.serverLevel();
        if (result != null) {
            player.swing(session.hand, true);
            if (result.hit()) {
                boolean perfect = result.quality() >= 0.9F;
                level.playSound(player, session.pos, perfect ? ModSounds.ANVIL_STRIKE_PERFECT.get() : ModSounds.ANVIL_STRIKE.get(),
                        SoundSource.BLOCKS, 1.0F, 0.9F + result.quality() * 0.2F);
                StationEffects.send(level, session.pos, StationEffects.SPARKS, result.quality());
            } else {
                level.playSound(player, session.pos, ModSounds.ANVIL_MISS.get(), SoundSource.BLOCKS, 0.8F, 1.0F);
                StationEffects.send(level, session.pos, StationEffects.MISS, 0.2F);
            }
        }
        AnvilRun run = session.run;
        send(player, new AnvilStrikeResultPacket(session.id, sequence, result == null, result != null && result.hit(),
                result != null ? result.quality() : 0F, run.index(), run.attempt(), run.spawnMs(), run.sum(), run.samples()));
        if (run.isComplete()) finishAnvil(player, session, false);
    }

    private static void finishAnvil(ServerPlayer player, AnvilSession session, boolean timedOut) {
        SmithsAnvilBlockEntity anvil = anvilAt(player.serverLevel(), session);
        if (anvil == null || anvil.workpiece() == null) {
            cancel(session, player, false, message("station_missing"));
            return;
        }
        unregister(session);
        int score = session.run.score();
        anvil.unlock();
        anvil.setWorkpiece(anvil.workpiece().shaped(score, timedOut));

        ItemStack hammer = player.getItemInHand(session.hand);
        if (hammer.getItem() instanceof SmithingHammerItem) {
            hammer.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(session.hand));
        }
        player.level().playSound(null, session.pos, ModSounds.ANVIL_FINISH.get(), SoundSource.BLOCKS, 1.0F, timedOut ? 0.7F : 1.0F);
        StationEffects.send(player.serverLevel(), session.pos, StationEffects.SPARKS, timedOut ? 0.3F : 1.0F);
        ModAdvancements.award(player, ModAdvancements.HAMMER_AND_STEEL);
        send(player, new SessionResultPacket(session.id, SmithingSession.Kind.ANVIL, score, timedOut));
    }

    // ------------------------------------------------------------------ cancellation and upkeep

    public static void cancelFromClient(ServerPlayer player, int sessionId) {
        SmithingSession session = BY_PLAYER.get(player.getUUID());
        if (session != null && session.id == sessionId) cancel(session, player, true, null);
    }

    public static void onLogout(ServerPlayer player) {
        SmithingSession session = BY_PLAYER.get(player.getUUID());
        if (session != null) cancel(session, player, true, null);
    }

    public static void onDeath(ServerPlayer player) {
        SmithingSession session = BY_PLAYER.get(player.getUUID());
        if (session != null) cancel(session, player, true, message("session_interrupted"));
    }

    /** The station block was broken or replaced: refund immediately, before its contents are dropped. */
    public static void onStationRemoved(Level level, BlockPos pos) {
        SmithingSession session = atStation(level, pos);
        if (session == null || level.getServer() == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId);
        cancel(session, player, true, message("station_missing"));
    }

    /**
     * The station's chunk is unloading and has already been saved. Leave the escrow in the block entity (it is
     * dropped at the forge when the chunk next loads) so nothing is refunded twice.
     */
    public static void onStationUnloaded(Level level, BlockPos pos) {
        SmithingSession session = atStation(level, pos);
        if (session == null) return;
        unregister(session);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SmithsForgeBlockEntity forge) forge.endSession();
        if (be instanceof SmithsAnvilBlockEntity anvil) anvil.unlock();
        if (level.getServer() != null) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId);
            if (player != null) send(player, new CloseSessionPacket(session.id, message("session_interrupted")));
        }
    }

    public static void onServerStopping(MinecraftServer server) {
        for (SmithingSession session : all()) {
            cancel(session, server.getPlayerList().getPlayer(session.playerId), true, null);
        }
        BY_PLAYER.clear();
        BY_STATION.clear();
    }

    public static void tick(MinecraftServer server) {
        if (BY_PLAYER.isEmpty()) return;
        tickCounter++;
        long now = Util.getMillis();
        for (SmithingSession session : all()) {
            ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
            if (player == null) {
                cancel(session, null, false, null);
                continue;
            }
            if (!session.isPlaying()) {
                if (now - session.createdMs > SELECTION_TIMEOUT_MS) cancel(session, player, true, message("session_expired"));
                else if (tickCounter % 5 == 0 && !stillValid(player, session)) cancel(session, player, true, message("session_interrupted"));
                continue;
            }
            long elapsed = now - session.startMs;
            if (elapsed - SessionTiming.graceMs(player.latency) > session.allowedMs) {
                if (session instanceof ForgeSession f) finishForge(player, f, true);
                else if (session instanceof AnvilSession a) finishAnvil(player, a, true);
                continue;
            }
            if (tickCounter % 5 == 0 && !stillValid(player, session)) {
                cancel(session, player, true, message("session_interrupted"));
            }
        }
    }

    private static void cancel(SmithingSession session, @Nullable ServerPlayer player, boolean refundToPlayer, @Nullable Component reason) {
        unregister(session);
        MinecraftServer server = player != null ? player.getServer() : null;
        ServerLevel level = server != null ? server.getLevel(session.dimension) : null;
        if (level == null && player == null) {
            level = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() != null
                    ? net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getLevel(session.dimension) : null;
        }
        if (level != null && level.isLoaded(session.pos)) {
            BlockEntity be = level.getBlockEntity(session.pos);
            if (be instanceof SmithsForgeBlockEntity forge && session instanceof ForgeSession) {
                List<ItemStack> escrow = forge.takeEscrow();
                forge.endSession();
                for (ItemStack stack : escrow) {
                    if (player != null && refundToPlayer) player.getInventory().add(stack);
                    if (!stack.isEmpty()) {
                        Containers.dropItemStack(level, session.pos.getX() + 0.5, session.pos.getY() + 1.0, session.pos.getZ() + 0.5, stack);
                    }
                }
            } else if (be instanceof SmithsAnvilBlockEntity anvil) {
                anvil.unlock();
            }
        }
        if (player != null && reason != null) send(player, new CloseSessionPacket(session.id, reason));
    }

    private static boolean stillValid(ServerPlayer player, SmithingSession session) {
        if (!player.isAlive() || player.level().dimension() != session.dimension) return false;
        double max = ServerConfig.get(ServerConfig.MAX_STATION_DISTANCE);
        if (player.distanceToSqr(session.pos.getX() + 0.5, session.pos.getY() + 0.5, session.pos.getZ() + 0.5) > max * max) return false;
        if (!toolValid(player, session)) return false;
        return session instanceof ForgeSession ? forgeAt(player.serverLevel(), session) != null : anvilAt(player.serverLevel(), session) != null;
    }

    private static boolean toolValid(ServerPlayer player, SmithingSession session) {
        ItemStack held = player.getItemInHand(session.hand);
        if (session instanceof ForgeSession) {
            return held.getItem() instanceof SmithingTongsItem tongs && tongs.tier() == session.tier && !WorkpieceCodec.isHolding(held);
        }
        return held.getItem() instanceof SmithingHammerItem hammer && hammer.tier() == session.tier;
    }

    private static void cancelExisting(ServerPlayer player) {
        SmithingSession existing = BY_PLAYER.get(player.getUUID());
        if (existing != null) cancel(existing, player, true, null);
    }

    @Nullable
    private static <T extends SmithingSession> T session(ServerPlayer player, int sessionId, Class<T> type) {
        SmithingSession session = BY_PLAYER.get(player.getUUID());
        if (session == null || session.id != sessionId || !type.isInstance(session)) return null;
        if (!player.level().dimension().equals(session.dimension)) return null;
        double max = ServerConfig.get(ServerConfig.MAX_STATION_DISTANCE);
        if (player.distanceToSqr(session.pos.getX() + 0.5, session.pos.getY() + 0.5, session.pos.getZ() + 0.5) > max * max) return null;
        return type.cast(session);
    }

    @Nullable
    private static SmithsForgeBlockEntity forgeAt(ServerLevel level, SmithingSession session) {
        if (!level.isLoaded(session.pos)) return null;
        return level.getBlockEntity(session.pos) instanceof SmithsForgeBlockEntity forge ? forge : null;
    }

    @Nullable
    private static SmithsAnvilBlockEntity anvilAt(ServerLevel level, SmithingSession session) {
        if (!level.isLoaded(session.pos)) return null;
        return level.getBlockEntity(session.pos) instanceof SmithsAnvilBlockEntity anvil ? anvil : null;
    }

    private static void register(SmithingSession session) {
        BY_PLAYER.put(session.playerId, session);
        BY_STATION.put(session.station(), session);
    }

    private static void unregister(SmithingSession session) {
        BY_PLAYER.remove(session.playerId, session);
        BY_STATION.remove(session.station(), session);
    }

    private static int nextId() {
        return ++nextId;
    }

    private static Component message(String key) {
        return Component.translatable("message.immersive_smithing." + key);
    }

    private static void send(ServerPlayer player, Object packet) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private SessionManager() {}
}
