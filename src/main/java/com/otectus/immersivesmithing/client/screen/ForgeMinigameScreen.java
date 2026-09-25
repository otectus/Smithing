package com.otectus.immersivesmithing.client.screen;

import com.otectus.immersivesmithing.material.MaterialUnits;
import com.otectus.immersivesmithing.minigame.ForgeMinigame;
import com.otectus.immersivesmithing.minigame.ForgeRun;
import com.otectus.immersivesmithing.network.ModNetwork;
import com.otectus.immersivesmithing.network.packet.CancelSessionPacket;
import com.otectus.immersivesmithing.network.packet.ForgeActionPacket;
import com.otectus.immersivesmithing.network.packet.ForgeActionResultPacket;
import com.otectus.immersivesmithing.network.packet.ForgeStartPacket;
import com.otectus.immersivesmithing.network.packet.OpenForgeScreenPacket;
import com.otectus.immersivesmithing.network.packet.SelectForgeRecipePacket;
import com.otectus.immersivesmithing.network.packet.SessionResultPacket;
import com.otectus.immersivesmithing.quality.SmithingQuality;
import com.otectus.immersivesmithing.config.ClientConfig;
import com.otectus.immersivesmithing.registry.ModSounds;
import com.otectus.immersivesmithing.registry.ModTags;
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

    private static final int MAX_WIDTH = 360;
    private static final int MAX_HEIGHT = 228;
    private static final int ROW_HEIGHT = 28;
    private static final int LIST_TOP = 46;

    private final OpenForgeScreenPacket offer;
    private final List<OpenForgeScreenPacket.Entry> filtered = new ArrayList<>();
    private Stage stage = Stage.SELECT;
    private EditBox search;
    private Button doneButton;
    private int scroll;
    private int selectedIndex;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int listRows;
    private boolean draggingScrollbar;

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
        super(Component.translatable("screen.immersive_smithing.forge"));
        this.offer = offer;
    }

    public int sessionId() {
        return offer.sessionId();
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, width - 20);
        panelHeight = Math.min(MAX_HEIGHT, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        listRows = Math.max(3, (panelHeight - LIST_TOP - 32) / ROW_HEIGHT);
        String previous = search != null ? search.getValue() : "";
        search = new EditBox(font, left + 60, top + 27, panelWidth - 76, 12, Component.translatable("screen.immersive_smithing.forge.search"));
        search.setBordered(false);
        search.setTextColor(SmithingGui.TEXT);
        search.setTextColorUneditable(SmithingGui.TEXT_DIM);
        search.setHint(Component.translatable("screen.immersive_smithing.forge.search_hint").withStyle(ChatFormatting.GRAY));
        search.setValue(previous);
        search.setResponder(s -> refilter());
        search.visible = stage == Stage.SELECT;
        addRenderableWidget(search);
        doneButton = SmithingGui.button(left + panelWidth / 2 - 45, top + panelHeight - 27, 90, 20,
                Component.translatable("gui.done"), b -> onClose());
        doneButton.visible = stage == Stage.RESULT;
        addRenderableWidget(doneButton);
        refilter();
        if (stage == Stage.SELECT) setFocused(search);
    }

    private void refilter() {
        filtered.clear();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        for (OpenForgeScreenPacket.Entry e : offer.entries()) {
            if (query.isEmpty() || e.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) filtered.add(e);
        }
        scroll = Mth.clamp(scroll, 0, maxScroll());
        selectedIndex = Mth.clamp(selectedIndex, 0, Math.max(0, filtered.size() - 1));
    }

    private int maxScroll() {
        return Math.max(0, filtered.size() - listRows);
    }

    private void revealSelection() {
        if (selectedIndex < scroll) scroll = selectedIndex;
        if (selectedIndex >= scroll + listRows) scroll = selectedIndex - listRows + 1;
        scroll = Mth.clamp(scroll, 0, maxScroll());
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
            if (overScrollbar(mouseX, mouseY) && maxScroll() > 0) {
                draggingScrollbar = true;
                scrollFromMouse(mouseY);
                return true;
            }
            int index = rowAt(mouseX, mouseY);
            if (index >= 0) {
                selectedIndex = index;
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
        if (stage == Stage.SELECT && !filtered.isEmpty()) {
            if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_PAGE_DOWN || key == GLFW.GLFW_KEY_PAGE_UP) {
                int amount = key == GLFW.GLFW_KEY_DOWN ? 1 : key == GLFW.GLFW_KEY_UP ? -1 : key == GLFW.GLFW_KEY_PAGE_DOWN ? listRows : -listRows;
                selectedIndex = Mth.clamp(selectedIndex + amount, 0, filtered.size() - 1);
                revealSelection();
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER) {
                select(filtered.get(selectedIndex));
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (stage == Stage.SELECT) {
            scroll = Mth.clamp(scroll - (int) Math.signum(delta), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (stage == Stage.SELECT && button == 0 && draggingScrollbar) {
            scrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
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
        return x >= left && x < left + panelWidth && y >= top && y < top + panelHeight;
    }

    private int rowAt(double x, double y) {
        int rx = left + 10;
        int ry = top + LIST_TOP;
        if (x < rx || x >= rx + panelWidth - 20 || y < ry || y >= ry + listRows * ROW_HEIGHT) return -1;
        int index = scroll + (int) ((y - ry) / ROW_HEIGHT);
        return index < filtered.size() ? index : -1;
    }

    private boolean overScrollbar(double x, double y) {
        int sx = left + panelWidth - 16;
        int sy = top + LIST_TOP;
        return x >= sx && x < sx + 6 && y >= sy && y < sy + listRows * ROW_HEIGHT;
    }

    private void scrollFromMouse(double mouseY) {
        int trackTop = top + LIST_TOP;
        int trackHeight = listRows * ROW_HEIGHT;
        float ratio = (float) ((mouseY - trackTop) / trackHeight);
        scroll = Mth.clamp(Math.round(ratio * maxScroll()), 0, maxScroll());
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
        SmithingGui.panel(g, left, top, panelWidth, panelHeight);
        Component title = Component.translatable("screen.immersive_smithing.forge.title", offer.familyName());
        Component units = MaterialUnits.describe(offer.moltenUnits());
        int titleWidth = panelWidth - 34 - font.width(units);
        g.drawString(font, SmithingGui.clipped(font, title, titleWidth), left + 12, top + 9, SmithingGui.TEXT_WARM, false);
        g.drawString(font, units, left + panelWidth - 12 - font.width(units), top + 9, SmithingGui.TEXT_DIM, false);
        SmithingGui.rule(g, left + 10, top + 21, panelWidth - 20);

        switch (stage) {
            case SELECT -> renderSelection(g, mouseX, mouseY);
            case WAITING -> renderWaiting(g);
            case READY -> renderReady(g);
            case PLAYING -> renderPlaying(g);
            case RESULT -> renderResult(g);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (stage == Stage.SELECT) renderRowTooltip(g, mouseX, mouseY);
        renderHeaderTooltip(g, mouseX, mouseY);
    }

    private void renderSelection(GuiGraphics g, int mouseX, int mouseY) {
        int x = left + 10;
        int y = top + LIST_TOP;
        int w = panelWidth - 20;
        SmithingGui.well(g, left + 11, top + 24, panelWidth - 22, 18);
        g.drawString(font, Component.translatable("screen.immersive_smithing.forge.search_label"), left + 17, top + 29, SmithingGui.TEXT_DIM, false);
        SmithingGui.well(g, x - 2, y - 2, w + 4, listRows * ROW_HEIGHT + 4);
        if (filtered.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.immersive_smithing.forge.no_match"), left + panelWidth / 2, y + 40, SmithingGui.TEXT_DIM);
        }
        g.enableScissor(x, y, x + w - 7, y + listRows * ROW_HEIGHT);
        for (int row = 0; row < listRows; row++) {
            int index = scroll + row;
            if (index >= filtered.size()) break;
            OpenForgeScreenPacket.Entry e = filtered.get(index);
            int ry = y + row * ROW_HEIGHT;
            boolean hovered = rowAt(mouseX, mouseY) == index;
            if (index == selectedIndex) g.fill(x, ry, x + w - 7, ry + ROW_HEIGHT, SmithingGui.ROW_FOCUSED);
            if (hovered) g.fill(x, ry, x + w - 7, ry + ROW_HEIGHT, SmithingGui.ROW_HOVER);
            if (index == selectedIndex) g.fill(x, ry, x + 2, ry + ROW_HEIGHT, SmithingGui.BRONZE);
            g.renderItem(e.result(), x + 5, ry + 6);
            int shownAux = Math.min(4, e.auxiliary().size());
            int auxSpace = shownAux * 18 + (e.auxiliary().size() > shownAux ? 17 : 0);
            int textWidth = Math.max(48, w - 38 - auxSpace);
            g.drawString(font, SmithingGui.clipped(font, e.result().getHoverName(), textWidth), x + 27, ry + 4, SmithingGui.TEXT, false);
            Component cost = Component.translatable("screen.immersive_smithing.forge.cost", MaterialUnits.describe(e.metalUnits()),
                    MaterialUnits.describe(offer.moltenUnits() - e.metalUnits()));
            g.drawString(font, SmithingGui.clipped(font, cost, textWidth), x + 27, ry + 15, SmithingGui.TEXT_DIM, false);
            int ax = x + w - 27;
            for (int i = 0; i < shownAux; i++) {
                ItemStack aux = e.auxiliary().get(i);
                g.renderItem(aux, ax, ry + 6);
                g.renderItemDecorations(font, aux, ax, ry + 6);
                ax -= 18;
            }
            if (e.auxiliary().size() > shownAux) {
                Component more = Component.translatable("screen.immersive_smithing.forge.more_components", e.auxiliary().size() - shownAux);
                g.drawString(font, more, ax + 1, ry + 10, SmithingGui.TEXT_DIM, false);
            }
            if (row > 0) g.fill(x + 4, ry, x + w - 11, ry + 1, 0x3049382E);
        }
        g.disableScissor();
        // scrollbar
        if (filtered.size() > listRows) {
            int trackH = listRows * ROW_HEIGHT;
            int barH = Math.max(12, trackH * listRows / filtered.size());
            int barY = y + (trackH - barH) * scroll / Math.max(1, filtered.size() - listRows);
            g.fill(x + w - 4, y, x + w - 1, y + trackH, 0x40000000);
            g.fill(x + w - 4, barY, x + w - 1, barY + barH, SmithingGui.BRONZE);
        }
        Component hint = Component.translatable("screen.immersive_smithing.forge.choose", offer.timeMs() / 1000);
        g.drawCenteredString(font, SmithingGui.clipped(font, hint, panelWidth - 24), left + panelWidth / 2, top + panelHeight - 17, SmithingGui.TEXT_DIM);
    }

    private void renderRowTooltip(GuiGraphics g, int mouseX, int mouseY) {
        int index = rowAt(mouseX, mouseY);
        if (index < 0) return;
        OpenForgeScreenPacket.Entry e = filtered.get(index);
        int x = left + 10;
        int w = panelWidth - 20;
        if (mouseX < x + 24) {
            g.renderTooltip(font, e.result(), mouseX, mouseY);
            return;
        }
        int ax = x + w - 26;
        for (int i = 0; i < Math.min(4, e.auxiliary().size()); i++) {
            ItemStack aux = e.auxiliary().get(i);
            if (mouseX >= ax && mouseX < ax + 16) {
                g.renderTooltip(font, aux, mouseX, mouseY);
                return;
            }
            ax -= 18;
        }
        List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
        int tooltipWidth = Math.min(260, width - 24);
        lines.addAll(font.split(e.result().getHoverName(), tooltipWidth));
        Component cost = Component.translatable("screen.immersive_smithing.forge.cost", MaterialUnits.describe(e.metalUnits()),
                MaterialUnits.describe(offer.moltenUnits() - e.metalUnits())).withStyle(ChatFormatting.GRAY);
        lines.addAll(font.split(cost, tooltipWidth));
        if (!e.auxiliary().isEmpty()) {
            lines.addAll(font.split(Component.translatable("screen.immersive_smithing.forge.components").withStyle(ChatFormatting.GOLD), tooltipWidth));
            for (ItemStack auxiliary : e.auxiliary()) {
                lines.addAll(font.split(Component.translatable("screen.immersive_smithing.forge.component_line",
                        auxiliary.getCount(), auxiliary.getHoverName()).withStyle(ChatFormatting.GRAY), tooltipWidth));
            }
        }
        g.renderTooltip(font, lines, mouseX, mouseY);
    }

    private void renderHeaderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        Component heading = Component.translatable("screen.immersive_smithing.forge.title", offer.familyName());
        Component units = MaterialUnits.describe(offer.moltenUnits());
        int available = panelWidth - 34 - font.width(units);
        if (font.width(heading) > available && mouseX >= left + 10 && mouseX < left + 12 + available
                && mouseY >= top + 6 && mouseY < top + 21) {
            g.renderTooltip(font, heading, mouseX, mouseY);
        }
        if (stage == Stage.READY && font.width(selected.getHoverName()) > panelWidth - 30
                && mouseX >= left + 15 && mouseX < left + panelWidth - 15 && mouseY >= top + 105 && mouseY < top + 122) {
            g.renderTooltip(font, selected.getHoverName(), mouseX, mouseY);
        }
    }

    private void renderWaiting(GuiGraphics g) {
        int cx = left + panelWidth / 2;
        renderWorkpiece(g, cx, top + panelHeight / 2 - 28, 3F);
        g.drawCenteredString(font, Component.translatable("screen.immersive_smithing.preparing"), cx, top + panelHeight / 2 + 10, SmithingGui.TEXT_WARM);
        SmithingGui.drawWrappedCentered(g, font, Component.translatable("screen.immersive_smithing.forge.preparing_detail"),
                cx, top + panelHeight / 2 + 24, panelWidth - 40, SmithingGui.TEXT_DIM);
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
        int cx = left + panelWidth / 2;
        renderWorkpiece(g, cx, top + 78, 3F);
        g.drawCenteredString(font, SmithingGui.clipped(font, selected.getHoverName(), panelWidth - 30), cx, top + 110, SmithingGui.TEXT);
        Component ready = Component.translatable("screen.immersive_smithing.ready", Math.max(1, (remaining + 999) / 1000));
        g.drawCenteredString(font, ready, cx, top + 132, SmithingGui.TEXT_WARM);
        SmithingGui.drawWrappedCentered(g, font, Component.translatable("screen.immersive_smithing.forge.instructions"),
                cx, top + 150, panelWidth - 32, SmithingGui.TEXT_DIM);
        SmithingGui.drawWrappedCentered(g, font, Component.translatable("screen.immersive_smithing.forge.durability_explainer"),
                cx, top + 176, panelWidth - 32, SmithingGui.TEXT_DIM);
    }

    private void renderPlaying(GuiGraphics g) {
        int now = elapsed();
        float remaining = 1F - now / (float) Math.max(1, allowedMs);
        SmithingGui.timeBar(g, left + 14, top + 29, panelWidth - 28, 6, remaining);
        Component time = Component.translatable("screen.immersive_smithing.time_remaining", Math.max(0, (allowedMs - now + 999) / 1000));
        g.drawString(font, time, left + panelWidth - 14 - font.width(time), top + 39, SmithingGui.TEXT_DIM, false);
        g.drawString(font, Component.translatable("screen.immersive_smithing.forge.step"), left + 14, top + 39, SmithingGui.TEXT_WARM, false);

        SmithingGui.well(g, left + 18, top + 54, 56, 50);
        renderWorkpiece(g, left + 46, top + 77, 3F);
        int phaseCount = run.phases().size();
        int shown = Math.min(run.index() + 1, phaseCount);
        int infoX = left + 86;
        g.drawString(font, Component.translatable("screen.immersive_smithing.forge.phase", shown, phaseCount), infoX, top + 54, SmithingGui.TEXT, false);
        int cell = Math.min(24, (panelWidth - 102) / Math.max(1, phaseCount));
        for (int i = 0; i < phaseCount; i++) {
            int px = infoX + i * cell;
            int color = i < phaseResults.size() ? SmithingGui.ratingColor(phaseResults.get(i)) : 0xFF4A4038;
            g.fill(px, top + 68, px + cell - 3, top + 79, SmithingGui.SOOT_DEEP);
            g.fill(px, top + 79, px + cell - 3, top + 81, color);
            if (cell >= 10) g.drawCenteredString(font, Integer.toString(i + 1), px + (cell - 3) / 2, top + 69,
                    i == run.index() ? SmithingGui.TEXT_WARM : SmithingGui.TEXT_DIM);
            if (i == run.index()) g.renderOutline(px - 1, top + 67, cell - 1, 15, SmithingGui.BRONZE);
        }
        if (!phaseResults.isEmpty()) {
            float sum = 0;
            for (float f : phaseResults) sum += f;
            int projected = Math.round(sum / phaseCount * 100F);
            Component projectedQuality = Component.translatable("screen.immersive_smithing.projected", SmithingQuality.fromScore(projected).displayName());
            List<net.minecraft.util.FormattedCharSequence> projectedLines = font.split(projectedQuality, panelWidth - 100);
            for (int i = 0; i < projectedLines.size(); i++) {
                g.drawString(font, projectedLines.get(i), infoX, top + 89 + i * 10, SmithingGui.TEXT_DIM, false);
            }
        }

        // Track
        float scale = SmithingGui.markerScale();
        int trackH = Math.round(16 * scale);
        int trackWidth = panelWidth - 44;
        int tx = left + 22;
        int ty = top + 132 - trackH / 2;
        SmithingGui.well(g, tx - 2, ty - 2, trackWidth + 4, trackH + 4);
        g.fillGradient(tx, ty, tx + trackWidth, ty + trackH, 0xFF303538, 0xFF171A1C);
        for (int i = 0; i <= 12; i++) {
            int tickX = tx + i * (trackWidth - 1) / 12;
            g.fill(tickX, ty + trackH - (i % 3 == 0 ? 5 : 3), tickX + 1, ty + trackH, 0xFF6B706B);
        }
        if (run.isComplete() || now > allowedMs) {
            g.drawCenteredString(font, Component.translatable(now > allowedMs ? "screen.immersive_smithing.times_up" : "screen.immersive_smithing.finishing"),
                    left + panelWidth / 2, ty + trackH / 2 - 4, SmithingGui.TEXT_DIM);
        } else {
            ForgeMinigame.Phase phase = run.phases().get(run.index());
            int zoneL = tx + Math.round((phase.center() - phase.halfWidth()) * trackWidth);
            int zoneR = tx + Math.round((phase.center() + phase.halfWidth()) * trackWidth);
            int perfL = tx + Math.round((phase.center() - phase.halfWidth() * 0.2F) * trackWidth);
            int perfR = tx + Math.round((phase.center() + phase.halfWidth() * 0.2F) * trackWidth);
            if (ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES)) g.fill(zoneL - 1, ty - 1, zoneR + 1, ty + trackH + 1, 0xFF000000);
            g.fill(zoneL, ty, zoneR, ty + trackH, SmithingGui.zoneColor());
            g.fill(perfL, ty, Math.max(perfR, perfL + 1), ty + trackH, SmithingGui.perfectColor());
            // Brackets and a centre notch make the timing zone readable without relying on hue.
            g.fill(zoneL, ty - 3, zoneR, ty - 2, SmithingGui.TEXT_WARM);
            g.fill(zoneL, ty - 3, zoneL + 1, ty, SmithingGui.TEXT_WARM);
            g.fill(zoneR - 1, ty - 3, zoneR, ty, SmithingGui.TEXT_WARM);
            int centre = tx + Math.round(phase.center() * trackWidth);
            g.fill(centre - 2, ty + trackH + 3, centre + 3, ty + trackH + 5, SmithingGui.perfectColor());

            int local = now - run.phaseStartMs();
            if (local >= 0) {
                float position = ForgeMinigame.position(phase, local / 1000F);
                int mx = tx + Math.round(position * trackWidth);
                int half = Math.max(1, Math.round(1.5F * scale));
                g.fill(mx - half - 1, ty - 5, mx + half + 1, ty + trackH + 5,
                        ClientConfig.get(ClientConfig.HIGH_CONTRAST_MINIGAMES) ? 0xFFFFFFFF : 0xFF090D10);
                g.fill(mx - half, ty - 4, mx + half, ty + trackH + 4, SmithingGui.markerColor());
                g.fill(mx - half - 2, ty - 6, mx + half + 2, ty - 4, SmithingGui.markerColor());
                boolean inZone = Math.abs(position - phase.center()) <= phase.halfWidth();
                if (inZone && !markerInZone) SmithingGui.playCue();
                markerInZone = inZone;
            } else {
                markerInZone = false;
            }
        }

        SmithingGui.well(g, left + 22, top + 164, panelWidth - 44, 23);
        boolean feedback = lastAccuracy >= 0 && Util.getMillis() - lastFeedbackMs < 900;
        g.drawCenteredString(font, feedback ? SmithingGui.rating(lastAccuracy)
                        : Component.translatable("screen.immersive_smithing.forge.target_label"),
                left + panelWidth / 2, top + 172, feedback ? SmithingGui.ratingColor(lastAccuracy) : SmithingGui.TEXT);
        SmithingGui.drawWrappedCentered(g, font, Component.translatable("screen.immersive_smithing.forge.instructions"),
                left + panelWidth / 2, top + panelHeight - 27, panelWidth - 32, SmithingGui.TEXT_DIM);
    }

    private void renderResult(GuiGraphics g) {
        int cx = left + panelWidth / 2;
        renderWorkpiece(g, cx, top + 62, 3F);
        Component headline = Component.translatable(resultTimedOut ? "screen.immersive_smithing.forge.timeout" : "screen.immersive_smithing.forge.complete");
        g.drawCenteredString(font, headline, cx, top + 94, resultTimedOut ? SmithingGui.missColor() : SmithingGui.TEXT_WARM);
        Component quality = SmithingQuality.fromScore(resultScore).displayName();
        if (ClientConfig.get(ClientConfig.SHOW_NUMERIC_QUALITY_SCORES)) quality = Component.translatable("screen.immersive_smithing.quality_with_score", quality, resultScore);
        int qualityLines = SmithingGui.drawWrappedCentered(g, font, Component.translatable("screen.immersive_smithing.forge.durability", quality),
                cx, top + 108, panelWidth - 36, SmithingGui.TEXT);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("screen.immersive_smithing.forge.next"), panelWidth - 40);
        for (int i = 0; i < lines.size(); i++) {
            g.drawCenteredString(font, lines.get(i), cx, top + 126 + qualityLines * 10 + i * 10, SmithingGui.TEXT_DIM);
        }
    }
}
