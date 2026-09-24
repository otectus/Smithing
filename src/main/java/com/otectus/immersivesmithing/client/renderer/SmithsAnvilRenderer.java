package com.otectus.immersivesmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.immersivesmithing.block.SmithsAnvilBlock;
import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;

/** Unshaped stock has physical volume; a shaped piece reveals its actual (possibly modded) item silhouette. */
public class SmithsAnvilRenderer implements BlockEntityRenderer<SmithsAnvilBlockEntity> {
    private final ItemRenderer itemRenderer;

    public SmithsAnvilRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public int getViewDistance() { return 48; }

    @Override
    public void render(SmithsAnvilBlockEntity anvil, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        WorkpieceData workpiece = anvil.workpiece();
        if (workpiece == null) return;
        BlockState state = anvil.getBlockState();
        Direction facing = state.getBlock() instanceof SmithsAnvilBlock ? state.getValue(SmithsAnvilBlock.FACING) : Direction.NORTH;
        pose.pushPose();
        pose.translate(0.5, 1.005, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-((facing.get2DDataValue() + 2) % 4) * 90F));
        pose.translate(0, 0, 0.05);
        var itemModel = workpiece.isShaped() ? itemRenderer.getModel(workpiece.target(), anvil.getLevel(), null, 0) : null;
        // Exotic equipment may own an unbounded custom renderer; use a safe stock silhouette for it.
        boolean stock = itemModel == null || itemModel.isCustomRenderer()
                || itemModel == Minecraft.getInstance().getModelManager().getMissingModel();
        if (stock) {
            String pattern = workpiece.anvilPattern().getPath();
            ResourceLocation model = switch (pattern) {
                case "sword", "pickaxe", "axe", "shovel", "hoe" -> StationModels.HOT_BLADE;
                case "chestplate", "leggings", "helmet", "boots", "shield" -> StationModels.HOT_PLATE;
                default -> StationModels.HOT_BILLET;
            };
            pose.scale(0.65F, 0.75F, 0.8F);
            pose.translate(-0.5, 0, -0.5);
            StationModels.render(model, pose, buffers, LightTexture.FULL_BRIGHT, 0xFFFFFF);
        } else {
            pose.translate(0, 0.025, 0);
            pose.mulPose(Axis.XP.rotationDegrees(90F));
            pose.mulPose(Axis.ZP.rotationDegrees(45F));
            pose.scale(0.55F, 0.55F, 0.55F);
            itemRenderer.renderStatic(workpiece.target(), ItemDisplayContext.FIXED, LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, new HeatedBufferSource(buffers), anvil.getLevel(), (int) anvil.getBlockPos().asLong());
        }
        pose.popPose();
    }
}
