package com.otectus.otessmithing.client;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.config.ClientConfig;
import com.otectus.otessmithing.quality.QualityData;
import com.otectus.otessmithing.quality.SmithingQuality;
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
@Mod.EventBusSubscriber(modid = OtesSmithing.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Optional<QualityData> data = QualityData.get(event.getItemStack());
        if (data.isEmpty()) return;
        QualityData q = data.get();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.otes_smithing.quality", q.quality().displayName()).withStyle(ChatFormatting.GRAY));
        if (q.faulty()) {
            lines.add(Component.translatable("tooltip.otes_smithing.faulty").withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
        if (Screen.hasShiftDown()) {
            lines.add(detail("tooltip.otes_smithing.durability_quality", q.durabilityQuality(), q.forgeScore()));
            lines.add(detail("tooltip.otes_smithing.efficacy_quality", q.efficacyQuality(), q.anvilScore()));
            lines.add(Component.translatable("tooltip.otes_smithing.forged_by").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else {
            lines.add(Component.translatable("tooltip.otes_smithing.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
        List<Component> tooltip = event.getToolTip();
        tooltip.addAll(Math.min(1, tooltip.size()), lines);
    }

    private static Component detail(String key, SmithingQuality quality, int score) {
        MutableComponent label = quality.displayName();
        if (ClientConfig.get(ClientConfig.SHOW_NUMERIC_QUALITY_SCORES) && quality != SmithingQuality.FAULTY) {
            label = label.append(Component.literal(" (" + score + ")").withStyle(ChatFormatting.DARK_GRAY));
        }
        return Component.translatable(key, label).withStyle(ChatFormatting.GRAY);
    }

    private ClientEvents() {}
}
