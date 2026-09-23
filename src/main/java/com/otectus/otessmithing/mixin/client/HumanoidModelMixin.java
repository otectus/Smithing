package com.otectus.otessmithing.mixin.client;

import com.otectus.otessmithing.client.TongsClientExtensions;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    /** Replaces the third-person swing with a stab while the swinging hand holds tongs. Cosmetic: never required. */
    @Inject(method = "setupAttackAnimation", at = @At("HEAD"), cancellable = true, require = 0, expect = 1)
    private void otes_smithing$tongsStab(LivingEntity entity, float ageInTicks, CallbackInfo ci) {
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        if (TongsClientExtensions.isStabbing(entity, model.attackTime)) {
            TongsClientExtensions.applyStab(model, entity, model.attackTime);
            ci.cancel();
        }
    }
}
