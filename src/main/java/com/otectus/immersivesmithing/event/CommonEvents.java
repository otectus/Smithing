package com.otectus.immersivesmithing.event;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.block.SmithsAnvilBlock;
import com.otectus.immersivesmithing.block.SmithsForgeBlock;
import com.otectus.immersivesmithing.block.SmithsGrindstoneBlock;
import com.otectus.immersivesmithing.block.SmithsTroughBlock;
import com.otectus.immersivesmithing.blockentity.SmithsAnvilBlockEntity;
import com.otectus.immersivesmithing.blockentity.SmithsForgeBlockEntity;
import com.otectus.immersivesmithing.command.SmithingCommands;
import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.material.MaterialDefinitionLoader;
import com.otectus.immersivesmithing.material.RecyclingOverrideLoader;
import com.otectus.immersivesmithing.minigame.EquipmentClassifier;
import com.otectus.immersivesmithing.quality.MakersMark;
import com.otectus.immersivesmithing.quality.QualityData;
import net.minecraft.world.Container;
import com.otectus.immersivesmithing.minigame.PatternLoader;
import com.otectus.immersivesmithing.minigame.SessionManager;
import com.otectus.immersivesmithing.recipe.SmithingData;
import com.otectus.immersivesmithing.registry.ModItems;
import com.otectus.immersivesmithing.util.Feedback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

/** Gameplay events (Forge bus). */
@Mod.EventBusSubscriber(modid = ImmersiveSmithing.MOD_ID)
public final class CommonEvents {
    private static final String GUIDE_GRANTED = "immersive_smithing_guide_granted";

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MaterialDefinitionLoader(event.getConditionContext()));
        event.addListener(new RecyclingOverrideLoader(event.getConditionContext()));
        event.addListener(new PatternLoader(PatternLoader.Kind.FORGE));
        event.addListener(new PatternLoader(PatternLoader.Kind.ANVIL));
    }

    /** First build, once tags are bound and before any player can join. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SmithingData.rebuild(event.getServer());
    }

    /**
     * After /reload the new tags are bound and this fires before the recipe packet is sent, so rebuilding here
     * means clients receive the updated (filtered and generated) recipe list.
     */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            SmithingData.rebuild(event.getPlayerList().getServer());
        } else if (SmithingData.isDirty()) {
            SmithingData.rebuild(event.getPlayerList().getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SessionManager.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        SmithingData.reset();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) SessionManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SessionManager.onLogout(player);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SessionManager.onDeath(player);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SmithingCommands.register(event.getDispatcher(), event.getBuildContext());
    }

    /**
     * Vanilla skips a block's use() when the player sneaks with an item in hand. Station interactions such as
     * "sneak with tongs to melt a workpiece back" or "sneak with ingots to insert the stack" need it, so allow
     * it for non-block items. Sneaking with a block item still places blocks as usual.
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.isSecondaryUseActive()) return;
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (!(block instanceof SmithsForgeBlock || block instanceof SmithsAnvilBlock || block instanceof SmithsTroughBlock
                || block instanceof SmithsGrindstoneBlock)) return;
        ItemStack held = event.getItemStack();
        if (!held.isEmpty() && !(held.getItem() instanceof BlockItem)) event.setUseBlock(Event.Result.ALLOW);
    }

    /** A forge holding molten metal cannot be broken in survival; an anvil in use by someone else is protected. */
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.getAbilities().instabuild) return;
        var be = event.getLevel().getBlockEntity(event.getPos());
        if (be instanceof SmithsForgeBlockEntity forge && (forge.moltenUnits() > 0 || forge.isLocked())) {
            event.setCanceled(true);
            Feedback.fail(player, "empty_forge_before_breaking");
        } else if (be instanceof SmithsAnvilBlockEntity anvil && anvil.isLockedFor(player.getUUID())) {
            event.setCanceled(true);
            Feedback.fail(player, "station_in_use");
        }
    }

    /**
     * Quality inheritance: a recipe that turns one smithed piece into another (dyeing a helmet, an elemental
     * upgrade of a sword) keeps the quality and the maker's mark of the piece it consumed. Only when exactly one
     * input is graded and the result is graded equipment without a grade of its own.
     */
    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!ServerConfig.get(ServerConfig.INHERIT_QUALITY_ON_CRAFT)) return;
        ItemStack result = event.getCrafting();
        if (result.isEmpty() || QualityData.has(result) || !EquipmentClassifier.isCandidate(result)) return;
        Container inputs = event.getInventory();
        ItemStack source = ItemStack.EMPTY;
        for (int i = 0; i < inputs.getContainerSize(); i++) {
            ItemStack input = inputs.getItem(i);
            if (input.isEmpty() || !QualityData.has(input)) continue;
            if (!source.isEmpty()) return; // two graded inputs: no honest answer
            source = input;
        }
        if (source.isEmpty()) return;
        QualityData.get(source).ifPresent(q -> q.apply(result));
        MakersMark.copy(source, result);
    }

    /** The first smithing advancement hands out the Smithing Guide, once per player. */
    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent.AdvancementEarnEvent event) {
        if (!event.getAdvancement().getId().equals(ImmersiveSmithing.id("the_trade"))) return;
        if (!(event.getEntity() instanceof ServerPlayer player) || !ServerConfig.get(ServerConfig.GRANT_GUIDE_BOOK_AUTOMATICALLY)) return;
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (persisted.getBoolean(GUIDE_GRANTED)) return;
        persisted.putBoolean(GUIDE_GRANTED, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.SMITHING_GUIDE.get()));
        Feedback.info(player, "guide_granted");
    }

    private CommonEvents() {}
}
