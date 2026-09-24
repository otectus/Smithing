package com.otectus.immersivesmithing.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

/** Warms a shaped item's existing render passes without assuming a vanilla model or replacing its textures. */
final class HeatedBufferSource implements MultiBufferSource {
    private final MultiBufferSource source;
    HeatedBufferSource(MultiBufferSource source) { this.source = source; }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return new WarmVertex(source.getBuffer(type));
    }

    private record WarmVertex(VertexConsumer delegate) implements VertexConsumer {
        @Override public VertexConsumer vertex(double x, double y, double z) { delegate.vertex(x,y,z); return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { delegate.color(r, Math.round(g * 0.62F), Math.round(b * 0.28F), a); return this; }
        @Override public VertexConsumer uv(float u, float v) { delegate.uv(u,v); return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { delegate.overlayCoords(u,v); return this; }
        @Override public VertexConsumer uv2(int u, int v) { delegate.uv2(u,v); return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { delegate.normal(x,y,z); return this; }
        @Override public void endVertex() { delegate.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { delegate.defaultColor(r,Math.round(g*0.62F),Math.round(b*0.28F),a); }
        @Override public void unsetDefaultColor() { delegate.unsetDefaultColor(); }
    }
}
