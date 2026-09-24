package com.otectus.immersivesmithing.workpiece;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/** Versioned NBT form of {@link WorkpieceData}. Every read is validated; malformed data is treated as absent. */
public final class WorkpieceCodec {
    public static final String ROOT = "immersive_smithing";
    public static final String HELD = "HeldWorkpiece";
    public static final int VERSION = 1;

    public static CompoundTag save(WorkpieceData data) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Version", VERSION);
        tag.put("TargetStack", data.target().save(new CompoundTag()));
        tag.putString("RecipeId", data.recipeId().toString());
        tag.putString("MaterialFamily", data.family().toString());
        tag.putInt("MetalUnits", data.metalUnits());
        tag.putInt("ForgeScore", data.forgeScore());
        tag.putInt("AnvilScore", data.anvilScore());
        tag.putBoolean("Faulty", data.faulty());
        tag.putString("State", data.state().name());
        tag.putString("AnvilPattern", data.anvilPattern().toString());
        return tag;
    }

    public static Optional<WorkpieceData> load(CompoundTag tag) {
        try {
            if (!tag.contains("TargetStack", Tag.TAG_COMPOUND)) return Optional.empty();
            ItemStack target = ItemStack.of(tag.getCompound("TargetStack"));
            if (target.isEmpty()) return Optional.empty();
            WorkpieceState state = WorkpieceState.byName(tag.getString("State"));
            if (state == null) return Optional.empty();
            ResourceLocation recipe = ResourceLocation.tryParse(tag.getString("RecipeId"));
            ResourceLocation family = ResourceLocation.tryParse(tag.getString("MaterialFamily"));
            ResourceLocation pattern = ResourceLocation.tryParse(tag.getString("AnvilPattern"));
            if (recipe == null) recipe = ImmersiveSmithing.id("unknown");
            if (family == null) family = ImmersiveSmithing.id("unknown");
            if (pattern == null || tag.getString("AnvilPattern").isEmpty()) pattern = EquipmentClassifier.classify(target);
            int units = Math.max(0, tag.getInt("MetalUnits"));
            int forge = Mth.clamp(tag.getInt("ForgeScore"), 0, 100);
            int anvil = state == WorkpieceState.SHAPED ? Mth.clamp(tag.getInt("AnvilScore"), 0, 100) : -1;
            return Optional.of(new WorkpieceData(target, recipe, family, units, forge, anvil, tag.getBoolean("Faulty"), state, pattern));
        } catch (RuntimeException e) {
            ImmersiveSmithing.LOGGER.warn("Discarding unreadable workpiece data: {}", e.toString());
            return Optional.empty();
        }
    }

    /** The workpiece carried by tongs or stored in a Hot Workpiece item. */
    public static Optional<WorkpieceData> getHeld(ItemStack holder) {
        CompoundTag tag = holder.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) return Optional.empty();
        CompoundTag root = tag.getCompound(ROOT);
        if (!root.contains(HELD, Tag.TAG_COMPOUND)) return Optional.empty();
        return load(root.getCompound(HELD));
    }

    public static boolean isHolding(ItemStack holder) {
        return getHeld(holder).isPresent();
    }

    public static void setHeld(ItemStack holder, WorkpieceData data) {
        holder.getOrCreateTagElement(ROOT).put(HELD, save(data));
    }

    public static void clearHeld(ItemStack holder) {
        CompoundTag tag = holder.getTag();
        if (tag == null || !tag.contains(ROOT, Tag.TAG_COMPOUND)) return;
        CompoundTag root = tag.getCompound(ROOT);
        root.remove(HELD);
        if (root.isEmpty()) holder.removeTagKey(ROOT);
    }

    private WorkpieceCodec() {}
}
