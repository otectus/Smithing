package com.otectus.otessmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.otessmithing.block.SmithsAnvilBlock;
import com.otectus.otessmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.otessmithing.workpiece.WorkpieceData;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders the workpiece lying on the anvil face, full-bright so it reads as hot metal. The item-based
 * representation works for any modded item; bespoke models can replace it later.
 */
public class SmithsAnvilRenderer implements BlockEntityRenderer<SmithsAnvilBlockEntity> {
    private final ItemRenderer itemRenderer;

    public SmithsAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SmithsAnvilBlockEntity anvil, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        WorkpieceData workpiece = anvil.workpiece();
        if (workpiece == null) return;
        BlockState state = anvil.getBlockState();
        Direction facing = state.getBlock() instanceof SmithsAnvilBlock ? state.getValue(SmithsAnvilBlock.FACING) : Direction.NORTH;
        pose.pushPose();
        pose.translate(0.5, 1.0 + 1 / 32F, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(facing.getAxis() == Direction.Axis.X ? 0F : 90F));
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.mulPose(Axis.ZP.rotationDegrees(45F));
        pose.scale(0.62F, 0.62F, 0.62F);
        int glow = workpiece.isShaped() ? LightTexture.pack(15, 13) : LightTexture.FULL_BRIGHT;
        itemRenderer.renderStatic(workpiece.target(), ItemDisplayContext.FIXED, glow, OverlayTexture.NO_OVERLAY, pose, buffers,
                anvil.getLevel(), (int) anvil.getBlockPos().asLong());
        pose.popPose();
    }
}
