package com.otectus.immersivesmithing.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.immersivesmithing.registry.ModTags;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** A lifted hammer, decisive downward stroke, then recovery; entirely cosmetic and driven by vanilla swing sync. */
public final class HammerClientExtensions implements IClientItemExtensions {
    public static final HammerClientExtensions INSTANCE = new HammerClientExtensions();

    private static float smooth(float t) {
        t = Mth.clamp(t, 0F, 1F);
        return t * t * (3F - 2F * t);
    }

    private static float lift(float t) {
        if (t < 0.3F) return smooth(t / 0.3F);
        if (t < 0.6F) return 1F - 1.2F * smooth((t - 0.3F) / 0.3F);
        return -0.2F * (1F - smooth((t - 0.6F) / 0.4F));
    }

    @Override
    public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack,
                                           float partialTick, float equipProcess, float swingProcess) {
        float side = arm == HumanoidArm.RIGHT ? 1F : -1F;
        float lift = lift(swingProcess);
        float reach = Mth.sin(swingProcess * Mth.PI);
        pose.translate(side * 0.56F, -0.52F - equipProcess * 0.6F + lift * 0.13F, -0.72F - reach * 0.12F);
        pose.mulPose(Axis.XP.rotationDegrees(lift * -65F));
        pose.mulPose(Axis.ZP.rotationDegrees(side * lift * -8F));
        return true;
    }

    public static boolean isHammering(LivingEntity entity, float attackTime) {
        return attackTime > 0F && entity.swingingArm != null && entity.getItemInHand(entity.swingingArm).is(ModTags.HAMMERS);
    }

    public static void applyStrike(HumanoidModel<?> model, LivingEntity entity, float attackTime) {
        HumanoidArm arm = entity.swingingArm == InteractionHand.MAIN_HAND ? entity.getMainArm() : entity.getMainArm().getOpposite();
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        float reach = Mth.sin(attackTime * Mth.PI);
        part.xRot = Mth.lerp(reach, part.xRot, -0.85F) - lift(attackTime) * 1.4F;
        part.zRot = (arm == HumanoidArm.RIGHT ? -1F : 1F) * reach * 0.08F;
        part.yRot *= 1F - reach;
    }

    private HammerClientExtensions() {}
}
