package com.otectus.immersivesmithing.client;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.quality.QualityCalculator;
import com.otectus.immersivesmithing.quality.MakersMark;
import com.otectus.immersivesmithing.quality.QualityData;
import com.otectus.immersivesmithing.quality.SmithingQuality;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Client gameplay events: the quality tooltip. */
@Mod.EventBusSubscriber(modid = ImmersiveSmithing.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Optional<QualityData> data = QualityData.get(event.getItemStack());
        if (data.isEmpty()) return;
        QualityData q = data.get();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.immersive_smithing.quality", q.quality().displayName()).withStyle(ChatFormatting.GOLD));
        if (q.faulty()) {
            lines.add(Component.translatable("tooltip.immersive_smithing.faulty").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        Optional<MakersMark> mark = MakersMark.get(event.getItemStack());
        mark.ifPresent(m -> lines.add(Component.translatable("tooltip.immersive_smithing.crafted_by", m.smithName())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
        if (Screen.hasShiftDown()) {
            lines.add(detail("tooltip.immersive_smithing.durability_quality", q.durabilityQuality(), q.forgeScore(), QualityCalculator.durabilityMultiplier(q)));
            lines.add(Component.translatable("tooltip.immersive_smithing.durability_effect").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(detail("tooltip.immersive_smithing.efficacy_quality", q.efficacyQuality(), q.anvilScore(), QualityCalculator.efficacyMultiplier(q)));
            lines.add(Component.translatable("tooltip.immersive_smithing.efficacy_effect").withStyle(ChatFormatting.DARK_GRAY));
            if (q.faulty()) lines.add(Component.translatable("tooltip.immersive_smithing.faulty_next").withStyle(ChatFormatting.RED));
            if (mark.isEmpty()) lines.add(Component.translatable("tooltip.immersive_smithing.forged_by").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            lines.add(Component.translatable("tooltip.immersive_smithing.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private static Component detail(String key, SmithingQuality quality, int score, double multiplier) {
        MutableComponent label = quality.displayName().copy();
        if (ClientConfig.get(ClientConfig.SHOW_NUMERIC_QUALITY_SCORES) && quality != SmithingQuality.FAULTY) {
            label = Component.translatable("tooltip.immersive_smithing.quality_with_score", label, score);
        }
        return Component.translatable(key, label, Math.round(multiplier * 100.0)).withStyle(ChatFormatting.GRAY);
    }

    private ClientEvents() {}
}
