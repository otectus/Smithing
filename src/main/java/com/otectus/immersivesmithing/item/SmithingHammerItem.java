package com.otectus.immersivesmithing.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import com.otectus.immersivesmithing.client.HammerClientExtensions;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/** Required to shape a workpiece on the Smith's Anvil. */
public class SmithingHammerItem extends SmithingToolItem {

    public SmithingHammerItem(SmithingTier tier, Properties properties) {
        super(tier, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(HammerClientExtensions.INSTANCE);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.immersive_smithing.hammer").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.immersive_smithing.anvil_time", tier().anvilTimeMs() / 1000).withStyle(ChatFormatting.DARK_GRAY));
    }
}
