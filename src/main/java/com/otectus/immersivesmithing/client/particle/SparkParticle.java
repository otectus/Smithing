package com.otectus.immersivesmithing.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import org.jetbrains.annotations.Nullable;

/** A short-lived, full-bright metal spark that falls and fades from yellow to deep orange. */
public class SparkParticle extends TextureSheetParticle {
    private final SpriteSet sprites;

    protected SparkParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, vx, vy, vz);
        this.sprites = sprites;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.gravity = 0.9F;
        this.friction = 0.94F;
        this.lifetime = 8 + random.nextInt(10);
        this.quadSize = 0.035F + random.nextFloat() * 0.03F;
        this.hasPhysics = true;
        this.rCol = 1.0F;
        this.gCol = 0.85F;
        this.bCol = 0.45F;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float life = age / (float) lifetime;
        gCol = 0.85F - 0.5F * life;
        bCol = 0.45F - 0.4F * life;
        alpha = 1.0F - life * life;
        setSpriteFromAge(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Nullable
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            return new SparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
