package com.otectus.otessmithing.client;

import com.otectus.otessmithing.config.ClientConfig;
import com.otectus.otessmithing.registry.ModParticles;
import com.otectus.otessmithing.util.StationEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

/** Spawns station effects locally, scaled by the client's particle setting. */
public final class ClientEffects {

    public static void spawn(BlockPos pos, int type, float strength) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        RandomSource r = level.random;
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        switch (type) {
            case StationEffects.SPARKS -> {
                int n = ClientConfig.scaleParticles(4 + Math.round(strength * 12));
                for (int i = 0; i < n; i++) {
                    level.addParticle(ModParticles.SPARK.get(), x + (r.nextDouble() - 0.5) * 0.3, y + 1.05, z + (r.nextDouble() - 0.5) * 0.3,
                            (r.nextDouble() - 0.5) * 0.25, 0.1 + r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.25);
                }
            }
            case StationEffects.MISS -> {
                int n = ClientConfig.scaleParticles(2);
                for (int i = 0; i < n; i++) {
                    level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.3, y + 1.05, z + (r.nextDouble() - 0.5) * 0.3, 0, 0.02, 0);
                }
            }
            case StationEffects.STEAM -> {
                int n = ClientConfig.scaleParticles(24);
                for (int i = 0; i < n; i++) {
                    level.addParticle(ParticleTypes.CLOUD, x + (r.nextDouble() - 0.5) * 0.7, y + 0.6, z + (r.nextDouble() - 0.5) * 0.7,
                            (r.nextDouble() - 0.5) * 0.04, 0.08 + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.04);
                }
                int bubbles = ClientConfig.scaleParticles(10);
                for (int i = 0; i < bubbles; i++) {
                    level.addParticle(ParticleTypes.BUBBLE_POP, x + (r.nextDouble() - 0.5) * 0.6, y + 0.5, z + (r.nextDouble() - 0.5) * 0.6, 0, 0.02, 0);
                }
            }
            case StationEffects.FORGE_WORK -> {
                int n = ClientConfig.scaleParticles(3 + Math.round(strength * 5));
                for (int i = 0; i < n; i++) {
                    level.addParticle(ModParticles.SPARK.get(), x + (r.nextDouble() - 0.5) * 0.5, y + 0.9, z + (r.nextDouble() - 0.5) * 0.5,
                            (r.nextDouble() - 0.5) * 0.15, 0.12 + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.15);
                }
                if (ClientConfig.particleAmount() != ClientConfig.ParticleAmount.MINIMAL) {
                    level.addParticle(ParticleTypes.LAVA, x, y + 0.85, z, 0, 0, 0);
                }
            }
            case StationEffects.GRIND -> {
                int n = ClientConfig.scaleParticles(12);
                for (int i = 0; i < n; i++) {
                    level.addParticle(ModParticles.SPARK.get(), x, y + 0.8, z,
                            (r.nextDouble() - 0.5) * 0.35, 0.05 + r.nextDouble() * 0.15, (r.nextDouble() - 0.5) * 0.35);
                }
            }
            case StationEffects.IGNITE -> {
                int n = ClientConfig.scaleParticles(10);
                for (int i = 0; i < n; i++) {
                    level.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.6, y + 0.25, z + (r.nextDouble() - 0.5) * 0.6,
                            0, 0.03 + r.nextDouble() * 0.03, 0);
                }
            }
            default -> {
            }
        }
    }

    private ClientEffects() {}
}
