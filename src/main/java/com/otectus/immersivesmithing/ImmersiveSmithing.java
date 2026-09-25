package com.otectus.immersivesmithing;

import com.mojang.logging.LogUtils;
import com.otectus.immersivesmithing.client.ClientSetup;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.network.ModNetwork;
import com.otectus.immersivesmithing.registry.ModBlockEntities;
import com.otectus.immersivesmithing.registry.ModBlocks;
import com.otectus.immersivesmithing.registry.ModCreativeTabs;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.registry.ModLootModifiers;
import com.otectus.immersivesmithing.registry.ModParticles;
import com.otectus.immersivesmithing.registry.ModRecipeSerializers;
import com.otectus.immersivesmithing.registry.ModRecipeTypes;
import com.otectus.immersivesmithing.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ImmersiveSmithing.MOD_ID)
public class ImmersiveSmithing {
    public static final String MOD_ID = "immersive_smithing";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ImmersiveSmithing() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModRecipeTypes.RECIPE_TYPES.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modBus.addListener(this::commonSetup);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.init(modBus));
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
