package com.otectus.otessmithing.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared behaviour of tongs and hammers: tiered, configurable durability, repairable with the tier material. */
public abstract class SmithingToolItem extends Item {
    private final SmithingTier tier;

    protected SmithingToolItem(SmithingTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public SmithingTier tier() {
        return tier;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return Math.max(1, tier.durability());
    }

    @Override
    public int getEnchantmentValue() {
        return tier.enchantability();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return tier.repairIngredient().test(repair) || super.isValidRepairItem(stack, repair);
    }

    /**
     * Damages the tool by one without ever breaking it. Used while the tool still holds a workpiece, so wear
     * can never destroy one.
     */
    public static void wearWithoutBreaking(ItemStack stack) {
        if (!stack.isDamageableItem()) return;
        if (stack.getDamageValue() + 1 < stack.getMaxDamage()) {
            stack.setDamageValue(stack.getDamageValue() + 1);
        }
    }
}
