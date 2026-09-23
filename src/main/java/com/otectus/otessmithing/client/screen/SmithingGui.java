package com.otectus.otessmithing.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.config.ClientConfig;
import com.otectus.otessmithing.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * Shared look of the smithing screens: dark iron panels with bronze trim and warm highlights. Every sprite
 * comes from {@code textures/gui/smithing.png} so resource packs can restyle the minigames.
 */
public final class SmithingGui {
    public static final ResourceLocation TEXTURE = OtesSmithing.id("textures/gui/smithing.png");

    public static final int TEXT = 0xFFE8DCC8;
    public static final int TEXT_DIM = 0xFF9C8F7C;
    public static final int TEXT_WARM = 0xFFFFB266;
    public static final int ROW_HOVER = 0x40FFB266;

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.blitNineSliced(TEXTURE, x, y, w, h, 8, 64, 64, 0, 0);
    }

    public static void well(GuiGraphics g, int x, int y, int w, int h) {
        g.blitNineSliced(TEXTURE, x, y, w, h, 4, 32, 32, 64, 0);
    }

    /** Draws the ring (outline) or disc sprite centred on (cx, cy), tinted with an ARGB colour. */
    public static void circle(GuiGraphics g, float cx, float cy, float radius, boolean ring, int argb) {
        int size = Math.max(2, Math.round(radius * 2));
        int x = Math.round(cx - size / 2F);
        int y = Math.round(cy - size / 2F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((argb >> 16) & 0xFF) / 255F, ((argb >> 8) & 0xFF) / 255F, (argb & 0xFF) / 255F, ((argb >>> 24) & 0xFF) / 255F);
        g.blit(TEXTURE, x, y, size, size, ring ? 96 : 160, 0, 64, 64, 256, 256);
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    /** A horizontal time bar that warms from gold to red as it empties. */
    public static void timeBar(GuiGraphics g, int x, int y, int w, int h, float remaining) {
        well(g, x - 1, y - 1, w + 2, h + 2);
        int filled = Math.round(w * Math.max(0F, Math.min(1F, remaining)));
        int color = remaining > 0.25F ? 0xFFE0A030 : (ClientConfig.get(ClientConfig.COLORBLIND_SAFE_TARGETS) ? 0xFFFF9F1C : 0xFFD04030);
        if (filled > 0) g.fill(x, y, x + filled, y + h, color);
    }

    public static int zoneColor() {
        if (ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES)) return 0xFFFFFFFF;
        return ClientConfig.get(ClientConfig.COLORBLIND_SAFE_TARGETS) ? 0xFF3D8BFF : 0xFFE07A2C;
    }

    /** Fill of an anvil strike target: cool against the hot silhouette. */
    public static int targetColor() {
        if (ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES)) return 0xFFFFFFFF;
        return ClientConfig.get(ClientConfig.COLORBLIND_SAFE_TARGETS) ? 0xFF3D8BFF : 0xFF4FD0E8;
    }

    public static int perfectColor() {
        if (ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES)) return 0xFFFFE600;
        return ClientConfig.get(ClientConfig.COLORBLIND_SAFE_TARGETS) ? 0xFFBFE0FF : 0xFFFFD66B;
    }

    public static int markerColor() {
        return ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES) ? 0xFF000000 : 0xFFFFFFFF;
    }

    public static int goodColor() {
        return ClientConfig.get(ClientConfig.COLORBLIND_SAFE_TARGETS) ? 0xFF3D8BFF : 0xFF5AD05A;
    }

    public static int missColor() {
        return ClientConfig.get(ClientConfig.COLORBLIND_SAFE_TARGETS) ? 0xFFFF9F1C : 0xFFE04848;
    }

    /** "Perfect!", "Good", "Rough" or "Miss" for an accuracy in 0..1. */
    public static Component rating(float accuracy) {
        String key = accuracy >= 0.9F ? "perfect" : accuracy >= 0.6F ? "good" : accuracy > 0F ? "rough" : "miss";
        return Component.translatable("minigame.otes_smithing.rating." + key);
    }

    public static int ratingColor(float accuracy) {
        if (accuracy >= 0.9F) return perfectColor();
        if (accuracy >= 0.6F) return goodColor();
        if (accuracy > 0F) return 0xFFE0C050;
        return missColor();
    }

    public static float markerScale() {
        return ClientConfig.get(ClientConfig.LARGE_MINIGAME_TARGETS) ? 1.6F : 1.0F;
    }

    public static void playUi(SoundEvent sound, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
    }

    public static void playCue() {
        if (!ClientConfig.get(ClientConfig.TIMING_CUE_SOUNDS)) return;
        float volume = (float) ClientConfig.get(ClientConfig.SOUND_CUE_VOLUME);
        if (volume > 0F) playUi(ModSounds.TIMING_CUE.get(), 1.4F, volume);
    }

    private SmithingGui() {}
}
