package com.otectus.otessmithing.quality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Quality labels. Faulty is a separate failure state, never a score band. */
public enum SmithingQuality {
    FAULTY("faulty", ChatFormatting.DARK_RED),
    CRUDE("crude", ChatFormatting.GRAY),
    STANDARD("standard", ChatFormatting.WHITE),
    FINE("fine", ChatFormatting.AQUA),
    MASTERWORK("masterwork", ChatFormatting.GOLD);

    private final String name;
    private final ChatFormatting color;

    SmithingQuality(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
    }

    public String id() {
        return name;
    }

    public ChatFormatting color() {
        return color;
    }

    public MutableComponent displayName() {
        return Component.translatable("quality.otes_smithing." + name).withStyle(color);
    }

    /** Score bands: 0-34 Crude, 35-69 Standard, 70-89 Fine, 90-100 Masterwork. */
    public static SmithingQuality fromScore(int score) {
        if (score < 35) return CRUDE;
        if (score < 70) return STANDARD;
        if (score < 90) return FINE;
        return MASTERWORK;
    }

    public static SmithingQuality overall(int forgeScore, int anvilScore, boolean faulty) {
        if (faulty) return FAULTY;
        return fromScore(overallScore(forgeScore, anvilScore));
    }

    public static int overallScore(int forgeScore, int anvilScore) {
        return Math.round((forgeScore + anvilScore) / 2.0F);
    }

    public static SmithingQuality byId(String id) {
        for (SmithingQuality q : values()) {
            if (q.name.equals(id)) return q;
        }
        return STANDARD;
    }
}
