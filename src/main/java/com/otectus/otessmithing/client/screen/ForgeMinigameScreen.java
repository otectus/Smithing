package com.otectus.otessmithing.client.screen;

import com.otectus.otessmithing.material.MaterialUnits;
import com.otectus.otessmithing.minigame.ForgeMinigame;
import com.otectus.otessmithing.minigame.ForgeRun;
import com.otectus.otessmithing.network.ModNetwork;
import com.otectus.otessmithing.network.packet.CancelSessionPacket;
import com.otectus.otessmithing.network.packet.ForgeActionPacket;
import com.otectus.otessmithing.network.packet.ForgeActionResultPacket;
import com.otectus.otessmithing.network.packet.ForgeStartPacket;
import com.otectus.otessmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.otessmithing.network.packet.SelectForgeRecipePacket;
import com.otectus.otessmithing.network.packet.SessionResultPacket;
import com.otectus.otessmithing.quality.SmithingQuality;
import com.otectus.otessmithing.config.ClientConfig;
import com.otectus.otessmithing.registry.ModSounds;
import com.otectus.otessmithing.registry.ModTags;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Forge screen: recipe selection, a short countdown, the forming-rhythm minigame and the result. The
 * client predicts feedback locally but only ever sends input events; the score shown at the end is the
 * server's.
 */
public class ForgeMinigameScreen extends Screen {
    private enum Stage { SELECT, WAITING, READY, PLAYING, RESULT }

    private static final int WIDTH = 248;
    private static final int HEIGHT = 200;
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 42;
    private static final int LIST_ROWS = 5;
    private static final int TRACK_WIDTH = 200;

    private final OpenForgeScreenPacket offer;
    private final List<OpenForgeScreenPacket.Entry> filtered = new ArrayList<>();
    private Stage stage = Stage.SELECT;
    private EditBox search;
    private Button doneButton;
    private int scroll;
    private int left;
    private int top;

    private ItemStack selected = ItemStack.EMPTY;
    private long clientStartMs;
    private int allowedMs;
    private ForgeRun run;
    private final List<Float> phaseResults = new ArrayList<>();
    private float lastAccuracy = -1F;
    private long lastFeedbackMs;
    private boolean markerInZone;
    private int resultScore;
    private boolean resultTimedOut;
    private boolean closedByServer;

    public ForgeMinigameScreen(OpenForgeScreenPacket offer) {
        super(Component.translatable("screen.otes_smithing.forge"));
        this.offer = offer;
    }

    public int sessionId() {
        return offer.sessionId();
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        String previous = search != null ? search.getValue() : "";
        search = new EditBox(font, left + 12, top + 24, WIDTH - 24, 14, Component.translatable("screen.otes_smithing.forge.search"));
        search.setHint(Component.translatable("screen.otes_smithing.forge.search").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(previous);
        search.setResponder(s -> refilter());
        search.visible = stage == Stage.SELECT;
        addRenderableWidget(search);
        doneButton = Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH / 2 - 40, top + HEIGHT - 30, 80, 20).build();
        doneButton.visible = stage == Stage.RESULT;
        addRenderableWidget(doneButton);
        refilter();
    }

    private void refilter() {
        filtered.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (OpenForgeScreenPacket.Entry e : offer.entries()) {
            if (query.isEmpty() || e.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) filtered.add(e);
        }
        scroll = Mth.clamp(scroll, 0, Math.max(0, filtered.size() - LIST_ROWS));
    }

    // ------------------------------------------------------------------ server events

    public void onStart(ForgeStartPacket packet) {
        if (packet.sessionId() != offer.sessionId()) return;
        selected = packet.result();
        allowedMs = packet.allowedMs();
        run = new ForgeRun(ForgeMinigame.phases(packet.pattern(), packet.seed(), packet.metalUnits()), packet.pattern().transitionMs());
        clientStartMs = Util.getMillis() + packet.readyDelayMs();
        stage = Stage.READY;
        search.visible = false;
        setFocused(null);
    }

    public void onActionResult(ForgeActionResultPacket packet) {
        if (packet.sessionId() != offer.sessionId()) return;
        if (packet.phase() >= 0 && packet.phase() < phaseResults.size()) {
            phaseResults.set(packet.phase(), packet.accuracy());
            if (packet.phase() == phaseResults.size() - 1) lastAccuracy = packet.accuracy();
        }
    }

    public void onResult(SessionResultPacket packet) {
        if (packet.sessionId() != offer.sessionId()) return;
        resultScore = packet.score();
        resultTimedOut = packet.timedOut();
        stage = Stage.RESULT;
        doneButton.visible = true;
        SmithingGui.playUi(ModSounds.FORGE_READY.get(), 1.0F, 0.8F);
    }

    public void closeByServer() {
        closedByServer = true;
        onClose();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (stage == Stage.SELECT && button == 0) {
            int index = rowAt(mouseX, mouseY);
            if (index >= 0) {
                select(filtered.get(index));
                return true;
            }
        }
        if (stage == Stage.PLAYING && button == 0 && inside(mouseX, mouseY)) {
            act();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (stage == Stage.PLAYING && (key == GLFW.GLFW_KEY_SPACE || key == GLFW.GLFW_KEY_ENTER)) {
            act();
            return true;
        }
        if (stage == Stage.SELECT && key == GLFW.GLFW_KEY_ENTER && !filtered.isEmpty()) {
            select(filtered.get(0));
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (stage == Stage.SELECT) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, Math.max(0, filtered.size() - LIST_ROWS));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void select(OpenForgeScreenPacket.Entry entry) {
        selected = entry.result();
        stage = Stage.WAITING;
        search.visible = false;
        ModNetwork.CHANNEL.sendToServer(new SelectForgeRecipePacket(offer.sessionId(), entry.recipeId()));
        SmithingGui.playUi(ModSounds.TONGS_GRAB.get(), 1.0F, 0.6F);
    }

    private void act() {
        if (run == null || run.isComplete()) return;
        int now = elapsed();
        if (now < 0 || now > allowedMs) return;
        int phase = run.index();
        float accuracy = run.press(now);
        if (accuracy < 0) return;
        ModNetwork.CHANNEL.sendToServer(new ForgeActionPacket(offer.sessionId(), phase, now));
        phaseResults.add(accuracy);
        lastAccuracy = accuracy;
        lastFeedbackMs = Util.getMillis();
        SmithingGui.playUi(ModSounds.FORGE_WORK.get(), 0.9F + accuracy * 0.3F, 0.8F);
        // A stab into the forge; vanilla broadcasts the swing, so other players see it too.
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.swing(minecraft.player.getMainHandItem().is(ModTags.TONGS) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
        }
    }

    private int elapsed() {
        return (int) (Util.getMillis() - clientStartMs);
    }

    private boolean inside(double x, double y) {
        return x >= left && x < left + WIDTH && y >= top && y < top + HEIGHT;
    }

    private int rowAt(double x, double y) {
        int rx = left + 10;
        int ry = top + LIST_TOP;
        if (x < rx || x >= rx + WIDTH - 20 || y < ry || y >= ry + LIST_ROWS * ROW_HEIGHT) return -1;
        int index = scroll + (int) ((y - ry) / ROW_HEIGHT);
        return index < filtered.size() ? index : -1;
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void tick() {
        if (stage == Stage.READY && elapsed() >= 0) stage = Stage.PLAYING;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (stage != Stage.RESULT && !closedByServer) {
            ModNetwork.CHANNEL.sendToServer(new CancelSessionPacket(offer.sessionId()));
        }
        super.removed();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        SmithingGui.panel(g, left, top, WIDTH, HEIGHT);
        Component title = Component.translatable("screen.otes_smithing.forge.title", offer.familyName());
        g.drawString(font, title, left + 12, top + 9, SmithingGui.TEXT_WARM, false);
        Component units = MaterialUnits.describe(offer.moltenUnits());
        g.drawString(font, units, left + WIDTH - 12 - font.width(units), top + 9, SmithingGui.TEXT_DIM, false);

        switch (stage) {
            case SELECT -> renderSelection(g, mouseX, mouseY);
            case WAITING -> g.drawCenteredString(font, Component.translatable("screen.otes_smithing.preparing"), left + WIDTH / 2, top + HEIGHT / 2, SmithingGui.TEXT_DIM);
            case READY -> renderReady(g);
            case PLAYING -> renderPlaying(g);
            case RESULT -> renderResult(g);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (stage == Stage.SELECT) renderRowTooltip(g, mouseX, mouseY);
    }

    private void renderSelection(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 10;
        int y = top + LIST_TOP;
        int w = WIDTH - 20;
        SmithingGui.well(g, x - 2, y - 2, w + 4, LIST_ROWS * ROW_HEIGHT + 4);
        if (filtered.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.otes_smithing.forge.no_match"), left + WIDTH / 2, y + 40, SmithingGui.TEXT_DIM);
        }
        for (int row = 0; row < LIST_ROWS; row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            OpenForgeScreenPacket.Entry e = filtered.get(index);
            int ry = y + row * ROW_HEIGHT;
            if (rowAt(mouseX, mouseY) == index) g.fill(x, ry, x + w - 6, ry + ROW_HEIGHT, SmithingGui.ROW_HOVER);
            g.renderItem(e.result(), x + 3, ry + 4);
            g.drawString(font, e.result().getHoverName(), x + 24, ry + 3, SmithingGui.TEXT, false);
            Component cost = Component.translatable("screen.otes_smithing.forge.cost", MaterialUnits.describe(e.metalUnits()),
                    MaterialUnits.describe(offer.moltenUnits() - e.metalUnits()));
            g.drawString(font, cost, x + 24, ry + 13, SmithingGui.TEXT_DIM, false);
            int ax = x + w - 26;
            for (ItemStack aux : e.auxiliary()) {
                if (ax < x + 150) break;
                g.renderItem(aux, ax, ry + 4);
                g.renderItemDecorations(font, aux, ax, ry + 4);
                ax -= 18;
            }
        }
        // scrollbar
        if (filtered.size() > LIST_ROWS) {
            int trackH = LIST_ROWS * ROW_HEIGHT;
            int barH = Math.max(12, trackH * LIST_ROWS / filtered.size());
            int barY = y + (trackH - barH) * scroll / Math.max(1, filtered.size() - LIST_ROWS);
            g.fill(x + w - 4, y, x + w - 1, y + trackH, 0x40000000);
            g.fill(x + w - 4, barY, x + w - 1, barY + barH, 0xFFB08050);
        }
        Component hint = Component.translatable("screen.otes_smithing.forge.choose", offer.timeMs() / 1000);
        g.drawCenteredString(font, hint, left + WIDTH / 2, top + HEIGHT - 20, SmithingGui.TEXT_DIM);
    }

    private void renderRowTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int index = rowAt(mouseX, mouseY);
        if (index < 0) return;
        OpenForgeScreenPacket.Entry e = filtered.get(index);
        int x = left + 10;
        int w = WIDTH - 20;
        if (mouseX < x + 24) {
            g.renderTooltip(font, e.result(), mouseX, mouseY);
            return;
        }
        int ax = x + w - 26;
        for (ItemStack aux : e.auxiliary()) {
            if (ax < x + 150) break;
            if (mouseX >= ax && mouseX < ax + 16) {
                g.renderTooltip(font, aux, mouseX, mouseY);
                return;
            }
            ax -= 18;
        }
    }

    private void renderWorkpiece(GuiGraphics g, int cx, int cy, float scale) {
        g.pose().pushPose();
        g.pose().translate(cx - 8 * scale, cy - 8 * scale, 0);
        g.pose().scale(scale, scale, 1F);
        g.renderItem(selected, 0, 0);
        g.pose().popPose();
    }

    private void renderReady(GuiGraphics g) {
        int remaining = -elapsed();
        renderWorkpiece(g, left + WIDTH / 2, top + 80, 3F);
        g.drawCenteredString(font, selected.getHoverName(), left + WIDTH / 2, top + 112, SmithingGui.TEXT);
        Component ready = Component.translatable("screen.otes_smithing.ready", Math.max(1, (remaining + 999) / 1000));
        g.drawCenteredString(font, ready, left + WIDTH / 2, top + 132, SmithingGui.TEXT_WARM);
        g.drawCenteredString(font, Component.translatable("screen.otes_smithing.forge.instructions"), left + WIDTH / 2, top + 152, SmithingGui.TEXT_DIM);
    }

    private void renderPlaying(GuiGraphics g) {
        int now = elapsed();
        float remaining = 1F - now / (float) Math.max(1, allowedMs);
        SmithingGui.timeBar(g, left + 12, top + 24, WIDTH - 24, 6, remaining);

        renderWorkpiece(g, left + 44, top + 70, 3F);
        int phaseCount = run.phases().size();
        int shown = Math.min(run.index() + 1, phaseCount);
        g.drawString(font, Component.translatable("screen.otes_smithing.forge.phase", shown, phaseCount), left + 88, top + 44, SmithingGui.TEXT, false);
        for (int i = 0; i < phaseCount; i++) {
            int px = left + 88 + i * 12;
            int color = i < phaseResults.size() ? SmithingGui.ratingColor(phaseResults.get(i)) : 0xFF4A4038;
            g.fill(px, top + 58, px + 9, top + 64, color);
        }
        if (!phaseResults.isEmpty()) {
            float sum = 0;
            for (float f : phaseResults) sum += f;
            int projected = Math.round(sum / phaseCount * 100F);
            g.drawString(font, Component.translatable("screen.otes_smithing.projected",
                    SmithingQuality.fromScore(projected).displayName()), left + 88, top + 72, SmithingGui.TEXT_DIM, false);
        }

        // Track
        float scale = SmithingGui.markerScale();
        int trackH = Math.round(16 * scale);
        int tx = left + (WIDTH - TRACK_WIDTH) / 2;
        int ty = top + 118 - trackH / 2;
        SmithingGui.well(g, tx - 2, ty - 2, TRACK_WIDTH + 4, trackH + 4);
        if (run.isComplete() || now > allowedMs) {
            g.drawCenteredString(font, Component.translatable(now > allowedMs ? "screen.otes_smithing.times_up" : "screen.otes_smithing.finishing"),
                    left + WIDTH / 2, ty + trackH / 2 - 4, SmithingGui.TEXT_DIM);
        } else {
            ForgeMinigame.Phase phase = run.phases().get(run.index());
            int zoneL = tx + Math.round((phase.center() - phase.halfWidth()) * TRACK_WIDTH);
            int zoneR = tx + Math.round((phase.center() + phase.halfWidth()) * TRACK_WIDTH);
            int perfL = tx + Math.round((phase.center() - phase.halfWidth() * 0.2F) * TRACK_WIDTH);
            int perfR = tx + Math.round((phase.center() + phase.halfWidth() * 0.2F) * TRACK_WIDTH);
            if (ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES)) g.fill(zoneL - 1, ty - 1, zoneR + 1, ty + trackH + 1, 0xFF000000);
            g.fill(zoneL, ty, zoneR, ty + trackH, SmithingGui.zoneColor());
            g.fill(perfL, ty, Math.max(perfR, perfL + 1), ty + trackH, SmithingGui.perfectColor());

            int local = now - run.phaseStartMs();
            if (local >= 0) {
                float position = ForgeMinigame.position(phase, local / 1000F);
                int mx = tx + Math.round(position * TRACK_WIDTH);
                int half = Math.max(1, Math.round(1.5F * scale));
                if (ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES)) g.fill(mx - half - 1, ty - 5, mx + half + 1, ty + trackH + 5, 0xFFFFFFFF);
                g.fill(mx - half, ty - 4, mx + half, ty + trackH + 4, SmithingGui.markerColor());
                boolean inZone = Math.abs(position - phase.center()) <= phase.halfWidth();
                if (inZone && !markerInZone) SmithingGui.playCue();
                markerInZone = inZone;
            } else {
                markerInZone = false;
            }
        }

        if (lastAccuracy >= 0 && Util.getMillis() - lastFeedbackMs < 900) {
            g.drawCenteredString(font, SmithingGui.rating(lastAccuracy), left + WIDTH / 2, top + 142, SmithingGui.ratingColor(lastAccuracy));
        }
        g.drawCenteredString(font, Component.translatable("screen.otes_smithing.forge.instructions"), left + WIDTH / 2, top + HEIGHT - 22, SmithingGui.TEXT_DIM);
    }

    private void renderResult(GuiGraphics g) {
        renderWorkpiece(g, left + WIDTH / 2, top + 64, 3F);
        Component headline = Component.translatable(resultTimedOut ? "screen.otes_smithing.forge.timeout" : "screen.otes_smithing.forge.complete");
        g.drawCenteredString(font, headline, left + WIDTH / 2, top + 96, resultTimedOut ? SmithingGui.missColor() : SmithingGui.TEXT_WARM);
        Component quality = SmithingQuality.fromScore(resultScore).displayName();
        if (ClientConfig.get(ClientConfig.SHOW_NUMERIC_QUALITY_SCORES)) quality = quality.copy().append(" (" + resultScore + ")");
        g.drawCenteredString(font, Component.translatable("screen.otes_smithing.forge.durability", quality), left + WIDTH / 2, top + 112, SmithingGui.TEXT);
        g.drawCenteredString(font, Component.translatable("screen.otes_smithing.forge.next"), left + WIDTH / 2, top + 132, SmithingGui.TEXT_DIM);
    }
}
