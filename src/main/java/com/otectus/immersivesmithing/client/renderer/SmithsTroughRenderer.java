package com.otectus.immersivesmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otectus.immersivesmithing.block.SmithsTroughBlock;
import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/** Renders the trough's water surface at a height matching how many quenches remain. */
public class SmithsTroughRenderer implements BlockEntityRenderer<SmithsTroughBlockEntity> {
    private static final ResourceLocation WATER = new ResourceLocation("block/water_still");

    public SmithsTroughRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public int getViewDistance() { return 48; }

    @Override
    public void render(SmithsTroughBlockEntity trough, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float fill = trough.clientFillFraction();
        if (fill <= 0F) return;
        BlockState state = trough.getBlockState();
        boolean alongX = !(state.getBlock() instanceof SmithsTroughBlock) || state.getValue(SmithsTroughBlock.FACING).getAxis() == Direction.Axis.Z;
        int color = trough.getLevel() != null ? BiomeColors.getAverageWaterColor(trough.getLevel(), trough.getBlockPos()) : 0x3F76E4;
        float y = surfaceHeight(fill);
        float x0 = alongX ? 2 / 16F : 4 / 16F;
        float x1 = alongX ? 14 / 16F : 12 / 16F;
        float z0 = alongX ? 4 / 16F : 2 / 16F;
        float z1 = alongX ? 12 / 16F : 14 / 16F;
        RenderUtil.horizontalQuad(pose, buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)),
                RenderUtil.sprite(WATER), x0, z0, x1, z1, y, 0xC0000000 | (color & 0xFFFFFF), light);
    }

    public static float surfaceHeight(float fill) {
        return (3.2F + 5.3F * fill) / 16F;
    }
}
