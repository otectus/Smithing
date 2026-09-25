package com.otectus.immersivesmithing.block;

import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.item.SmithingHammerItem;
import com.otectus.immersivesmithing.item.SmithingTongsItem;
import com.otectus.immersivesmithing.minigame.SessionManager;
import com.otectus.immersivesmithing.quality.SigningService;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.registry.ModSounds;
import com.otectus.immersivesmithing.util.Feedback;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The Smith's Anvil: a forging workstation, separate from the vanilla anvil's repair/rename UI. It holds one
 * workpiece; a Smithing Hammer starts the Anvil minigame, Smithing Tongs place and remove the workpiece.
 */
public class SmithsAnvilBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final StationShape SHAPE = new StationShape(Shapes.or(
            Block.box(4, 0, 2, 12, 9, 4),
            Block.box(2, 0, 4, 14, 9, 12),
            Block.box(4, 0, 12, 12, 9, 14),
            Block.box(4, 2, 1.75, 12, 3, 2),
            Block.box(1.75, 2, 4, 2, 3, 12),
            Block.box(14, 2, 4, 14.25, 3, 12),
            Block.box(4, 2, 14, 12, 3, 14.25),
            Block.box(2, 2, 3.75, 4, 3, 4),
            Block.box(12, 2, 3.75, 14, 3, 4),
            Block.box(2, 2, 12, 4, 3, 12.25),
            Block.box(12, 2, 12, 14, 3, 12.25),
            Block.box(4, 7, 1.75, 12, 8, 2),
            Block.box(1.75, 7, 4, 2, 8, 12),
            Block.box(14, 7, 4, 14.25, 8, 12),
            Block.box(4, 7, 14, 12, 8, 14.25),
            Block.box(2, 7, 3.75, 4, 8, 4),
            Block.box(12, 7, 3.75, 14, 8, 4),
            Block.box(2, 7, 12, 4, 8, 12.25),
            Block.box(12, 7, 12, 14, 8, 12.25),
            Block.box(4, 9, 5, 12, 10, 13),
            Block.box(5, 10, 6, 11, 11, 12),
            Block.box(6, 11, 6, 10, 13, 12),
            Block.box(5, 13, 4, 11, 14, 14),
            Block.box(4, 14, 4, 12, 16, 15),
            Block.box(5, 14, 2, 11, 15.5, 4),
            Block.box(6, 14, 1, 10, 15, 2),
            Block.box(7, 14, 0, 9, 14.5, 1)));

    public SmithsAnvilBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
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
        return SHAPE.facing(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SmithsAnvilBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof SmithsAnvilBlockEntity anvil) {
            SessionManager.onStationRemoved(level, pos);
            anvil.dropWorkpiece();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack held = player.getItemInHand(hand);
        if (player.isSecondaryUseActive() && held.getItem() instanceof BlockItem) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof SmithsAnvilBlockEntity anvil) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        interact(sp, anvil, held, hand);
        return InteractionResult.CONSUME;
    }

    private void interact(ServerPlayer player, SmithsAnvilBlockEntity anvil, ItemStack held, InteractionHand hand) {
        if (anvil.isLockedFor(player.getUUID())) {
            Feedback.fail(player, "station_in_use");
            return;
        }
        WorkpieceData onAnvil = anvil.workpiece();
        // A finished piece brought back to the anvil by its smith: sign it, or change the signature.
        if (player.isSecondaryUseActive() && SigningService.canSign(held, player)) {
            SigningService.open(player, player.getInventory().selected);
            return;
        }
        if (held.getItem() instanceof SmithingTongsItem) {
            Optional<WorkpieceData> carried = WorkpieceCodec.getHeld(held);
            if (carried.isPresent()) {
                if (onAnvil != null) {
                    Feedback.fail(player, "anvil_occupied");
                    return;
                }
                anvil.setWorkpiece(carried.get());
                WorkpieceCodec.clearHeld(held);
                player.level().playSound(null, anvil.getBlockPos(), ModSounds.TONGS_GRAB.get(), SoundSource.BLOCKS, 0.9F, 1.1F);
                Feedback.info(player, carried.get().isShaped() ? "workpiece_to_trough" : "use_hammer");
            } else if (onAnvil != null) {
                WorkpieceCodec.setHeld(held, anvil.takeWorkpiece());
                player.level().playSound(null, anvil.getBlockPos(), ModSounds.TONGS_GRAB.get(), SoundSource.BLOCKS, 0.9F, 0.9F);
            } else {
                Feedback.fail(player, "anvil_empty");
            }
            return;
        }
        if (held.getItem() instanceof SmithingHammerItem hammer) {
            if (onAnvil == null) Feedback.fail(player, "anvil_empty");
            else if (onAnvil.isShaped()) Feedback.fail(player, "already_shaped");
            else SessionManager.openAnvil(player, anvil, hand, hammer.tier());
            return;
        }
        if (held.is(ModItems.HOT_WORKPIECE.get())) {
            Optional<WorkpieceData> data = WorkpieceCodec.getHeld(held);
            if (onAnvil != null) {
                Feedback.fail(player, "anvil_occupied");
            } else if (data.isPresent()) {
                anvil.setWorkpiece(data.get());
                held.shrink(1);
                player.level().playSound(null, anvil.getBlockPos(), ModSounds.TONGS_GRAB.get(), SoundSource.BLOCKS, 0.9F, 1.1F);
            }
            return;
        }
        if (onAnvil == null) Feedback.info(player, "anvil_empty");
        else if (onAnvil.isShaped()) Feedback.info(player, "workpiece_to_trough");
        else Feedback.info(player, "use_hammer");
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SmithsAnvilBlockEntity anvil) || !anvil.hasWorkpiece()) return;
        if (random.nextFloat() < 0.3F * ClientConfig.scaleParticles(10) / 10F) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 1.05,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.02, 0);
        }
        // Incandescent stock does not burn: strike sparks come only from accepted server actions.
    }
}
