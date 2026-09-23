package com.otectus.otessmithing.mixin;

import com.otectus.otessmithing.quality.QualityHooks;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    /** Scales incoming durability damage for smith-forged stacks before Unbreaking is applied. */
    @ModifyVariable(method = "hurt(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int otes_smithing$scaleDurabilityDamage(int amount) {
        return QualityHooks.adjustDamage((ItemStack) (Object) this, amount);
    }
}
