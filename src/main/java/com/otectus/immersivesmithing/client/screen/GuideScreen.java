package com.otectus.immersivesmithing.client.screen;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.guide.GuideData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * The Smithing Guide: a chapter list on the left and paginated text on the right. All text comes from the
 * language files and the page art from {@code textures/gui/guide.png}, so resource packs can replace both.
 */
public class GuideScreen extends Screen {
    private static final ResourceLocation TEXTURE = ImmersiveSmithing.id("textures/gui/guide.png");
    private static final int MAX_WIDTH = 386;
    private static final int MAX_HEIGHT = 224;
    private static final int LINE_HEIGHT = 10;
    private static final int ENTRY_HEIGHT = 12;
    private static final int INK = 0xFF342820;
    private static final int INK_DIM = 0xFF75624E;
    private static final int INK_ACTIVE = 0xFF8A492A;

    private static int lastChapter = 1;
    private static int lastPage;

    private int chapter;
    private int page;
    private int listScroll;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();
    private Button prev;
    private Button next;

    public GuideScreen(int chapter) {
        super(Component.translatable("item.immersive_smithing.smithing_guide"));
        if (chapter > 0) {
            this.chapter = Mth.clamp(chapter, 1, GuideData.chapterCount());
            this.page = 0;
        } else {
            this.chapter = lastChapter;
            this.page = lastPage;
        }
    }

    @Override
    protected void init() {
        panelWidth = Math.min(MAX_WIDTH, width - 20);
        panelHeight = Math.min(MAX_HEIGHT, height - 16);
        listWidth = Math.min(142, Math.max(108, panelWidth * 37 / 100));
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        prev = addRenderableWidget(SmithingGui.button(left + listWidth + 16, top + panelHeight - 24, 48, 18,
                Component.translatable("screen.immersive_smithing.guide.previous"), b -> turn(-1)));
        next = addRenderableWidget(SmithingGui.button(left + panelWidth - 64, top + panelHeight - 24, 48, 18,
                Component.translatable("screen.immersive_smithing.guide.next"), b -> turn(1)));
        paginate();
        revealChapter();
    }

    private int textWidth() {
        return panelWidth - listWidth - 36;
    }

    private int linesPerPage() {
        return (panelHeight - 74) / LINE_HEIGHT;
    }

    private void paginate() {
        pages.clear();
        List<FormattedCharSequence> lines = new ArrayList<>();
        String raw = Component.translatable(GuideData.textKey(chapter)).getString();
        for (String paragraph : raw.split("\n")) {
            if (paragraph.isBlank()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(paragraph), textWidth()));
        }
        int per = linesPerPage();
        for (int i = 0; i < lines.size(); i += per) {
            pages.add(lines.subList(i, Math.min(lines.size(), i + per)));
        }
        if (pages.isEmpty()) pages.add(List.of());
        page = Mth.clamp(page, 0, pages.size() - 1);
        updateButtons();
    }

    private void turn(int delta) {
        int target = page + delta;
        if (target < 0 && chapter > 1) {
            chapter--;
            page = Integer.MAX_VALUE;
        } else if (target >= pages.size() && chapter < GuideData.chapterCount()) {
            chapter++;
            page = 0;
        } else {
            page = Mth.clamp(target, 0, pages.size() - 1);
        }
        paginate();
        revealChapter();
        remember();
    }

    private void updateButtons() {
        if (prev != null) prev.active = page > 0 || chapter > 1;
        if (next != null) next.active = page < pages.size() - 1 || chapter < GuideData.chapterCount();
    }

    private void remember() {
        lastChapter = chapter;
        lastPage = page;
    }

    private int visibleChapters() {
        return (panelHeight - 48) / ENTRY_HEIGHT;
    }

    private void revealChapter() {
        if (chapter <= listScroll) listScroll = chapter - 1;
        if (chapter > listScroll + visibleChapters()) listScroll = chapter - visibleChapters();
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, GuideData.chapterCount() - visibleChapters()));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int lx = left + 12;
        int ly = top + 28;
        if (button == 0 && mouseX >= lx && mouseX < lx + listWidth - 8 && mouseY >= ly && mouseY < ly + visibleChapters() * ENTRY_HEIGHT) {
            int index = listScroll + (int) ((mouseY - ly) / ENTRY_HEIGHT) + 1;
            if (index <= GuideData.chapterCount()) {
                chapter = index;
                page = 0;
                paginate();
                revealChapter();
                remember();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < left + listWidth) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta), 0, Math.max(0, GuideData.chapterCount() - visibleChapters()));
        } else {
            turn(delta > 0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_UP || key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN
                || key == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME || key == org.lwjgl.glfw.GLFW.GLFW_KEY_END) {
            if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_HOME) chapter = 1;
            else if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_END) chapter = GuideData.chapterCount();
            else chapter = Mth.clamp(chapter + (key == org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN ? 1 : -1), 1, GuideData.chapterCount());
            page = 0;
            paginate();
            revealChapter();
            remember();
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
            turn(-1);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT || key == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(1);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        g.blitNineSliced(TEXTURE, left, top, panelWidth, panelHeight, 12, 64, 64, 0, 0);
        g.blitNineSliced(TEXTURE, left + 8, top + 8, listWidth, panelHeight - 16, 6, 32, 32, 64, 0);
        g.drawString(font, SmithingGui.clipped(font, title, listWidth - 28), left + 14, top + 13, INK_ACTIVE, false);
        SmithingGui.rule(g, left + 12, top + 23, listWidth - 8);

        int lx = left + 14;
        int ly = top + 28;
        g.enableScissor(left + 10, ly - 2, left + listWidth + 4, ly + visibleChapters() * ENTRY_HEIGHT);
        for (int i = 0; i < visibleChapters(); i++) {
            int index = listScroll + i + 1;
            if (index > GuideData.chapterCount()) break;
            Component fullName = Component.translatable("screen.immersive_smithing.guide.chapter", index, GuideData.chapterTitle(index));
            String name = SmithingGui.clipped(font, fullName, listWidth - 22);
            boolean hovered = mouseX >= lx - 2 && mouseX < lx + listWidth - 10 && mouseY >= ly + i * ENTRY_HEIGHT - 1 && mouseY < ly + (i + 1) * ENTRY_HEIGHT - 1;
            if (index == chapter) {
                g.fill(lx - 3, ly + i * ENTRY_HEIGHT - 2, lx + listWidth - 12, ly + (i + 1) * ENTRY_HEIGHT - 1, SmithingGui.ROW_FOCUSED);
                g.fill(lx - 3, ly + i * ENTRY_HEIGHT - 2, lx - 1, ly + (i + 1) * ENTRY_HEIGHT - 1, SmithingGui.BRONZE);
            } else if (hovered) {
                g.fill(lx - 3, ly + i * ENTRY_HEIGHT - 2, lx + listWidth - 12, ly + (i + 1) * ENTRY_HEIGHT - 1, SmithingGui.ROW_HOVER);
            }
            int color = index == chapter ? INK_ACTIVE : hovered ? INK : INK_DIM;
            g.drawString(font, name, lx, ly + i * ENTRY_HEIGHT, color, false);
        }
        g.disableScissor();
        int arrowX = left + 8 + listWidth - 14;
        if (listScroll > 0) g.drawString(font, Component.translatable("screen.immersive_smithing.guide.scroll_up"), arrowX, top + 14, INK_DIM, false);
        if (listScroll + visibleChapters() < GuideData.chapterCount()) g.drawString(font, Component.translatable("screen.immersive_smithing.guide.scroll_down"), arrowX, top + panelHeight - 19, INK_DIM, false);

        int tx = left + listWidth + 20;
        int ty = top + 14;
        Component chapterTitle = GuideData.chapterTitle(chapter);
        g.drawString(font, SmithingGui.clipped(font, chapterTitle, textWidth()), tx, ty, INK_ACTIVE, false);
        SmithingGui.rule(g, tx, ty + 11, textWidth());
        List<FormattedCharSequence> lines = pages.get(page);
        g.enableScissor(tx, ty + 17, tx + textWidth(), top + panelHeight - 39);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), tx, ty + 18 + i * LINE_HEIGHT, INK, false);
        }
        g.disableScissor();
        Component chapterLabel = Component.translatable("screen.immersive_smithing.guide.chapter_count", chapter, GuideData.chapterCount());
        g.drawString(font, chapterLabel, left + 14, top + panelHeight - 20, INK_DIM, false);
        Component pageLabel = Component.translatable("screen.immersive_smithing.guide.page", page + 1, pages.size());
        int centre = (left + listWidth + 16 + left + panelWidth - 16) / 2;
        g.drawString(font, pageLabel, centre - font.width(pageLabel) / 2, top + panelHeight - 36, INK_DIM, false);
        super.render(g, mouseX, mouseY, partialTick);
        renderGuideTooltips(g, mouseX, mouseY);
    }

    private void renderGuideTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (font.width(title) > listWidth - 28 && mouseX >= left + 12 && mouseX < left + listWidth - 10
                && mouseY >= top + 9 && mouseY < top + 25) {
            g.renderTooltip(font, title, mouseX, mouseY);
            return;
        }
        Component activeTitle = GuideData.chapterTitle(chapter);
        int tx = left + listWidth + 20;
        if (font.width(activeTitle) > textWidth() && mouseX >= tx && mouseX < tx + textWidth()
                && mouseY >= top + 10 && mouseY < top + 27) {
            g.renderTooltip(font, activeTitle, mouseX, mouseY);
            return;
        }
        int lx = left + 12;
        int ly = top + 28;
        if (mouseX < lx || mouseX >= lx + listWidth - 8 || mouseY < ly || mouseY >= ly + visibleChapters() * ENTRY_HEIGHT) return;
        int index = listScroll + (int) ((mouseY - ly) / ENTRY_HEIGHT) + 1;
        if (index > GuideData.chapterCount()) return;
        Component name = Component.translatable("screen.immersive_smithing.guide.chapter", index, GuideData.chapterTitle(index));
        if (font.width(name) > listWidth - 22) g.renderTooltip(font, name, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
