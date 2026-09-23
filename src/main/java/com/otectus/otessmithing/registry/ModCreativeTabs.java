package com.otectus.otessmithing.registry;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.item.SmithingTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OtesSmithing.MOD_ID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.otes_smithing"))
            .icon(() -> new ItemStack(ModItems.hammer(SmithingTier.IRON)))
            .displayItems((params, output) -> {
                output.accept(ModItems.SMITHS_FORGE.get());
                output.accept(ModItems.SMITHS_ANVIL.get());
                output.accept(ModItems.SMITHS_TROUGH.get());
                output.accept(ModItems.SMITHS_GRINDSTONE.get());
                for (SmithingTier tier : SmithingTier.values()) {
                    output.accept(ModItems.tongs(tier));
                    output.accept(ModItems.hammer(tier));
                }
                output.accept(ModItems.SMITHING_GUIDE.get());
            })
            .build());

    private ModCreativeTabs() {}
}
