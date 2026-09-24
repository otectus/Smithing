package com.otectus.immersivesmithing.blockentity;

import com.otectus.immersivesmithing.block.SmithsForgeBlock;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.material.MaterialFamily;
import com.otectus.immersivesmithing.material.MaterialResolver;
import com.otectus.immersivesmithing.minigame.SessionManager;
import com.otectus.immersivesmithing.recipe.SmithingData;
import com.otectus.immersivesmithing.registry.ModBlockEntities;
import com.otectus.immersivesmithing.registry.ModSounds;
import com.otectus.immersivesmithing.registry.ModTags;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * State of a Smith's Forge: the active material family, unmelted deposits (kept as the original stacks so
 * they can be taken back before melting), molten units, fuel or lava, melt progress, and the auxiliary
 * ingredients held in escrow during a forging session.
 */
public class SmithsForgeBlockEntity extends BlockEntity {
    public static final ItemStack LAVA_HEAT_SOURCE = new ItemStack(Items.LAVA_BUCKET);

    /** Metal put into the forge but not yet melted. */
    public record Deposit(ItemStack stack, int unitsEach) {
        int units() {
            return stack.getCount() * unitsEach;
        }
    }

    public enum Result {
        OK, LOCKED, NOT_METAL, WRONG_FAMILY, FULL, NOT_FUEL, DIFFERENT_FUEL, LAVA_PRESENT, SOLID_FUEL_PRESENT,
        NOTHING_TO_MELT, NO_FUEL, WRONG_FUEL, ALREADY_LIT, LAVA_NEEDS_NO_IGNITION, UNKNOWN_FAMILY;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    @Nullable
    private ResourceLocation family;
    private int familyTint = MaterialFamily.DEFAULT_TINT;
    private final List<Deposit> deposits = new ArrayList<>();
    private int moltenUnits;
    private int meltTicks;
    private ItemStack fuel = ItemStack.EMPTY;
    private ItemStack burningFuel = ItemStack.EMPTY;
    private int burnTicks;
    private int burnTicksTotal;
    private int lavaTicks;
    private boolean ignited;
    private final List<ItemStack> escrow = new ArrayList<>();
    private boolean refundEscrowOnTick;

    // Transient: sessions never survive a restart or chunk unload.
    @Nullable
    private UUID sessionOwner;

    private final LazyOptional<IItemHandler> metalInput = LazyOptional.of(() -> new InputHandler(true));
    private final LazyOptional<IItemHandler> fuelInput = LazyOptional.of(() -> new InputHandler(false));

    public SmithsForgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SMITHS_FORGE.get(), pos, state);
    }

    /** The visible workpiece/pile can rise above the block; retain normal frustum culling. */
    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition).expandTowards(0, 0.4, 0);
    }

    // ---------------------------------------------------------------- queries

    @Nullable
    public ResourceLocation family() {
        return family;
    }

    public Optional<MaterialFamily> familyData() {
        return SmithingData.current().materials().family(family);
    }

    public int familyTint() {
        return familyTint;
    }

    public List<Deposit> deposits() {
        return deposits;
    }

    public int depositUnits() {
        int sum = 0;
        for (Deposit d : deposits) sum += d.units();
        return sum;
    }

    public int moltenUnits() {
        return moltenUnits;
    }

    public int totalUnits() {
        return moltenUnits + depositUnits();
    }

    public int capacity() {
        return ServerConfig.get(ServerConfig.FORGE_CAPACITY_UNITS);
    }

    public boolean isReady() {
        return moltenUnits > 0 && deposits.isEmpty();
    }

    public boolean isEmpty() {
        return moltenUnits == 0 && deposits.isEmpty();
    }

    public ItemStack fuel() {
        return fuel;
    }

    public ItemStack burningFuel() {
        return burningFuel;
    }

    public int lavaTicks() {
        return lavaTicks;
    }

    public boolean hasLava() {
        return lavaTicks > 0;
    }

    public boolean isBurning() {
        return burnTicks > 0 || (ignited && !fuel.isEmpty());
    }

    public float meltProgress() {
        int required = requiredMeltTicks();
        return required <= 0 ? 0F : Math.min(1F, meltTicks / (float) required);
    }

    public boolean isLocked() {
        return sessionOwner != null;
    }

    public boolean isLockedFor(UUID player) {
        return sessionOwner != null && !sessionOwner.equals(player);
    }

    public int requiredMeltTicks() {
        float multiplier = familyData().map(MaterialFamily::meltMultiplier).orElse(1.0F);
        return Math.max(20, Math.round(ServerConfig.get(ServerConfig.BASE_MELT_TIME_TICKS) * multiplier));
    }

    // ---------------------------------------------------------------- metal

    /** Adds up to {@code max} items of {@code stack} as metal. Returns how many were (or would be) accepted. */
    public int insertMetal(ItemStack stack, int max, boolean simulate, Result[] error) {
        if (isLocked()) return fail(error, Result.LOCKED);
        Optional<MaterialResolver.Resolved> resolved = MaterialResolver.resolve(stack);
        if (resolved.isEmpty()) return fail(error, Result.NOT_METAL);
        MaterialResolver.Resolved r = resolved.get();
        if (family != null && !family.equals(r.family().id())) return fail(error, Result.WRONG_FAMILY);
        int space = capacity() - totalUnits();
        int fit = Math.min(Math.min(max, stack.getCount()), space / r.unitsEach());
        if (fit <= 0) return fail(error, Result.FULL);
        if (!simulate) {
            ItemStack added = stack.copyWithCount(fit);
            Deposit last = deposits.isEmpty() ? null : deposits.get(deposits.size() - 1);
            if (last != null && last.unitsEach() == r.unitsEach() && ItemStack.isSameItemSameTags(last.stack(), added)
                    && last.stack().getCount() + fit <= last.stack().getMaxStackSize()) {
                last.stack().grow(fit);
            } else {
                deposits.add(new Deposit(added, r.unitsEach()));
            }
            if (family == null) meltTicks = 0;
            family = r.family().id();
            familyTint = r.family().tint();
            changed(true);
        }
        return fit;
    }

    /** Takes back the most recent unmelted deposit. Only possible while the forge is not heating. */
    public ItemStack extractLastDeposit() {
        if (deposits.isEmpty()) return ItemStack.EMPTY;
        Deposit d = deposits.remove(deposits.size() - 1);
        if (deposits.isEmpty()) meltTicks = 0;
        if (isEmpty()) family = null;
        changed(true);
        return d.stack();
    }

    public boolean canExtractDeposits() {
        return !deposits.isEmpty() && !ignited && !hasUsableLava();
    }

    /** Melts a hot workpiece straight back into molten units (auxiliary ingredients are not recovered). */
    public Result acceptWorkpiece(WorkpieceData workpiece) {
        if (isLocked()) return Result.LOCKED;
        Optional<MaterialFamily> wpFamily = SmithingData.current().materials().family(workpiece.family());
        if (wpFamily.isEmpty()) return Result.UNKNOWN_FAMILY;
        if (family != null && !family.equals(workpiece.family())) return Result.WRONG_FAMILY;
        if (totalUnits() + workpiece.metalUnits() > capacity()) return Result.FULL;
        moltenUnits += workpiece.metalUnits();
        family = wpFamily.get().id();
        familyTint = wpFamily.get().tint();
        changed(true);
        return Result.OK;
    }

    // ---------------------------------------------------------------- fuel

    public static boolean isFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.LAVA_BUCKET)) return true;
        if (stack.hasCraftingRemainingItem()) return false;
        if (ForgeHooks.getBurnTime(stack, RecipeType.SMELTING) <= 0) return false;
        return stack.is(ModTags.FORGE_FUELS) || ServerConfig.get(ServerConfig.ACCEPT_ANY_FURNACE_FUEL);
    }

    public int insertFuel(ItemStack stack, int max, boolean simulate, Result[] error) {
        if (isLocked()) return fail(error, Result.LOCKED);
        if (!isFuel(stack) || stack.is(Items.LAVA_BUCKET)) return fail(error, Result.NOT_FUEL);
        if (lavaTicks > 0) return fail(error, Result.LAVA_PRESENT);
        if (!fuel.isEmpty() && !ItemStack.isSameItemSameTags(fuel, stack)) return fail(error, Result.DIFFERENT_FUEL);
        int fit = Math.min(Math.min(max, stack.getCount()), stack.getMaxStackSize() - fuel.getCount());
        if (fit <= 0) return fail(error, Result.FULL);
        if (!simulate) {
            if (fuel.isEmpty()) fuel = stack.copyWithCount(fit);
            else fuel.grow(fit);
            changed(true);
        }
        return fit;
    }

    public Result addLavaBucket(boolean simulate) {
        if (isLocked()) return Result.LOCKED;
        if (!fuel.isEmpty() || burnTicks > 0) return Result.SOLID_FUEL_PRESENT;
        int perBucket = ServerConfig.get(ServerConfig.LAVA_BUCKET_HEAT_TICKS);
        if (lavaTicks + perBucket > perBucket * ServerConfig.get(ServerConfig.MAX_LAVA_BUCKETS)) return Result.FULL;
        if (!simulate) {
            lavaTicks += perBucket;
            changed(true);
        }
        return Result.OK;
    }

    /** Scoops a full bucket of lava back out, if one bucket's worth remains. */
    public boolean takeLavaBucket() {
        int perBucket = ServerConfig.get(ServerConfig.LAVA_BUCKET_HEAT_TICKS);
        if (isLocked() || lavaTicks < perBucket) return false;
        lavaTicks -= perBucket;
        changed(true);
        return true;
    }

    public ItemStack extractFuel() {
        if (fuel.isEmpty() || ignited) return ItemStack.EMPTY;
        ItemStack out = fuel;
        fuel = ItemStack.EMPTY;
        changed(true);
        return out;
    }

    public Result ignite() {
        if (isLocked()) return Result.LOCKED;
        if (deposits.isEmpty()) return Result.NOTHING_TO_MELT;
        if (lavaTicks > 0) return Result.LAVA_NEEDS_NO_IGNITION;
        if (fuel.isEmpty() && burnTicks <= 0) return Result.NO_FUEL;
        ItemStack source = !fuel.isEmpty() ? fuel : burningFuel;
        if (!fuelAllowed(requiredFuelTag(), source)) return Result.WRONG_FUEL;
        if (ignited && burnTicks > 0) return Result.ALREADY_LIT;
        ignited = true;
        changed(true);
        if (level != null) {
            level.playSound(null, worldPosition, ModSounds.FORGE_IGNITE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return Result.OK;
    }

    @Nullable
    public TagKey<Item> requiredFuelTag() {
        return familyData().map(MaterialFamily::requiredFuelTag).orElse(null);
    }

    private boolean hasUsableLava() {
        return lavaTicks > 0 && fuelAllowed(requiredFuelTag(), LAVA_HEAT_SOURCE);
    }

    private static boolean fuelAllowed(@Nullable TagKey<Item> required, ItemStack source) {
        return required == null || (!source.isEmpty() && source.is(required));
    }

    // ---------------------------------------------------------------- ticking

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, SmithsForgeBlockEntity be) {
        be.tick();
    }

    private void tick() {
        if (level == null) return;
        if (refundEscrowOnTick) {
            refundEscrowOnTick = false;
            for (ItemStack stack : escrow) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5, stack);
            }
            escrow.clear();
            setChanged();
        }
        boolean sync = false;
        if (!deposits.isEmpty()) {
            HeatResult heat = consumeHeat();
            sync |= heat.changed;
            if (heat.heating) {
                meltTicks++;
                if (meltTicks >= requiredMeltTicks()) {
                    completeMelt();
                    sync = true;
                } else if (meltTicks % 20 == 0) {
                    setChanged();
                }
            }
        } else if (burnTicks > 0) {
            // Nothing left to melt: the current fuel item burns down and the fire goes out.
            burnTicks--;
            if (burnTicks == 0) {
                burningFuel = ItemStack.EMPTY;
                ignited = false;
                sync = true;
            }
        } else if (ignited) {
            ignited = false;
            sync = true;
        }
        if (sync) changed(true);
        updateBlockState();
    }

    private record HeatResult(boolean heating, boolean changed) {}

    private HeatResult consumeHeat() {
        MaterialFamily fam = familyData().orElse(null);
        TagKey<Item> required = fam != null ? fam.requiredFuelTag() : null;
        if (lavaTicks > 0 && fuelAllowed(required, LAVA_HEAT_SOURCE)) {
            lavaTicks--;
            return new HeatResult(true, lavaTicks == 0);
        }
        boolean needsIgnition = fam == null || fam.requiresIgnition();
        if (!ignited) {
            if (needsIgnition || fuel.isEmpty() || !fuelAllowed(required, fuel)) return new HeatResult(false, false);
            ignited = true;
        }
        boolean changed = false;
        if (burnTicks <= 0) {
            if (!fuel.isEmpty() && fuelAllowed(required, fuel)) {
                double multiplier = ServerConfig.get(ServerConfig.FUEL_CONSUMPTION_MULTIPLIER);
                int ticks = (int) Math.max(1, ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING) / multiplier);
                burningFuel = fuel.copyWithCount(1);
                fuel.shrink(1);
                if (fuel.isEmpty()) fuel = ItemStack.EMPTY;
                burnTicks = burnTicksTotal = ticks;
                changed = true;
            } else {
                ignited = false;
                burningFuel = ItemStack.EMPTY;
                return new HeatResult(false, true);
            }
        }
        burnTicks--;
        return new HeatResult(fuelAllowed(required, burningFuel), changed);
    }

    private void completeMelt() {
        moltenUnits += depositUnits();
        deposits.clear();
        meltTicks = 0;
        if (level != null) {
            level.playSound(null, worldPosition, ModSounds.FORGE_READY.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private void updateBlockState() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof SmithsForgeBlock)) return;
        boolean lit = (ignited && burnTicks > 0) || lavaTicks > 0;
        boolean ready = isReady();
        if (state.getValue(SmithsForgeBlock.LIT) != lit || state.getValue(SmithsForgeBlock.READY) != ready) {
            level.setBlock(worldPosition, state.setValue(SmithsForgeBlock.LIT, lit).setValue(SmithsForgeBlock.READY, ready), Block.UPDATE_ALL);
        }
    }

    // ---------------------------------------------------------------- sessions

    public void beginSession(UUID player, List<ItemStack> escrowed) {
        sessionOwner = player;
        escrow.clear();
        escrow.addAll(escrowed);
        setChanged();
    }

    public void lockForSelection(UUID player) {
        sessionOwner = player;
    }

    /** Consumes the reserved metal and the escrowed auxiliary ingredients. */
    public void completeSession(int units) {
        moltenUnits = Math.max(0, moltenUnits - units);
        escrow.clear();
        sessionOwner = null;
        if (isEmpty()) family = null;
        changed(true);
        updateBlockState();
    }

    /** Hands the escrow back to the caller for refunding and clears it, so it can only be refunded once. */
    public List<ItemStack> takeEscrow() {
        List<ItemStack> out = new ArrayList<>(escrow);
        escrow.clear();
        setChanged();
        return out;
    }

    public void endSession() {
        sessionOwner = null;
    }

    @Nullable
    public UUID sessionOwner() {
        return sessionOwner;
    }

    // ---------------------------------------------------------------- removal

    /** Drops what can be recovered: unmelted deposits, solid fuel and any escrow. Molten metal is lost. */
    public void dropContents() {
        if (level == null) return;
        List<ItemStack> drops = new ArrayList<>();
        deposits.forEach(d -> drops.add(d.stack()));
        if (!fuel.isEmpty()) drops.add(fuel);
        drops.addAll(escrow);
        deposits.clear();
        fuel = ItemStack.EMPTY;
        escrow.clear();
        for (ItemStack stack : drops) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) SessionManager.onStationUnloaded(level, worldPosition);
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeState(tag, true);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        family = tag.contains("Family") ? ResourceLocation.tryParse(tag.getString("Family")) : null;
        familyTint = tag.contains("Tint") ? tag.getInt("Tint") : MaterialFamily.DEFAULT_TINT;
        deposits.clear();
        ListTag list = tag.getList("Deposits", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag d = list.getCompound(i);
            ItemStack stack = ItemStack.of(d.getCompound("Stack"));
            int units = d.getInt("Units");
            if (!stack.isEmpty() && units > 0) deposits.add(new Deposit(stack, units));
        }
        moltenUnits = Math.max(0, tag.getInt("Molten"));
        meltTicks = tag.getInt("MeltTicks");
        fuel = ItemStack.of(tag.getCompound("Fuel"));
        burningFuel = ItemStack.of(tag.getCompound("Burning"));
        burnTicks = tag.getInt("BurnTicks");
        burnTicksTotal = tag.getInt("BurnTotal");
        lavaTicks = tag.getInt("Lava");
        ignited = tag.getBoolean("Ignited");
        escrow.clear();
        ListTag esc = tag.getList("Escrow", Tag.TAG_COMPOUND);
        for (int i = 0; i < esc.size(); i++) {
            ItemStack stack = ItemStack.of(esc.getCompound(i));
            if (!stack.isEmpty()) escrow.add(stack);
        }
        // A session can never survive a reload, so anything still in escrow belongs back in the world.
        refundEscrowOnTick = !escrow.isEmpty();
    }

    private void writeState(CompoundTag tag, boolean full) {
        if (family != null) tag.putString("Family", family.toString());
        tag.putInt("Tint", familyTint);
        ListTag list = new ListTag();
        for (Deposit d : deposits) {
            CompoundTag c = new CompoundTag();
            c.put("Stack", d.stack().save(new CompoundTag()));
            c.putInt("Units", d.unitsEach());
            list.add(c);
        }
        tag.put("Deposits", list);
        tag.putInt("Molten", moltenUnits);
        tag.putInt("MeltTicks", meltTicks);
        tag.put("Fuel", fuel.save(new CompoundTag()));
        tag.put("Burning", burningFuel.save(new CompoundTag()));
        tag.putInt("BurnTicks", burnTicks);
        tag.putInt("BurnTotal", burnTicksTotal);
        tag.putInt("Lava", lavaTicks);
        tag.putBoolean("Ignited", ignited);
        if (full) {
            ListTag esc = new ListTag();
            for (ItemStack stack : escrow) esc.add(stack.save(new CompoundTag()));
            tag.put("Escrow", esc);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeState(tag, false);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void changed(boolean sync) {
        setChanged();
        if (sync && level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static int fail(Result[] error, Result result) {
        if (error != null && error.length > 0) error[0] = result;
        return 0;
    }

    // ---------------------------------------------------------------- automation

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null && ServerConfig.get(ServerConfig.ENABLE_AUTOMATION)) {
            String face = relativeFace(side);
            if (ServerConfig.list(ServerConfig.FORGE_METAL_FACES).contains(face)) return metalInput.cast();
            if (ServerConfig.list(ServerConfig.FORGE_FUEL_FACES).contains(face)) return fuelInput.cast();
        }
        return super.getCapability(cap, side);
    }

    /** The face name as seen on a north-facing forge, so config is independent of placement. */
    private String relativeFace(Direction side) {
        if (side.getAxis().isVertical()) return side.getName();
        Direction facing = getBlockState().hasProperty(SmithsForgeBlock.FACING) ? getBlockState().getValue(SmithsForgeBlock.FACING) : Direction.NORTH;
        int turns = (side.get2DDataValue() - facing.get2DDataValue() + 4) % 4;
        return Direction.from2DDataValue(Direction.NORTH.get2DDataValue() + turns).getName();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        metalInput.invalidate();
        fuelInput.invalidate();
    }

    /** Insert-only handler. Automation can load metal or fuel but never extract or start a session. */
    private final class InputHandler implements IItemHandler {
        private final boolean metal;

        InputHandler(boolean metal) {
            this.metal = metal;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || level == null || level.isClientSide) return stack;
            int accepted;
            if (metal) {
                accepted = insertMetal(stack, stack.getCount(), simulate, null);
            } else {
                if (stack.is(Items.LAVA_BUCKET)) return stack; // buckets would need their container handed back
                accepted = insertFuel(stack, stack.getCount(), simulate, null);
            }
            if (accepted <= 0) return stack;
            return stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return metal ? MaterialResolver.resolve(stack).isPresent() : isFuel(stack) && !stack.is(Items.LAVA_BUCKET);
        }
    }
}
