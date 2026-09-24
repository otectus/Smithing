package com.otectus.immersivesmithing.block;

import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.registry.ModSounds;
import com.otectus.immersivesmithing.util.Feedback;
import com.otectus.immersivesmithing.util.StationEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Post-crafting refinement for XP. Use to refine durability, sneak-use to refine efficacy; each refinement is
 * confirmed by a second use. Scores are capped at 100 and Faulty items cannot be refined.
 */
public class SmithsGrindstoneBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final StationShape SHAPE = new StationShape(Shapes.or(
            Block.box(1, 0, 3, 4, 2, 13), Block.box(12, 0, 3, 15, 2, 13),
            Block.box(2, 2, 6, 4, 11, 10), Block.box(12, 2, 6, 14, 11, 10),
            Block.box(1, 9, 7, 15, 11, 9), Block.box(5, 5, 3, 11, 15, 13),
            Block.box(4, 5, 1, 12, 6, 3), Block.box(14, 7, 7, 15, 10, 9), Block.box(14, 6, 7, 16, 7, 9)));
    private static final long CONFIRM_WINDOW_TICKS = 60;

    private record Pending(BlockPos pos, boolean efficacy, int score, long gameTime) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    public SmithsGrindstoneBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.facing(state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && held.getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) refine(sp, pos, held, level.getGameTime());
        return InteractionResult.CONSUME;
    }

    private void refine(ServerPlayer player, BlockPos pos, ItemStack held, long gameTime) {
        if (!ServerConfig.get(ServerConfig.ENABLE_SMITH_GRINDSTONE)) {
            Feedback.fail(player, "grindstone_disabled");
            return;
        }
        QualityData quality = QualityData.get(held).orElse(null);
        if (quality == null) {
            Feedback.info(player, "grindstone_help");
            return;
        }
        if (quality.faulty()) {
            Feedback.fail(player, "grindstone_faulty");
            return;
        }
        boolean efficacy = player.isSecondaryUseActive();
        int score = efficacy ? quality.anvilScore() : quality.forgeScore();
        Component aspect = Component.translatable(efficacy ? "quality.immersive_smithing.aspect.efficacy" : "quality.immersive_smithing.aspect.durability");
        if (score >= 100) {
            Feedback.fail(player, "grindstone_maxed", aspect);
            return;
        }
        int step = ServerConfig.get(ServerConfig.GRINDSTONE_SCORE_STEP);
        int cost = levelCost(score);
        int next = Math.min(100, score + step);

        Pending pending = PENDING.get(player.getUUID());
        boolean confirmed = pending != null && pending.pos.equals(pos) && pending.efficacy == efficacy && pending.score == score
                && gameTime - pending.gameTime <= CONFIRM_WINDOW_TICKS;
        if (!confirmed) {
            PENDING.put(player.getUUID(), new Pending(pos.immutable(), efficacy, score, gameTime));
            Feedback.info(player, "grindstone_confirm", aspect, next - score, cost);
            return;
        }
        PENDING.remove(player.getUUID());
        if (!player.getAbilities().instabuild && player.experienceLevel < cost) {
            Feedback.fail(player, "grindstone_no_xp", cost);
            return;
        }
        if (!player.getAbilities().instabuild) player.giveExperienceLevels(-cost);
        QualityData refined = efficacy ? quality.withAnvilScore(next) : quality.withForgeScore(next);
        refined.apply(held);
        player.level().playSound(null, pos, ModSounds.GRINDSTONE_REFINE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        StationEffects.send(player.serverLevel(), pos, StationEffects.GRIND, 1.0F);
        Feedback.info(player, "grindstone_refined", aspect,
                (efficacy ? refined.efficacyQuality() : refined.durabilityQuality()).displayName());
    }

    /** XP levels for one refinement: the base cost plus one level per configured band of the current score. */
    public static int levelCost(int score) {
        return ServerConfig.get(ServerConfig.GRINDSTONE_BASE_LEVEL_COST) + score / ServerConfig.get(ServerConfig.GRINDSTONE_SCORE_PER_EXTRA_LEVEL);
    }
}
