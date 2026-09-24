package com.otectus.immersivesmithing.client;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.client.particle.SparkParticle;
import com.otectus.immersivesmithing.client.renderer.SmithsAnvilRenderer;
import com.otectus.immersivesmithing.client.renderer.SmithsForgeRenderer;
import com.otectus.immersivesmithing.client.renderer.SmithsTroughRenderer;
import com.otectus.immersivesmithing.client.renderer.StationModels;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.registry.ModBlockEntities;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.registry.ModParticles;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only mod-bus registration. Reached only through DistExecutor, so it never loads on a server. */
public final class ClientSetup {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClientSetup::onClientSetup);
        modBus.addListener(ClientSetup::onRegisterRenderers);
        modBus.addListener(ClientSetup::onRegisterParticles);
        modBus.addListener(ClientSetup::onRegisterModels);
    }

    private static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        StationModels.ALL.forEach(event::register);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (SmithingTier tier : SmithingTier.values()) {
                ItemProperties.register(ModItems.tongs(tier), ImmersiveSmithing.id("loaded"),
                        (stack, level, entity, seed) -> WorkpieceCodec.isHolding(stack) ? 1.0F : 0.0F);
            }
        });
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SMITHS_FORGE.get(), SmithsForgeRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SMITHS_ANVIL.get(), SmithsAnvilRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SMITHS_TROUGH.get(), SmithsTroughRenderer::new);
    }

    private static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.SPARK.get(), SparkParticle.Provider::new);
    }

    private ClientSetup() {}
}
