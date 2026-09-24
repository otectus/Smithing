package com.otectus.immersivesmithing.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws flat horizontal surfaces (lava, water, molten metal) from block-atlas sprites. */
public final class RenderUtil {

    /** Packed light is two independent channels, never a numerically ordered brightness. */
    public static int atLeastBlockLight(int light, int minimum) {
        return LightTexture.pack(Math.max(LightTexture.block(light), minimum), LightTexture.sky(light));
    }

    public static int mixColor(int a, int b, float amount) {
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int from = (a >> shift) & 255;
            result |= Math.round(from + (((b >> shift) & 255) - from) * amount) << shift;
        }
        return result;
    }

    public static TextureAtlasSprite sprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
    }

    /** An upward-facing quad at height {@code y}, spanning x0..x1 and z0..z1 in block units. */
    public static void horizontalQuad(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite,
                                      float x0, float z0, float x1, float z1, float y, int argb, int light) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        float u0 = sprite.getU(x0 * 16);
        float u1 = sprite.getU(x1 * 16);
        float v0 = sprite.getV(z0 * 16);
        float v1 = sprite.getV(z1 * 16);
        buffer.vertex(m, x0, y, z0).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        buffer.vertex(m, x0, y, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        buffer.vertex(m, x1, y, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
        buffer.vertex(m, x1, y, z0).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0).endVertex();
    }

    private RenderUtil() {}
}
