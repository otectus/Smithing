package com.otectus.immersivesmithing.blockentity;

import com.otectus.immersivesmithing.minigame.SessionManager;
import com.otectus.immersivesmithing.registry.ModBlockEntities;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** Holds at most one workpiece. No automation capability is exposed: only players move workpieces. */
public class SmithsAnvilBlockEntity extends BlockEntity {
    @Nullable
    private WorkpieceData workpiece;
    @Nullable
    private UUID lockedBy; // transient: locks never survive a restart

    public SmithsAnvilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMITHS_ANVIL.get(), pos, state);
    }

    /** The visible workpiece/pile can rise above the block; retain normal frustum culling. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0, 0.5, 0);
    }

    @Nullable
    public WorkpieceData workpiece() {
        return workpiece;
    }

    public boolean hasWorkpiece() {
        return workpiece != null;
    }

    public void setWorkpiece(@Nullable WorkpieceData data) {
        this.workpiece = data;
        sync();
    }

    @Nullable
    public WorkpieceData takeWorkpiece() {
        WorkpieceData data = workpiece;
        workpiece = null;
        sync();
        return data;
    }

    public boolean isLocked() {
        return lockedBy != null;
    }

    public boolean isLockedFor(UUID player) {
        return lockedBy != null && !lockedBy.equals(player);
    }

    public void lock(UUID player) {
        lockedBy = player;
    }

    public void unlock() {
        lockedBy = null;
    }

    /** Drops the workpiece as a Hot Workpiece item so it is never lost with the block. */
    public void dropWorkpiece() {
        if (level == null || workpiece == null) return;
        ItemStack item = new ItemStack(ModItems.HOT_WORKPIECE.get());
        WorkpieceCodec.setHeld(item, workpiece);
        workpiece = null;
        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, item);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) SessionManager.onStationUnloaded(level, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (workpiece != null) tag.put("Workpiece", WorkpieceCodec.save(workpiece));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        workpiece = tag.contains("Workpiece", Tag.TAG_COMPOUND) ? WorkpieceCodec.load(tag.getCompound("Workpiece")).orElse(null) : null;
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }
}
