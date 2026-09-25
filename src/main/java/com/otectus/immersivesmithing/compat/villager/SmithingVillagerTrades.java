package com.otectus.immersivesmithing.compat.villager;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.quality.RandomQuality;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import com.otectus.immersivesmithing.registry.ModItems;
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

import java.util.List;
import java.util.function.Supplier;

/**
 * Lightweight trades for the Armorer, Toolsmith and Weaponsmith. Forged equipment sold by villagers carries
 * a random quality centred on Standard/Fine; Masterwork is uncommon and Faulty is never sold unless enabled.
 * Every listing checks the config when the offer is generated, so disabling trades needs no restart.
 */
@Mod.EventBusSubscriber(modid = ImmersiveSmithing.MOD_ID)
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
            modded(trades, 4, "iceandfire:silver_pickaxe", 14, 20);
            modded(trades, 5, "cataclysm:black_steel_pickaxe", 26, 30);
        } else if (profession == VillagerProfession.WEAPONSMITH) {
            trades.get(1).add(sell(() -> ModItems.hammer(SmithingTier.STONE), 2, 8, 2));
            trades.get(2).add(sell(() -> ModItems.hammer(SmithingTier.IRON), 7, 6, 10));
            trades.get(3).add(forged(() -> Items.IRON_SWORD, 8, 15));
            trades.get(4).add(sell(() -> ModItems.hammer(SmithingTier.DIAMOND), 22, 3, 20));
            modded(trades, 4, "iceandfire:silver_sword", 13, 20);
            modded(trades, 5, "cataclysm:black_steel_sword", 24, 30);
        } else if (profession == VillagerProfession.ARMORER) {
            trades.get(1).add(sell(ModItems.SMITHING_GUIDE::get, 1, 4, 1));
            trades.get(3).add(forged(() -> Items.IRON_HELMET, 9, 15));
            trades.get(3).add(forged(() -> Items.IRON_BOOTS, 8, 15));
            trades.get(4).add(forged(() -> Items.IRON_CHESTPLATE, 16, 20));
            trades.get(4).add(forged(() -> Items.IRON_LEGGINGS, 14, 20));
            modded(trades, 5, "iceandfire:armor_silver_metal_chestplate", 24, 30);
        }
    }

    private static VillagerTrades.ItemListing sell(Supplier<? extends Item> item, int emeralds, int maxUses, int xp) {
        return (trader, random) -> {
            if (!ServerConfig.get(ServerConfig.ENABLE_VILLAGER_TRADES)) return null;
            return new MerchantOffer(new ItemStack(Items.EMERALD, emeralds), new ItemStack(item.get()), maxUses, xp, 0.05F);
        };
    }

    /** A forged offer for another mod's item, added only when that mod (the item's namespace) is loaded. */
    private static void modded(Int2ObjectMap<List<VillagerTrades.ItemListing>> trades, int level, String itemId, int emeralds, int xp) {
        ResourceLocation id = new ResourceLocation(itemId);
        if (!ModList.get().isLoaded(id.getNamespace()) || !ForgeRegistries.ITEMS.containsKey(id)) return;
        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == Items.AIR) return;
        trades.get(level).add(forged(() -> item, emeralds, xp));
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
        return RandomQuality.gaussian(random, ServerConfig.get(ServerConfig.ALLOW_FAULTY_VILLAGER_ITEMS));
    }

    private SmithingVillagerTrades() {}
}
