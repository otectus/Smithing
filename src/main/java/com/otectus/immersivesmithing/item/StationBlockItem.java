package com.otectus.immersivesmithing.item;

import com.otectus.immersivesmithing.guide.GuideData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Block item for a smithing station, pointing to its Smithing Guide chapter. */
public class StationBlockItem extends BlockItem {
    private final int guideChapter;

    public StationBlockItem(Block block, Properties properties, int guideChapter) {
        super(block, properties);
        this.guideChapter = guideChapter;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.immersive_smithing.guide_link", GuideData.chapterTitle(guideChapter))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
