package com.otectus.immersivesmithing.registry;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.item.HotWorkpieceItem;
import com.otectus.immersivesmithing.item.SmithingGuideItem;
import com.otectus.immersivesmithing.item.SmithingHammerItem;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.item.SmithingTongsItem;
import com.otectus.immersivesmithing.item.StationBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ImmersiveSmithing.MOD_ID);

    public static final RegistryObject<Item> SMITHS_FORGE = ITEMS.register("smiths_forge",
            () -> new StationBlockItem(ModBlocks.SMITHS_FORGE.get(), new Item.Properties(), 2));
    public static final RegistryObject<Item> SMITHS_ANVIL = ITEMS.register("smiths_anvil",
            () -> new StationBlockItem(ModBlocks.SMITHS_ANVIL.get(), new Item.Properties(), 7));
    public static final RegistryObject<Item> SMITHS_TROUGH = ITEMS.register("smiths_trough",
            () -> new StationBlockItem(ModBlocks.SMITHS_TROUGH.get(), new Item.Properties(), 9));
    public static final RegistryObject<Item> SMITHS_GRINDSTONE = ITEMS.register("smiths_grindstone",
            () -> new StationBlockItem(ModBlocks.SMITHS_GRINDSTONE.get(), new Item.Properties(), 13));

    public static final Map<SmithingTier, RegistryObject<SmithingTongsItem>> TONGS = new EnumMap<>(SmithingTier.class);
    public static final Map<SmithingTier, RegistryObject<SmithingHammerItem>> HAMMERS = new EnumMap<>(SmithingTier.class);

    static {
        for (SmithingTier tier : SmithingTier.values()) {
            TONGS.put(tier, ITEMS.register(tier.getSerializedName() + "_smithing_tongs",
                    () -> new SmithingTongsItem(tier, tier.itemProperties())));
            HAMMERS.put(tier, ITEMS.register(tier.getSerializedName() + "_smithing_hammer",
                    () -> new SmithingHammerItem(tier, tier.itemProperties())));
        }
    }

    public static final RegistryObject<SmithingGuideItem> SMITHING_GUIDE = ITEMS.register("smithing_guide",
            () -> new SmithingGuideItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<HotWorkpieceItem> HOT_WORKPIECE = ITEMS.register("hot_workpiece",
            () -> new HotWorkpieceItem(new Item.Properties().stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON)));

    public static SmithingTongsItem tongs(SmithingTier tier) {
        return TONGS.get(tier).get();
    }

    public static SmithingHammerItem hammer(SmithingTier tier) {
        return HAMMERS.get(tier).get();
    }

    private ModItems() {}
}
