package com.otectus.otessmithing.item;

import com.otectus.otessmithing.client.TongsClientExtensions;
import com.otectus.otessmithing.registry.ModItems;
import com.otectus.otessmithing.registry.ModSounds;
import com.otectus.otessmithing.workpiece.WorkpieceCodec;
import com.otectus.otessmithing.workpiece.WorkpieceData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Starts forging, and carries hot workpieces between Forge, Anvil and Trough. */
public class SmithingTongsItem extends SmithingToolItem {

    public SmithingTongsItem(SmithingTier tier, Properties properties) {
        super(tier, properties);
    }

    /** Tongs stab instead of swinging (first and third person). Only called on the client. */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(TongsClientExtensions.INSTANCE);
    }

    /** Using empty tongs in the air picks up a dropped Hot Workpiece from the inventory. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tongs = player.getItemInHand(hand);
        if (WorkpieceCodec.isHolding(tongs)) return InteractionResultHolder.pass(tongs);
        Inventory inv = player.getInventory();
        for (int slot = 0; slot < inv.getContainerSize(); slot++) {
            ItemStack candidate = inv.getItem(slot);
            if (!candidate.is(ModItems.HOT_WORKPIECE.get())) continue;
            Optional<WorkpieceData> data = WorkpieceCodec.getHeld(candidate);
            if (data.isEmpty()) continue;
            if (!level.isClientSide) {
                WorkpieceCodec.setHeld(tongs, data.get());
                candidate.shrink(1);
                level.playSound(null, player.blockPosition(), ModSounds.TONGS_GRAB.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return InteractionResultHolder.sidedSuccess(tongs, level.isClientSide);
        }
        return InteractionResultHolder.pass(tongs);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Optional<WorkpieceData> data = WorkpieceCodec.getHeld(stack);
        if (data.isPresent()) {
            appendWorkpiece(data.get(), tooltip);
        } else {
            tooltip.add(Component.translatable("tooltip.otes_smithing.tongs.empty").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.otes_smithing.forge_time", tier().forgeTimeMs() / 1000).withStyle(ChatFormatting.DARK_GRAY));
    }

    public static void appendWorkpiece(WorkpieceData data, List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.otes_smithing.workpiece.holding", data.target().getHoverName())
                .withStyle(ChatFormatting.GOLD));
        if (data.isShaped()) {
            if (data.faulty()) {
                tooltip.add(Component.translatable("tooltip.otes_smithing.workpiece.faulty").withStyle(ChatFormatting.DARK_RED));
            }
            tooltip.add(Component.translatable("tooltip.otes_smithing.workpiece.shaped").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.otes_smithing.workpiece.forged").withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }
}
