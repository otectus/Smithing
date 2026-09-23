package com.otectus.otessmithing;

import com.mojang.logging.LogUtils;
import com.otectus.otessmithing.client.ClientSetup;
import com.otectus.otessmithing.config.ClientConfig;
import com.otectus.otessmithing.config.ServerConfig;
import com.otectus.otessmithing.network.ModNetwork;
import com.otectus.otessmithing.registry.ModBlockEntities;
import com.otectus.otessmithing.registry.ModBlocks;
import com.otectus.otessmithing.registry.ModCreativeTabs;
import com.otectus.otessmithing.registry.ModItems;
import com.otectus.otessmithing.registry.ModParticles;
import com.otectus.otessmithing.registry.ModRecipeSerializers;
import com.otectus.otessmithing.registry.ModRecipeTypes;
import com.otectus.otessmithing.registry.ModSounds;
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

@Mod(OtesSmithing.MOD_ID)
public class OtesSmithing {
    public static final String MOD_ID = "otes_smithing";
    public static final Logger LOGGER = LogUtils.getLogger();

    public OtesSmithing() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModRecipeTypes.RECIPE_TYPES.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

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
