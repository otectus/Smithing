package com.otectus.immersivesmithing.client;

import com.otectus.immersivesmithing.blockentity.SmithsTroughBlockEntity;
import com.otectus.immersivesmithing.client.renderer.SmithsTroughRenderer;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.registry.ModParticles;
import com.otectus.immersivesmithing.util.StationEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import java.lang.ref.WeakReference;

/** Action packets drive bounded local effects. Direction comes from the same facing as the baked station. */
public final class ClientEffects {
    private static WeakReference<ClientLevel> budgetLevel = new WeakReference<>(null);
    private static long budgetTick;
    private static int remaining;

    public static void spawn(BlockPos pos, int type, float strength) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || !level.hasChunkAt(pos)
                || mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 48 * 48) return;
        if (budgetLevel.get() != level || budgetTick != level.getGameTime()) {
            budgetLevel = new WeakReference<>(level);
            budgetTick = level.getGameTime();
            remaining = 96;
        }
        strength = Float.isFinite(strength) ? Mth.clamp(strength, 0F, 1F) : 0F;
        boolean reducedFlash = ClientConfig.get(ClientConfig.REDUCED_FLASHES);
        RandomSource random = level.random;
        BlockState state = level.getBlockState(pos);
        Direction facing = state.hasProperty(HorizontalDirectionalBlock.FACING) ? state.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        double fx = facing.getStepX(), fz = facing.getStepZ();
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (type) {
            case StationEffects.SPARKS, StationEffects.GRIND -> {
                boolean grind = type == StationEffects.GRIND;
                int n = count(reducedFlash ? 3 : 5 + Math.round(strength * (grind ? 7 : 9)));
                double ahead = grind ? 0.3 : 0;
                for (int i = 0; i < n; i++) {
                    double spread = (random.nextDouble() - 0.5) * (grind ? 0.10 : 0.24);
                    double speed = (grind ? 0.16 : 0.07) + random.nextDouble() * 0.13;
                    level.addParticle(ModParticles.SPARK.get(), x + fx * ahead, y + (grind ? 0.42 : 1.04), z + fz * ahead,
                            fx * speed - fz * spread, (grind ? 0.035 : 0.08) + random.nextDouble() * 0.12, fz * speed + fx * spread);
                }
            }
            case StationEffects.MISS -> {
                for (int i = 0, n = count(2); i < n; i++) {
                    level.addParticle(ParticleTypes.SMOKE, x, y + 1.04, z, 0, 0.018, 0);
                }
            }
            case StationEffects.STEAM -> {
                float fill = level.getBlockEntity(pos) instanceof SmithsTroughBlockEntity trough ? trough.clientFillFraction() : 1F;
                double surface = y + SmithsTroughRenderer.surfaceHeight(fill);
                for (int i = 0, n = count(12); i < n; i++) {
                    double across = (random.nextDouble() - 0.5) * 0.6;
                    double along = (random.nextDouble() - 0.5) * 0.3;
                    level.addParticle(ParticleTypes.CLOUD, x - fz * across + fx * along, surface, z + fx * across + fz * along,
                            -fz * across * 0.04, 0.035 + random.nextDouble() * 0.05, fx * across * 0.04);
                }
                for (int i = 0, n = count(6); i < n; i++) {
                    level.addParticle(ParticleTypes.SPLASH, x + (random.nextDouble() - 0.5) * 0.3, surface + 0.02,
                            z + (random.nextDouble() - 0.5) * 0.3, (random.nextDouble() - 0.5) * 0.1,
                            0.08 + random.nextDouble() * 0.08, (random.nextDouble() - 0.5) * 0.1);
                }
            }
            case StationEffects.FORGE_WORK -> {
                for (int i = 0, n = count(reducedFlash ? 2 : 3 + Math.round(strength * 4)); i < n; i++) {
                    double across = (random.nextDouble() - 0.5) * 0.12;
                    level.addParticle(ModParticles.SPARK.get(), x + fx * 0.1, y + 0.88, z + fz * 0.1,
                            fx * 0.05 - fz * across, 0.07 + random.nextDouble() * 0.09, fz * 0.05 + fx * across);
                }
            }
            case StationEffects.IGNITE -> {
                ParticleOptions particle = reducedFlash ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME;
                for (int i = 0, n = count(6); i < n; i++) {
                    double across = (random.nextDouble() - 0.5) * 0.45;
                    level.addParticle(particle, x + fx * 0.3 - fz * across, y + 0.2, z + fz * 0.3 + fx * across,
                            fx * 0.015, 0.025 + random.nextDouble() * 0.025, fz * 0.015);
                }
            }
            default -> { }
        }
    }

    private static int count(int requested) {
        int count = Math.min(remaining, ClientConfig.scaleParticles(requested));
        remaining -= count;
        return count;
    }

    private ClientEffects() {}
}
