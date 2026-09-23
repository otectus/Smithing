package com.otectus.otessmithing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.otessmithing.registry.ModTags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/**
 * Tongs are thrust forward into the forge instead of swung: first person through the item's hand transform,
 * third person through {@link #applyStab} (called from the HumanoidModel mixin).
 */
public final class TongsClientExtensions implements IClientItemExtensions {
    public static final TongsClientExtensions INSTANCE = new TongsClientExtensions();

    private TongsClientExtensions() {}

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        // Vanilla's arm placement (ItemInHandRenderer#applyItemArmTransform); returning true skips its swing arc.
        pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
        float thrust = Mth.sin(swingProcess * Mth.PI);
        pose.translate(side * -0.08F * thrust, -0.1F * thrust, -0.45F * thrust);
        pose.mulPose(Axis.XP.rotationDegrees(-15.0F * thrust));
        return true;
    }

    /** Whether the entity's current swing is a tongs stab. */
    public static boolean isStabbing(LivingEntity entity, float attackTime) {
        InteractionHand hand = entity.swingingArm;
        return attackTime > 0.0F && hand != null && entity.getItemInHand(hand).is(ModTags.TONGS);
    }

    /** Third-person stab: the torso turns the tongs arm forward and the arm drives out and down, then back. */
    public static void applyStab(HumanoidModel<?> model, LivingEntity entity, float attackTime) {
        HumanoidArm arm = entity.swingingArm == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
        float thrust = Mth.sin(attackTime * Mth.PI);
        float side = arm == HumanoidArm.RIGHT ? -1.0F : 1.0F;
        model.body.yRot = side * 0.25F * thrust;
        model.rightArm.z = Mth.sin(model.body.yRot) * 5.0F;
        model.rightArm.x = -Mth.cos(model.body.yRot) * 5.0F;
        model.leftArm.z = -Mth.sin(model.body.yRot) * 5.0F;
        model.leftArm.x = Mth.cos(model.body.yRot) * 5.0F;
        model.rightArm.yRot += model.body.yRot;
        model.leftArm.yRot += model.body.yRot;
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = Mth.lerp(thrust, part.xRot, -1.25F);
        part.yRot += side * 0.2F * thrust;
        part.z -= 2.0F * thrust;
    }
}
