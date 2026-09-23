package com.otectus.otessmithing.quality;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Per-stack smithing quality, stored in the namespaced {@code otes_smithing} compound so other mods' NBT is
 * left alone. Stacks without it behave exactly like ordinary items (Standard baseline).
 */
public record QualityData(int forgeScore, int anvilScore, boolean faulty) {
    public static final String ROOT = "otes_smithing";
    public static final int VERSION = 1;
    public static final String CARRY = "DurabilityCarry";

    public QualityData {
        forgeScore = Mth.clamp(forgeScore, 0, 100);
        anvilScore = Mth.clamp(anvilScore, 0, 100);
    }

    public SmithingQuality quality() {
        return SmithingQuality.overall(forgeScore, anvilScore, faulty);
    }

    public int overallScore() {
        return SmithingQuality.overallScore(forgeScore, anvilScore);
    }

    public SmithingQuality durabilityQuality() {
        return faulty ? SmithingQuality.FAULTY : SmithingQuality.fromScore(forgeScore);
    }

    public SmithingQuality efficacyQuality() {
        return faulty ? SmithingQuality.FAULTY : SmithingQuality.fromScore(anvilScore);
    }

    public QualityData withForgeScore(int score) {
        return new QualityData(score, anvilScore, faulty);
    }

    public QualityData withAnvilScore(int score) {
        return new QualityData(forgeScore, score, faulty);
    }

    public static boolean has(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(ROOT, Tag.TAG_COMPOUND) && tag.getCompound(ROOT).getBoolean("Forged");
    }

    public static Optional<QualityData> get(ItemStack stack) {
        if (!has(stack)) return Optional.empty();
        CompoundTag root = stack.getTag().getCompound(ROOT);
        return Optional.of(new QualityData(root.getInt("ForgeScore"), root.getInt("AnvilScore"), root.getBoolean("Faulty")));
    }

    /** Writes this quality onto the stack, keeping any durability carry already accumulated. */
    public void apply(ItemStack stack) {
        CompoundTag root = stack.getOrCreateTagElement(ROOT);
        root.putInt("Version", VERSION);
        root.putBoolean("Forged", true);
        root.putInt("ForgeScore", forgeScore);
        root.putInt("AnvilScore", anvilScore);
        root.putBoolean("Faulty", faulty);
        root.putString("Quality", quality().id());
    }

    /** Removes quality (used when equipment is melted down). */
    public static void clear(ItemStack stack) {
        stack.removeTagKey(ROOT);
    }
}
