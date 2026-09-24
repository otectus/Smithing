package com.otectus.immersivesmithing.blockentity;

import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Water for quenching, measured in millibuckets. Pipes may fill it; nothing can drain it automatically. */
public class SmithsTroughBlockEntity extends BlockEntity {
    private int water;

    private final LazyOptional<IFluidHandler> fluidInput = LazyOptional.of(WaterInput::new);

    public SmithsTroughBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMITHS_TROUGH.get(), pos, state);
    }

    public int water() {
        return water;
    }

    public int capacity() {
        return FluidType.BUCKET_VOLUME * ServerConfig.get(ServerConfig.TROUGH_CAPACITY_BUCKETS);
    }

    public int waterPerQuench() {
        return Math.max(1, FluidType.BUCKET_VOLUME / ServerConfig.get(ServerConfig.TROUGH_QUENCHES_PER_BUCKET));
    }

    public int quenchesLeft() {
        return water / waterPerQuench();
    }

    public float fillFraction() {
        return capacity() <= 0 ? 0F : Math.min(1F, water / (float) capacity());
    }

    public boolean canQuench() {
        return water >= waterPerQuench();
    }

    public boolean canAcceptBucket() {
        return water < capacity();
    }

    public void addBucket() {
        setWater(Math.min(capacity(), water + FluidType.BUCKET_VOLUME));
    }

    public boolean takeBucket() {
        if (water < FluidType.BUCKET_VOLUME) return false;
        setWater(water - FluidType.BUCKET_VOLUME);
        return true;
    }

    public boolean consumeQuench() {
        if (!canQuench()) return false;
        setWater(water - waterPerQuench());
        return true;
    }

    private void setWater(int amount) {
        water = Math.max(0, amount);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Water", water);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        water = Math.max(0, tag.getInt("Water"));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("Water", water);
        tag.putInt("Capacity", capacity());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
        clientCapacity = tag.getInt("Capacity");
    }

    private int clientCapacity;

    /** Fill fraction for rendering; uses the capacity sent by the server. */
    public float clientFillFraction() {
        int cap = clientCapacity > 0 ? clientCapacity : capacity();
        return cap <= 0 ? 0F : Math.min(1F, water / (float) cap);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) handleUpdateTag(tag);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER && ServerConfig.get(ServerConfig.ENABLE_AUTOMATION)
                && ServerConfig.get(ServerConfig.TROUGH_FLUID_INPUT)) {
            return fluidInput.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluidInput.invalidate();
    }

    private final class WaterInput implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return water > 0 ? new FluidStack(Fluids.WATER, water) : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return capacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return stack.getFluid().isSame(Fluids.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !isFluidValid(0, resource)) return 0;
            int filled = Math.min(capacity() - water, resource.getAmount());
            if (filled > 0 && action.execute()) setWater(water + filled);
            return Math.max(0, filled);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }
}
