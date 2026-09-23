package com.otectus.otessmithing.client.screen;

import com.otectus.otessmithing.OtesSmithing;
import com.otectus.otessmithing.guide.GuideData;
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
    private static final ResourceLocation TEXTURE = OtesSmithing.id("textures/gui/guide.png");
    private static final int WIDTH = 336;
    private static final int HEIGHT = 206;
    private static final int LIST_WIDTH = 138;
    private static final int LINE_HEIGHT = 10;
    private static final int ENTRY_HEIGHT = 11;
    private static final int INK = 0xFF3B2A1A;
    private static final int INK_DIM = 0xFF7A6040;
    private static final int INK_ACTIVE = 0xFF9A3A10;

    private static int lastChapter = 1;
    private static int lastPage;

    private int chapter;
    private int page;
    private int listScroll;
    private int left;
    private int top;
    private final List<List<FormattedCharSequence>> pages = new ArrayList<>();
    private Button prev;
    private Button next;

    public GuideScreen(int chapter) {
        super(Component.translatable("item.otes_smithing.smithing_guide"));
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
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> turn(-1))
                .bounds(left + LIST_WIDTH + 16, top + HEIGHT - 26, 20, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), b -> turn(1))
                .bounds(left + WIDTH - 36, top + HEIGHT - 26, 20, 16).build());
        paginate();
    }

    private int textWidth() {
        return WIDTH - LIST_WIDTH - 36;
    }

    private int linesPerPage() {
        return (HEIGHT - 60) / LINE_HEIGHT;
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
        return (HEIGHT - 44) / ENTRY_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int lx = left + 12;
        int ly = top + 28;
        if (button == 0 && mouseX >= lx && mouseX < lx + LIST_WIDTH - 8 && mouseY >= ly && mouseY < ly + visibleChapters() * ENTRY_HEIGHT) {
            int index = listScroll + (int) ((mouseY - ly) / ENTRY_HEIGHT) + 1;
            if (index <= GuideData.chapterCount()) {
                chapter = index;
                page = 0;
                paginate();
                remember();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < left + LIST_WIDTH) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta), 0, Math.max(0, GuideData.chapterCount() - visibleChapters()));
        } else {
            turn(delta > 0 ? -1 : 1);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
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
        g.blitNineSliced(TEXTURE, left, top, WIDTH, HEIGHT, 12, 64, 64, 0, 0);
        g.blitNineSliced(TEXTURE, left + 8, top + 8, LIST_WIDTH, HEIGHT - 16, 6, 32, 32, 64, 0);
        g.drawString(font, title, left + 14, top + 13, INK_ACTIVE, false);

        int lx = left + 14;
        int ly = top + 28;
        for (int i = 0; i < visibleChapters(); i++) {
            int index = listScroll + i + 1;
            if (index > GuideData.chapterCount()) break;
            String name = index + ". " + GuideData.chapterTitle(index).getString();
            if (font.width(name) > LIST_WIDTH - 14) name = font.plainSubstrByWidth(name, LIST_WIDTH - 20) + "...";
            boolean hovered = mouseX >= lx - 2 && mouseX < lx + LIST_WIDTH - 10 && mouseY >= ly + i * ENTRY_HEIGHT - 1 && mouseY < ly + (i + 1) * ENTRY_HEIGHT - 1;
            int color = index == chapter ? INK_ACTIVE : hovered ? INK : INK_DIM;
            g.drawString(font, name, lx, ly + i * ENTRY_HEIGHT, color, false);
        }
        int arrowX = left + 8 + LIST_WIDTH - 14;
        if (listScroll > 0) g.drawString(font, "^", arrowX, top + 16, INK_DIM, false);
        if (listScroll + visibleChapters() < GuideData.chapterCount()) g.drawString(font, "v", arrowX, top + HEIGHT - 20, INK_DIM, false);

        int tx = left + LIST_WIDTH + 20;
        int ty = top + 14;
        g.drawString(font, GuideData.chapterTitle(chapter), tx, ty, INK_ACTIVE, false);
        g.fill(tx, ty + 11, tx + textWidth(), ty + 12, 0x603B2A1A);
        List<FormattedCharSequence> lines = pages.get(page);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), tx, ty + 18 + i * LINE_HEIGHT, INK, false);
        }
        Component pageLabel = Component.translatable("screen.otes_smithing.guide.page", page + 1, pages.size());
        int centre = (left + LIST_WIDTH + 16 + left + WIDTH - 16) / 2;
        g.drawString(font, pageLabel, centre - font.width(pageLabel) / 2, top + HEIGHT - 22, INK_DIM, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
