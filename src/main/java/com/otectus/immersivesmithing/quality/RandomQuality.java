package com.otectus.immersivesmithing.quality;

import com.otectus.immersivesmithing.config.ServerConfig;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Quality rolled for gear nobody forged: villager stock and, when enabled, loot. */
public final class RandomQuality {

    /** Two independent scores around 66 (sd 16): mostly Standard and Fine, Masterwork around 2%. */
    public static QualityData gaussian(RandomSource random, boolean allowFaulty) {
        boolean faulty = allowFaulty && random.nextFloat() < 0.05F;
        int forge = Mth.clamp((int) Math.round(66 + random.nextGaussian() * 16), 15, 100);
        int anvil = Mth.clamp((int) Math.round(66 + random.nextGaussian() * 16), 15, 100);
        return new QualityData(forge, anvil, faulty);
    }

    /** Both scores inside the Standard band (35 to 69): graded, never better than honest work. */
    public static QualityData standard(RandomSource random) {
        return new QualityData(35 + random.nextInt(35), 35 + random.nextInt(35), false);
    }

    public static QualityData forLoot(ServerConfig.LootQualityMode mode, RandomSource random) {
        return mode == ServerConfig.LootQualityMode.RANDOM ? gaussian(random, false) : standard(random);
    }

    private RandomQuality() {}
}
