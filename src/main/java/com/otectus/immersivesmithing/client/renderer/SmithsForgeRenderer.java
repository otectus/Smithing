package com.otectus.immersivesmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.block.SmithsForgeBlock;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.config.ServerConfig;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

import java.util.List;

/** Staged, stable physical piles: six metal meshes and four fuel meshes at most. No decorative entities. */
public class SmithsForgeRenderer implements BlockEntityRenderer<SmithsForgeBlockEntity> {
    private static final ResourceLocation LAVA = new ResourceLocation("block/lava_still");
    private static final ResourceLocation MOLTEN = ImmersiveSmithing.id("block/molten_metal");
    private static final float[][] PILE = {
            {0.50F, 0.39F, 0F}, {0.50F, 0.64F, 0F}, {0.39F, 0.50F, 90F},
            {0.64F, 0.50F, 90F}, {0.50F, 0.39F, 0F}, {0.50F, 0.64F, 0F}
    };
    private static final float[][] FUEL = {
            {0.38F, 0.32F, 12F}, {0.64F, 0.34F, -10F}, {0.48F, 0.61F, 20F}, {0.5F, 0.44F, 5F}
    };
    private final ItemRenderer itemRenderer;

    public SmithsForgeRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public int getViewDistance() { return 48; }

    @Override
    public void render(SmithsForgeBlockEntity forge, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = forge.getBlockState();
        if (!(state.getBlock() instanceof SmithsForgeBlock)) return;
        Direction facing = state.getValue(SmithsForgeBlock.FACING);
        boolean lit = state.getValue(SmithsForgeBlock.LIT);
        boolean distant = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceToSqr(
                forge.getBlockPos().getX() + 0.5, forge.getBlockPos().getY() + 0.5, forge.getBlockPos().getZ() + 0.5) > 24 * 24;
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-((facing.get2DDataValue() + 2) % 4) * 90F));
        pose.translate(-0.5, 0, -0.5);

        if (forge.hasLava()) {
            RenderUtil.horizontalQuad(pose, buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),
                    RenderUtil.sprite(LAVA), 3 / 16F, 1 / 16F, 13 / 16F, 13 / 16F, 3 / 16F, 0xFFFFFFFF, LightTexture.FULL_BRIGHT);
        } else {
            ItemStack fuel = !forge.fuel().isEmpty() ? forge.fuel() : forge.burningFuel();
            int count = Math.min(distant ? 1 : FUEL.length, fuelStage(forge.fuel().getCount()) + (forge.burningFuel().isEmpty() ? 0 : 1));
            int fuelLight = lit ? RenderUtil.atLeastBlockLight(light, 10) : light;
            for (int i = 0; i < count && !fuel.isEmpty(); i++) {
                if (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL)) {
                    mesh(StationModels.COAL, FUEL[i][0], (2 + (i == 3 ? 2 : 0)) / 16F, FUEL[i][1], FUEL[i][2],
                            pose, buffers, fuelLight, 0xFFFFFF);
                } else {
                    renderFlat(fuel, FUEL[i][0], 3 / 16F + i * 0.02F, FUEL[i][1], FUEL[i][2], 0.34F, pose, buffers, fuelLight, forge, i);
                }
            }
        }

        float poolY = 10.05F / 16F;
        if (forge.moltenUnits() > 0) {
            float fill = (float) Math.sqrt(Math.min(1.0, forge.moltenUnits() / (double) Math.max(1, ServerConfig.get(ServerConfig.FORGE_CAPACITY_UNITS))));
            poolY = (10.25F + 3.25F * fill) / 16F;
            // Warm emission is always legible, even for dark modded metals. Family tint is restrained in hot metal.
            int tint = RenderUtil.mixColor(0xFFFFFF, forge.familyTint(), 0.18F);
            RenderUtil.horizontalQuad(pose, buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)),
                    RenderUtil.sprite(MOLTEN), 2 / 16F, 2 / 16F, 14 / 16F, 14 / 16F, poolY, 0xFF000000 | tint, LightTexture.FULL_BRIGHT);
        }
        List<SmithsForgeBlockEntity.Deposit> deposits = forge.deposits();
        if (!deposits.isEmpty()) {
            int count = Math.min(distant ? 2 : PILE.length, pileStage(forge.depositUnits()));
            float base = Math.max(10.05F / 16F, poolY);
            float heat = lit ? forge.meltProgress() : 0;
            int pileLight = lit ? RenderUtil.atLeastBlockLight(light, 8 + Math.round(heat * 6)) : light;
            for (int i = 0; i < count; i++) {
                ItemStack stack = deposits.get(Math.floorMod(deposits.size() - 1 - i, deposits.size())).stack();
                // Conventional materials get volumetric billets; arbitrary equipment retains its own item model.
                if (stack.is(Tags.Items.INGOTS) || stack.is(Tags.Items.NUGGETS)) {
                    // Family tint describes molten metal in the datapack, not the colour of cold stock.
                    int tint = RenderUtil.mixColor(coldMetalColor(forge), forge.familyTint(), heat * 0.8F);
                    mesh(StationModels.BILLET, PILE[i][0], base + (i / 2) * 2 / 16F, PILE[i][1], PILE[i][2], pose, buffers, pileLight, tint);
                } else {
                    renderFlat(stack, PILE[i][0], base + 1 / 16F + i * 0.035F, PILE[i][1], PILE[i][2], 0.3F,
                            pose, buffers, pileLight, forge, i);
                }
            }
        }
        pose.popPose();
    }

    private static void mesh(ResourceLocation model, float x, float y, float z, float yaw, PoseStack pose,
                             MultiBufferSource buffers, int light, int color) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-0.5, 0, -0.5);
        StationModels.render(model, pose, buffers, light, color);
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

    private static int coldMetalColor(SmithsForgeBlockEntity forge) {
        if (forge.family() == null) return 0xFFFFFF;
        return switch (forge.family().getPath()) {
            case "gold" -> 0xFFE063;
            case "copper" -> 0xEAA082;
            case "bronze" -> 0xD2A576;
            case "netherite" -> 0x665965;
            case "steel" -> 0xAAB8C1;
            case "invar" -> 0xCDD8C6;
            case "nickel" -> 0xD4CDAA;
            case "lead" -> 0x998FAA;
            case "electrum" -> 0xECDB8A;
            default -> 0xFFFFFF;
        };
    }
}
