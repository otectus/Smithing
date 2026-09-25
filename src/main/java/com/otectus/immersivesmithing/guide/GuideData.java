package com.otectus.immersivesmithing.guide;

import net.minecraft.network.chat.Component;

/**
 * Chapter list of the Smithing Guide. Text lives in the language files under
 * {@code guide.immersive_smithing.chapter.<n>.title} and {@code guide.immersive_smithing.chapter.<n>.text}, so resource
 * packs can translate or rewrite it.
 */
public final class GuideData {
    public static final String[] CHAPTERS = {
            "getting_started", "smiths_forge", "fuels_and_ignition", "material_families", "forge_minigame",
            "smithing_tongs", "smiths_anvil", "anvil_minigame", "smiths_trough", "makers_mark", "equipment_quality",
            "faulty_equipment", "reforging", "smiths_grindstone", "tool_tiers", "netherite", "modded_equipment",
            "automation", "jei", "troubleshooting"
    };

    public static int chapterCount() {
        return CHAPTERS.length;
    }

    /** Chapters are numbered from 1 in the book. */
    public static Component chapterTitle(int chapter) {
        int index = Math.max(1, Math.min(CHAPTERS.length, chapter));
        return Component.translatable("guide.immersive_smithing." + CHAPTERS[index - 1] + ".title");
    }

    public static String textKey(int chapter) {
        int index = Math.max(1, Math.min(CHAPTERS.length, chapter));
        return "guide.immersive_smithing." + CHAPTERS[index - 1] + ".text";
    }

    private GuideData() {}
}
