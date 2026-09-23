package com.otectus.otessmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.block.SmithsForgeBlock;
import com.otectus.otessmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.otessmithing.config.ServerConfig;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Renders the forge's physical state without entities: a staged pile of deposited metal (at most six item
 * models), the fuel below, lava, and a glowing molten pool once metal has melted.
 */
public class SmithsForgeRenderer implements BlockEntityRenderer<SmithsForgeBlockEntity> {
    private static final ResourceLocation LAVA = new ResourceLocation("block/lava_still");
    private static final ResourceLocation MOLTEN = OtesSmithing.id("block/molten_metal");
    private static final float[][] PILE = {
            {0.50F, 0.50F, 0F}, {0.36F, 0.40F, 40F}, {0.62F, 0.62F, 110F}, {0.40F, 0.64F, 200F}, {0.63F, 0.37F, 290F}, {0.50F, 0.52F, 65F}
    };
    private static final float[][] FUEL = {
            {0.50F, 0.40F, 10F}, {0.34F, 0.52F, 80F}, {0.66F, 0.50F, 150F}, {0.50F, 0.62F, 230F}
    };

    private final ItemRenderer itemRenderer;

    public SmithsForgeRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SmithsForgeBlockEntity forge, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = forge.getBlockState();
        if (!(state.getBlock() instanceof SmithsForgeBlock)) return;
        Direction facing = state.getValue(SmithsForgeBlock.FACING);
        boolean lit = state.getValue(SmithsForgeBlock.LIT);

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-((facing.get2DDataValue() + 2) % 4) * 90F));
        pose.translate(-0.5, 0, -0.5);

        // Fuel below.
        if (forge.hasLava()) {
            RenderUtil.horizontalQuad(pose, buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)),
                    RenderUtil.sprite(LAVA), 2 / 16F, 1 / 16F, 14 / 16F, 14 / 16F, 3.5F / 16F, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
        } else {
            ItemStack fuel = !forge.fuel().isEmpty() ? forge.fuel() : forge.burningFuel();
            int count = fuelStage(forge.fuel().getCount()) + (forge.burningFuel().isEmpty() ? 0 : 1);
            int fuelLight = lit ? LightTexture.FULL_BRIGHT : light;
            for (int i = 0; i < Math.min(count, FUEL.length) && !fuel.isEmpty(); i++) {
                renderFlat(fuel, FUEL[i][0], 2.6F / 16F + i * 0.012F, FUEL[i][1], FUEL[i][2], 0.42F, pose, buffers, fuelLight, forge, i);
            }
        }

        // Molten pool, then any unmelted metal on top of it.
        if (forge.moltenUnits() > 0) {
            float fill = (float) Math.sqrt(Math.min(1.0, forge.moltenUnits() / (double) Math.max(1, ServerConfig.get(ServerConfig.FORGE_CAPACITY_UNITS))));
            float y = (10.4F + 3.2F * fill) / 16F;
            RenderUtil.horizontalQuad(pose, buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS)),
                    RenderUtil.sprite(MOLTEN), 2 / 16F, 2 / 16F, 14 / 16F, 14 / 16F, y, 0xFF000000 | forge.familyTint(), LightTexture.FULL_BRIGHT);
        }
        List<SmithsForgeBlockEntity.Deposit> deposits = forge.deposits();
        if (!deposits.isEmpty()) {
            int count = Math.min(PILE.length, pileStage(forge.depositUnits()));
            float base = forge.moltenUnits() > 0 ? 12.5F / 16F : 10.2F / 16F;
            int pileLight = lit ? Math.max(light, LightTexture.pack(12, 12)) : light;
            for (int i = 0; i < count; i++) {
                ItemStack stack = deposits.get(Math.floorMod(deposits.size() - 1 - i, deposits.size())).stack();
                renderFlat(stack, PILE[i][0], base + i * 0.022F, PILE[i][1], PILE[i][2], 0.36F, pose, buffers, pileLight, forge, i);
            }
        }
        pose.popPose();
    }

    private void renderFlat(ItemStack stack, float x, float y, float z, float yaw, float scale, PoseStack pose,
                            MultiBufferSource buffers, int light, SmithsForgeBlockEntity forge, int seed) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(90F));
        pose.scale(scale, scale, scale);
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, forge.getLevel(),
                (int) forge.getBlockPos().asLong() + seed);
        pose.popPose();
    }

    /** 1 item, then small, medium, large and full piles. */
    private static int pileStage(int units) {
        if (units < 9) return 1;
        if (units < 36) return 2;
        if (units < 144) return 3;
        if (units < 324) return 4;
        return 6;
    }

    private static int fuelStage(int count) {
        if (count <= 0) return 0;
        if (count < 8) return 1;
        if (count < 24) return 2;
        return 3;
    }
}
