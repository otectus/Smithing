package com.otectus.immersivesmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.otectus.immersivesmithing.ImmersiveSmithing;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Tiny baked meshes shared by all stations. Resolve through ModelManager so resource reloads are immediate. */
public final class StationModels {
    public static final ResourceLocation BILLET = ImmersiveSmithing.id("block/metal_billet");
    public static final ResourceLocation COAL = ImmersiveSmithing.id("block/fuel_chunk");
    public static final ResourceLocation HOT_BILLET = ImmersiveSmithing.id("block/hot_billet");
    public static final ResourceLocation HOT_PLATE = ImmersiveSmithing.id("block/hot_plate");
    public static final ResourceLocation HOT_BLADE = ImmersiveSmithing.id("block/hot_blade");
    public static final List<ResourceLocation> ALL = List.of(BILLET, COAL, HOT_BILLET, HOT_PLATE, HOT_BLADE);

    public static void render(ResourceLocation id, PoseStack pose, MultiBufferSource buffers, int light, int color) {
        Minecraft mc = Minecraft.getInstance();
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),
                buffers.getBuffer(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS)), null,
                mc.getModelManager().getModel(id), ((color >> 16) & 255) / 255F,
                ((color >> 8) & 255) / 255F, (color & 255) / 255F, light, OverlayTexture.NO_OVERLAY);
    }

    private StationModels() {}
}
