package com.otectus.immersivesmithing.item;

import com.otectus.immersivesmithing.workpiece.WorkpieceCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A hot workpiece left behind when a Smith's Anvil is broken. It cannot be used as equipment; pick it up with
 * Smithing Tongs, place it back on an anvil, or melt it down in a forge.
 */
public class HotWorkpieceItem extends Item {

    public HotWorkpieceItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return WorkpieceCodec.getHeld(stack)
                .<Component>map(d -> Component.translatable("item.immersive_smithing.hot_workpiece.of", d.target().getHoverName()))
                .orElseGet(() -> super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        WorkpieceCodec.getHeld(stack).ifPresent(d -> SmithingTongsItem.appendWorkpiece(d, tooltip));
        tooltip.add(Component.translatable("tooltip.immersive_smithing.hot_workpiece").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
