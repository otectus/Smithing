package com.otectus.immersivesmithing.client.screen;

import com.otectus.immersivesmithing.ImmersiveSmithing;
import com.otectus.immersivesmithing.guide.GuideData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Paged field guide. Translated paragraphs may use a "## " prefix for a section heading. */
public class GuideScreen extends Screen {
    private static final ResourceLocation TEXTURE = ImmersiveSmithing.id("textures/gui/guide.png");
    private static final int LINE_HEIGHT = 13;
    private static final int ENTRY_HEIGHT = 22;
    private static final int INK = 0xFF302A24;
    private static final int INK_DIM = 0xFF63533E;
    private static final int ACCENT = 0xFF754025;
    private static int lastChapter = 1;
    private static int lastPage;

    private record Line(FormattedCharSequence text, boolean heading, boolean blank) {}
    private final List<List<Line>> pages = new ArrayList<>();
    private final List<Integer> matches = new ArrayList<>();
    private final List<Button> chapterButtons = new ArrayList<>();
    private int chapter;
    private int page;
    private int listScroll;
    private int left, top, panelWidth, panelHeight;
    private int textX, textWidth, bodyY;
    private boolean compact;
    private boolean contentsOpen;
    private String query = "";
    private EditBox search;
    private Button prev, next;

    public GuideScreen(int chapter) {
        super(Component.translatable("item.immersive_smithing.smithing_guide"));
        this.chapter = chapter > 0 ? Mth.clamp(chapter, 1, GuideData.chapterCount()) : lastChapter;
        this.page = chapter > 0 ? 0 : lastPage;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(520, width - 16);
        panelHeight = Math.min(324, height - 16);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        compact = panelWidth < 420;
        textX = left + (compact ? 20 : 178);
        textWidth = left + panelWidth - 20 - textX;
        bodyY = top + 48 + font.split(GuideData.chapterTitle(chapter).copy().withStyle(net.minecraft.ChatFormatting.BOLD), textWidth).size() * LINE_HEIGHT + 12;
        if (compact) {
            addRenderableWidget(SmithingGui.button(left + 14, top + 12, 78, 20,
                    Component.translatable(contentsOpen ? "screen.immersive_smithing.guide.read" : "screen.immersive_smithing.guide.contents"), b -> {
                        contentsOpen = !contentsOpen;
                        rebuildWidgets();
                    }));
        }
        addRenderableWidget(SmithingGui.button(left + panelWidth - 62, top + 12, 48, 20,
                Component.translatable("gui.done"), b -> onClose()));
        search = new EditBox(font, left + 20, top + 51, indexWidth() - 16, 18,
                Component.translatable("screen.immersive_smithing.guide.search"));
        search.setMaxLength(80);
        search.setValue(query);
        search.setTextColor(0xFFF0E4C8);
        search.setHint(Component.translatable("screen.immersive_smithing.guide.search"));
        search.setResponder(value -> {
            query = value;
            listScroll = 0;
            filterChapters();
            rebuildChapters();
        });
        search.visible = indexVisible();
        addRenderableWidget(search);
        prev = addRenderableWidget(SmithingGui.button(textX, top + panelHeight - 36, 62, 20,
                Component.translatable("screen.immersive_smithing.guide.previous"), b -> turn(-1)));
        next = addRenderableWidget(SmithingGui.button(left + panelWidth - 82, top + panelHeight - 36, 62, 20,
                Component.translatable("screen.immersive_smithing.guide.next"), b -> turn(1)));
        prev.setTooltip(Tooltip.create(Component.translatable("screen.immersive_smithing.guide.page_keys")));
        next.setTooltip(Tooltip.create(Component.translatable("screen.immersive_smithing.guide.page_keys")));
        paginate();
        filterChapters();
        revealChapter();
        rebuildChapters();
    }

    private boolean indexVisible() { return !compact || contentsOpen; }
    private int indexWidth() { return compact ? panelWidth - 40 : 142; }
    private int visibleChapters() { return Math.max(1, (panelHeight - 116) / ENTRY_HEIGHT); }

    private void filterChapters() {
        matches.clear();
        String needle = query.strip().toLowerCase(Locale.ROOT);
        for (int i = 1; i <= GuideData.chapterCount(); i++) {
            String haystack = GuideData.chapterTitle(i).getString() + " " + Component.translatable(GuideData.textKey(i)).getString();
            if (haystack.toLowerCase(Locale.ROOT).contains(needle)) matches.add(i);
        }
    }

    private void revealChapter() {
        int index = matches.indexOf(chapter);
        if (index >= 0 && index < listScroll) listScroll = index;
        if (index >= listScroll + visibleChapters()) listScroll = index - visibleChapters() + 1;
        listScroll = Mth.clamp(listScroll, 0, Math.max(0, matches.size() - visibleChapters()));
    }

    private void rebuildChapters() {
        chapterButtons.forEach(this::removeWidget);
        chapterButtons.clear();
        if (!indexVisible()) return;
        for (int row = 0; row < visibleChapters() && row + listScroll < matches.size(); row++) {
            int index = matches.get(row + listScroll);
            Component label = Component.translatable("screen.immersive_smithing.guide.chapter", index, GuideData.chapterTitle(index));
            Button button = new Button(left + 18, top + 78 + row * ENTRY_HEIGHT, indexWidth() - 8, ENTRY_HEIGHT - 2,
                    label, b -> selectChapter(index), narration -> narration.get()) {
                @Override
                protected void renderWidget(GuiGraphics g, int mx, int my, float partialTick) {
                    boolean selected = index == chapter;
                    if (selected || isHoveredOrFocused()) g.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(),
                            selected ? 0xFFD0B78A : 0xFFEADDBe);
                    if (selected) g.fill(getX(), getY() + 2, getX() + 2, getY() + getHeight() - 2, ACCENT);
                    if (isFocused()) g.renderOutline(getX(), getY(), getWidth(), getHeight(), ACCENT);
                    g.drawString(font, SmithingGui.clipped(font, getMessage(), getWidth() - 12), getX() + 6, getY() + 6,
                            selected ? ACCENT : INK, false);
                }
            };
            button.setTooltip(Tooltip.create(label));
            chapterButtons.add(addRenderableWidget(button));
        }
    }

    private void selectChapter(int index) {
        chapter = index;
        page = 0;
        contentsOpen = false;
        remember();
        rebuildWidgets();
    }

    private void paginate() {
        pages.clear();
        int per = Math.max(3, (top + panelHeight - 57 - bodyY) / LINE_HEIGHT);
        List<Line> current = new ArrayList<>();
        String raw = Component.translatable(GuideData.textKey(chapter)).getString();
        for (String paragraph : raw.split("\n")) {
            boolean heading = paragraph.startsWith("## ");
            boolean blank = paragraph.isBlank();
            Component text = Component.literal(heading ? paragraph.substring(3) : paragraph);
            if (heading) text = text.copy().withStyle(net.minecraft.ChatFormatting.BOLD);
            List<FormattedCharSequence> wrapped = blank ? List.of(FormattedCharSequence.EMPTY) : font.split(text, textWidth);
            // Keep short paragraphs together, and never orphan a heading at the bottom of a page.
            int needed = wrapped.size() + (heading ? 2 : 0);
            if (!current.isEmpty() && needed <= per && current.size() + needed > per) {
                // A paragraph may be moved as a whole; carry its heading with it as well.
                int carryFrom = current.size();
                while (carryFrom > 0 && current.get(carryFrom - 1).blank()) carryFrom--;
                int headingEnd = carryFrom;
                while (carryFrom > 0 && current.get(carryFrom - 1).heading()) carryFrom--;
                List<Line> carry = carryFrom < headingEnd ? new ArrayList<>(current.subList(carryFrom, current.size())) : new ArrayList<>();
                if (!carry.isEmpty()) current.subList(carryFrom, current.size()).clear();
                addPage(current);
                current = carry;
            }
            for (FormattedCharSequence line : wrapped) {
                if (current.size() == per) {
                    addPage(current);
                    current = new ArrayList<>();
                }
                if (blank && current.isEmpty()) continue;
                current.add(new Line(line, heading, blank));
            }
        }
        if (!current.isEmpty()) addPage(current);
        if (pages.isEmpty()) pages.add(List.of());
        page = Mth.clamp(page, 0, pages.size() - 1);
        prev.visible = next.visible = !compact || !contentsOpen;
        prev.active = page > 0 || chapter > 1;
        next.active = page < pages.size() - 1 || chapter < GuideData.chapterCount();
    }

    private void addPage(List<Line> lines) {
        while (!lines.isEmpty() && lines.get(lines.size() - 1).blank()) lines.remove(lines.size() - 1);
        if (!lines.isEmpty()) pages.add(List.copyOf(lines));
    }

    private void turn(int delta) {
        int target = page + delta;
        if (target < 0 && chapter > 1) {
            chapter--;
            page = Integer.MAX_VALUE;
        } else if (target >= pages.size() && chapter < GuideData.chapterCount()) {
            chapter++;
            page = 0;
        } else page = Mth.clamp(target, 0, pages.size() - 1);
        rebuildWidgets();
        remember();
    }

    private void remember() { lastChapter = chapter; lastPage = page; }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (delta == 0 || x < left || x >= left + panelWidth || y < top + 40 || y >= top + panelHeight - 12) return false;
        if (indexVisible() && x < left + 18 + indexWidth()) {
            listScroll = Mth.clamp(listScroll - (int) Math.signum(delta), 0, Math.max(0, matches.size() - visibleChapters()));
            rebuildChapters();
        } else if (!compact || !contentsOpen) turn(delta > 0 ? -1 : 1);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (search.isFocused()) return super.keyPressed(key, scanCode, modifiers);
        if (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            if (!compact || !contentsOpen) turn(key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            selectChapter(Mth.clamp(chapter + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), 1, GuideData.chapterCount()));
            return true;
        }
        if (key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END) {
            selectChapter(key == GLFW.GLFW_KEY_HOME ? 1 : GuideData.chapterCount());
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void tick() { search.tick(); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        renderBackground(g);
        g.blitNineSliced(TEXTURE, left, top, panelWidth, panelHeight, 12, 64, 64, 0, 0);
        if (!compact) g.drawString(font, title, left + 20, top + 18, ACCENT, false);
        else {
            Component label = Component.translatable("screen.immersive_smithing.guide.chapter_count", chapter, GuideData.chapterCount());
            g.drawString(font, label, left + 102, top + 18, INK_DIM, false);
        }
        g.fill(left + 16, top + 39, left + panelWidth - 16, top + 40, 0xFFBCA780);
        if (indexVisible()) {
            g.blitNineSliced(TEXTURE, left + 12, top + 44, indexWidth() + 4, panelHeight - 58, 6, 32, 32, 64, 0);
            if (matches.isEmpty()) g.drawWordWrap(font, Component.translatable("screen.immersive_smithing.guide.no_results"),
                    left + 24, top + 83, indexWidth() - 20, INK);
            if (matches.size() > visibleChapters()) {
                int h = visibleChapters() * ENTRY_HEIGHT;
                int thumb = Math.max(12, h * visibleChapters() / matches.size());
                int y = top + 78 + (h - thumb) * listScroll / (matches.size() - visibleChapters());
                g.fill(left + indexWidth() + 8, top + 78, left + indexWidth() + 10, top + 78 + h, 0xFFBCA780);
                g.fill(left + indexWidth() + 8, y, left + indexWidth() + 10, y + thumb, ACCENT);
            }
            Component count = Component.translatable("screen.immersive_smithing.guide.topics", matches.size());
            g.drawString(font, count, left + 24, top + panelHeight - 29, INK_DIM, false);
        }
        if (!compact || !contentsOpen) {
            int y = top + 48;
            for (FormattedCharSequence line : font.split(GuideData.chapterTitle(chapter).copy().withStyle(net.minecraft.ChatFormatting.BOLD), textWidth)) {
                g.drawString(font, line, textX, y, ACCENT, false);
                y += LINE_HEIGHT;
            }
            g.fill(textX, bodyY - 7, textX + textWidth, bodyY - 6, 0xFFBCA780);
            List<Line> lines = pages.get(page);
            for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i).text(), textX, bodyY + i * LINE_HEIGHT,
                    lines.get(i).heading() ? ACCENT : INK, false);
            int progressY = top + panelHeight - 48;
            g.fill(textX, progressY, textX + textWidth, progressY + 2, 0xFFD9C8A5);
            g.fill(textX, progressY, textX + textWidth * (page + 1) / pages.size(), progressY + 2, 0xFFA57C4B);
            Component label = Component.translatable("screen.immersive_smithing.guide.page", page + 1, pages.size());
            g.drawString(font, label, textX + (textWidth - font.width(label)) / 2, top + panelHeight - 30, INK_DIM, false);
        }
        super.render(g, mx, my, partialTick);
    }

    @Override
    public void removed() { remember(); super.removed(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
