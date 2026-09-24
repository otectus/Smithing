package com.otectus.immersivesmithing.packtest;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.item.SmithingTier;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import com.otectus.immersivesmithing.workpiece.WorkpieceData;
import com.otectus.immersivesmithing.workpiece.WorkpieceState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Server half of the opt-in two-client remote tool-animation check. */
@Mod.EventBusSubscriber(modid = PackTestAgent.MOD_ID)
public final class MultiplayerPackTestFixture {
    private static boolean staged;
    private static boolean hammerStaged;

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean("immersive_smithing.packtest.multiplayer") || event.phase != TickEvent.Phase.END) return;
        if (staged) {
            stageHammerWhenReady(event.getServer().getPlayerList().getPlayerByName("VisualLeader"));
            return;
        }
        if (event.getServer().getPlayerList().getPlayerCount() < 2) return;
        ServerLevel level = event.getServer().overworld();
        ServerPlayer leader = event.getServer().getPlayerList().getPlayerByName("VisualLeader");
        ServerPlayer peer = event.getServer().getPlayerList().getPlayerByName("VisualPeer");
        if (leader == null || peer == null) return;
        for (int x = -4; x <= 9; x++) for (int z = -4; z <= 4; z++) {
            level.setBlockAndUpdate(new BlockPos(x, 83, z), Blocks.POLISHED_ANDESITE.defaultBlockState());
            for (int y = 84; y <= 88; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        level.setBlockAndUpdate(new BlockPos(0, 86, -1), Blocks.LIGHT.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(4, 86, -1), Blocks.LIGHT.defaultBlockState());
        ItemStack loadedTongs = new ItemStack(ModItems.tongs(SmithingTier.NETHERITE));
        WorkpieceCodec.setHeld(loadedTongs, new WorkpieceData(new ItemStack(Items.IRON_SWORD), ImmersiveSmithing.id("packtest"),
                new ResourceLocation("minecraft", "iron"), 18, 90, 90, false, WorkpieceState.SHAPED,
                ImmersiveSmithing.id("sword")));
        leader.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, loadedTongs);
        leader.setGameMode(GameType.CREATIVE);
        peer.setGameMode(GameType.CREATIVE);
        // The disposable flat-server fixture can initially report zero client light. Vanilla night
        // vision makes the arm/tool silhouettes inspectable; ordinary station lighting is tested separately.
        leader.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
        peer.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.NIGHT_VISION, 2400, 0, false, false));
        leader.teleportTo(level, .5, 84, .5, -90F, 0F);
        peer.teleportTo(level, 4.5, 84, 3.5, 126.87F, 5F);
        staged = true;
        try {
            Path shared = Path.of(System.getProperty("immersive_smithing.packtest.shared"));
            Files.createDirectories(shared);
            Files.writeString(shared.resolve("server-ready"), "two players staged\n", StandardCharsets.UTF_8);
        } catch (Exception failure) {
            ImmersiveSmithing.LOGGER.error("PACKTEST multiplayer fixture could not write ready marker", failure);
        }
    }

    private static void stageHammerWhenReady(ServerPlayer leader) {
        if (hammerStaged || leader == null) return;
        try {
            Path shared = Path.of(System.getProperty("immersive_smithing.packtest.shared"));
            if (!Files.exists(shared.resolve("tongs-observed"))) return;
            leader.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new ItemStack(ModItems.hammer(SmithingTier.NETHERITE)));
            hammerStaged = true;
            Files.writeString(shared.resolve("hammer-ready"), "remote hammer staged\n", StandardCharsets.UTF_8);
        } catch (Exception failure) {
            ImmersiveSmithing.LOGGER.error("PACKTEST multiplayer fixture could not stage hammer", failure);
        }
    }

    private MultiplayerPackTestFixture() {}
}
