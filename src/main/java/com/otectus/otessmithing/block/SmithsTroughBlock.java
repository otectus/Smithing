package com.otectus.otessmithing.block;

import com.otectus.otessmithing.advancement.ModAdvancements;
import com.otectus.otessmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.otessmithing.item.SmithingTongsItem;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.quality.SmithingQuality;
import com.otectus.otessmithing.registry.ModItems;
import com.otectus.otessmithing.registry.ModSounds;
import com.otectus.otessmithing.util.Feedback;
import com.otectus.otessmithing.util.StationEffects;
import com.otectus.otessmithing.workpiece.WorkpieceCodec;
import com.otectus.otessmithing.workpiece.WorkpieceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** The Smith's Trough: water in, finished equipment out. No minigame, no cooling over time. */
public class SmithsTroughBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE_NS = Block.box(1, 0, 3, 15, 9, 13);
    private static final VoxelShape SHAPE_EW = Block.box(3, 0, 1, 13, 9, 15);

    public SmithsTroughBlock(Properties properties) {
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmithsTroughBlockEntity(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SmithsTroughBlockEntity trough ? Math.round(trough.fillFraction() * 15) : 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && held.getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SmithsTroughBlockEntity trough) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        interact(sp, trough, held, hand);
        return InteractionResult.CONSUME;
    }

    private void interact(ServerPlayer player, SmithsTroughBlockEntity trough, ItemStack held, InteractionHand hand) {
        if (held.is(Items.WATER_BUCKET)) {
            if (!trough.canAcceptBucket()) {
                Feedback.fail(player, "trough_full");
                return;
            }
            trough.addBucket();
            if (!player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
            }
            player.level().playSound(null, trough.getBlockPos(), ModSounds.TROUGH_FILL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }
        if (held.is(Items.BUCKET)) {
            if (trough.takeBucket()) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.WATER_BUCKET)));
                player.level().playSound(null, trough.getBlockPos(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                Feedback.fail(player, "trough_not_enough_water");
            }
            return;
        }
        if (held.getItem() instanceof SmithingTongsItem) {
            Optional<WorkpieceData> workpiece = WorkpieceCodec.getHeld(held);
            if (workpiece.isEmpty()) Feedback.fail(player, "nothing_to_quench");
            else quench(player, trough, held, hand, workpiece.get());
            return;
        }
        if (held.is(ModItems.HOT_WORKPIECE.get())) {
            Feedback.fail(player, "use_tongs_to_quench");
            return;
        }
        if (held.isEmpty()) {
            Feedback.info(player, "trough_status", trough.quenchesLeft());
            return;
        }
        Feedback.fail(player, "trough_only_workpieces");
    }

    private void quench(ServerPlayer player, SmithsTroughBlockEntity trough, ItemStack tongs, InteractionHand hand, WorkpieceData workpiece) {
        if (!workpiece.isShaped()) {
            Feedback.fail(player, "hammer_first");
            return;
        }
        if (!trough.consumeQuench()) {
            Feedback.fail(player, "trough_empty");
            return;
        }
        ItemStack result = workpiece.target().copy();
        QualityData quality = new QualityData(workpiece.forgeScore(), workpiece.anvilScore(), workpiece.faulty());
        quality.apply(result);
        // As if crafted: the crafted statistic, plus items that initialise their NBT on craft (Spartan throwing weapons).
        result.onCraftedBy(player.level(), player, result.getCount());
        WorkpieceCodec.clearHeld(tongs);

        BlockPos pos = trough.getBlockPos();
        if (!player.getInventory().add(result) && !result.isEmpty()) {
            Containers.dropItemStack(player.level(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, result);
        }
        // One wear per completed transfer cycle. The tongs are empty now, so wear can safely break them.
        tongs.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

        player.level().playSound(null, pos, ModSounds.QUENCH.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        StationEffects.send(player.serverLevel(), pos, StationEffects.STEAM, 1.0F);
        Feedback.info(player, "quenched", quality.quality().displayName());

        ModAdvancements.award(player, ModAdvancements.HISS);
        SmithingQuality label = quality.quality();
        if (label == SmithingQuality.FAULTY) ModAdvancements.award(player, ModAdvancements.THATLL_BUFF_OUT);
        if (label == SmithingQuality.FINE || label == SmithingQuality.MASTERWORK) ModAdvancements.award(player, ModAdvancements.FINE_WORK);
        if (label == SmithingQuality.MASTERWORK) ModAdvancements.award(player, ModAdvancements.MASTER_SMITH);
    }
}
