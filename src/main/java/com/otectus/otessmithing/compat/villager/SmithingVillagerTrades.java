package com.otectus.otessmithing.compat.villager;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.config.ServerConfig;
import com.otectus.otessmithing.item.SmithingTier;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

/**
 * Lightweight trades for the Armorer, Toolsmith and Weaponsmith. Forged equipment sold by villagers carries
 * a random quality centred on Standard/Fine; Masterwork is uncommon and Faulty is never sold unless enabled.
 * Every listing checks the config when the offer is generated, so disabling trades needs no restart.
 */
@Mod.EventBusSubscriber(modid = OtesSmithing.MOD_ID)
public final class SmithingVillagerTrades {

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        VillagerProfession profession = event.getType();
        var trades = event.getTrades();
        if (profession == VillagerProfession.TOOLSMITH) {
            trades.get(1).add(sell(() -> ModItems.tongs(SmithingTier.STONE), 2, 8, 2));
            trades.get(1).add(sell(() -> ModItems.hammer(SmithingTier.STONE), 2, 8, 2));
            trades.get(1).add(sell(ModItems.SMITHING_GUIDE::get, 1, 4, 1));
            trades.get(2).add(sell(() -> ModItems.tongs(SmithingTier.IRON), 7, 6, 10));
            trades.get(3).add(forged(() -> Items.IRON_PICKAXE, 9, 15));
            trades.get(3).add(forged(() -> Items.IRON_AXE, 8, 15));
            trades.get(4).add(sell(() -> ModItems.tongs(SmithingTier.DIAMOND), 22, 3, 20));
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            trades.get(1).add(sell(() -> ModItems.hammer(SmithingTier.STONE), 2, 8, 2));
            trades.get(2).add(sell(() -> ModItems.hammer(SmithingTier.IRON), 7, 6, 10));
            trades.get(3).add(forged(() -> Items.IRON_SWORD, 8, 15));
            trades.get(4).add(sell(() -> ModItems.hammer(SmithingTier.DIAMOND), 22, 3, 20));
        } else if (profession == VillagerProfession.ARMORER) {
            trades.get(1).add(sell(ModItems.SMITHING_GUIDE::get, 1, 4, 1));
            trades.get(3).add(forged(() -> Items.IRON_HELMET, 9, 15));
            trades.get(3).add(forged(() -> Items.IRON_BOOTS, 8, 15));
            trades.get(4).add(forged(() -> Items.IRON_CHESTPLATE, 16, 20));
            trades.get(4).add(forged(() -> Items.IRON_LEGGINGS, 14, 20));
        }
    }

    private static VillagerTrades.ItemListing sell(Supplier<? extends Item> item, int emeralds, int maxUses, int xp) {
        return (trader, random) -> {
            if (!ServerConfig.get(ServerConfig.ENABLE_VILLAGER_TRADES)) return null;
            return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item.get()), maxUses, xp, 0.05F);
        };
    }

    private static VillagerTrades.ItemListing forged(Supplier<? extends Item> item, int emeralds, int xp) {
        return (trader, random) -> {
            if (!ServerConfig.get(ServerConfig.ENABLE_VILLAGER_TRADES)) return null;
            ItemStack stack = new ItemStack(item.get());
            randomQuality(random).apply(stack);
            return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), stack, 3, xp, 0.2F);
        };
    }

    /** Two independent scores around 66 (sd 16): mostly Standard and Fine, Masterwork around 2%. */
    public static QualityData randomQuality(RandomSource random) {
        boolean faulty = ServerConfig.get(ServerConfig.ALLOW_FAULTY_VILLAGER_ITEMS) && random.nextFloat() < 0.05F;
        int forge = Mth.clamp((int) Math.round(66 + random.nextGaussian() * 16), 15, 100);
        int anvil = Mth.clamp((int) Math.round(66 + random.nextGaussian() * 16), 15, 100);
        return new QualityData(forge, anvil, faulty);
    }

    private SmithingVillagerTrades() {}
}
