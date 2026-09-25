package com.otectus.immersivesmithing.client.screen;

import com.otectus.immersivesmithing.config.ServerConfig;
import com.otectus.immersivesmithing.network.ModNetwork;
import com.otectus.immersivesmithing.network.packet.SignWorkPacket;
import com.otectus.immersivesmithing.quality.MakersMark;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The Maker's Mark: a title and a short inscription for a piece the player just forged (or brought back to the
 * anvil). Purely an input form; the server validates the slot, the smith and every string before writing.
 */
public class MakersMarkScreen extends Screen {
    private static final int WIDTH = 228;
    private static final int LINE_HEIGHT = 18;

    private final int slot;
    private final ItemStack piece;
    private String titleText;
    private final List<String> lineText = new ArrayList<>();

    private EditBox titleBox;
    private final List<EditBox> lineBoxes = new ArrayList<>();
    private int left;
    private int top;
    private int panelHeight;
    private int maxLines;

    public MakersMarkScreen(int slot, ItemStack piece) {
        super(Component.translatable("screen.immersive_smithing.makers_mark.title"));
        this.slot = slot;
        this.piece = piece;
        this.titleText = MakersMark.title(piece);
        this.lineText.addAll(MakersMark.inscription(piece));
    }

    @Override
    protected void init() {
        lineBoxes.clear();
        maxLines = Mth.clamp(ServerConfig.get(ServerConfig.MAX_INSCRIPTION_LINES), 0, 4);
        panelHeight = 112 + (maxLines > 0 ? 14 + maxLines * LINE_HEIGHT : 0);
        left = (width - WIDTH) / 2;
        top = (height - panelHeight) / 2;

        titleBox = new EditBox(font, left + 14, top + 50, WIDTH - 28, 14, Component.translatable("screen.immersive_smithing.makers_mark.name"));
        titleBox.setMaxLength(ServerConfig.get(ServerConfig.MAX_TITLE_LENGTH));
        titleBox.setTextColor(SmithingGui.TEXT);
        titleBox.setHint(Component.translatable("screen.immersive_smithing.makers_mark.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        titleBox.setValue(titleText);
        titleBox.setResponder(s -> titleText = s);
        addRenderableWidget(titleBox);

        int y = top + 84;
        for (int i = 0; i < maxLines; i++) {
            final int index = i;
            EditBox box = new EditBox(font, left + 14, y + i * LINE_HEIGHT, WIDTH - 28, 14,
                    Component.translatable("screen.immersive_smithing.makers_mark.line_hint", i + 1));
            box.setMaxLength(ServerConfig.get(ServerConfig.MAX_INSCRIPTION_LINE_LENGTH));
            box.setTextColor(SmithingGui.TEXT_DIM);
            box.setHint(Component.translatable("screen.immersive_smithing.makers_mark.line_hint", i + 1).withStyle(ChatFormatting.DARK_GRAY));
            box.setValue(index < lineText.size() ? lineText.get(index) : "");
            box.setResponder(s -> {
                while (lineText.size() <= index) lineText.add("");
                lineText.set(index, s);
            });
            addRenderableWidget(box);
            lineBoxes.add(box);
        }

        int buttonY = top + panelHeight - 28;
        addRenderableWidget(SmithingGui.button(left + 14, buttonY, 96, 20,
                Component.translatable("screen.immersive_smithing.makers_mark.sign"), b -> sign()));
        addRenderableWidget(SmithingGui.button(left + WIDTH - 14 - 96, buttonY, 96, 20,
                Component.translatable("screen.immersive_smithing.makers_mark.skip"), b -> onClose()));
        setInitialFocus(titleBox);
    }

    private void sign() {
        List<String> lines = new ArrayList<>();
        for (EditBox box : lineBoxes) lines.add(box.getValue());
        ModNetwork.CHANNEL.sendToServer(new SignWorkPacket(slot, titleBox.getValue(), lines));
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            sign();
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        SmithingGui.panel(g, left, top, WIDTH, panelHeight);
        g.drawCenteredString(font, title, left + WIDTH / 2, top + 10, SmithingGui.TEXT_WARM);
        String smith = minecraft != null && minecraft.player != null ? minecraft.player.getGameProfile().getName() : "";
        g.drawCenteredString(font, Component.translatable("screen.immersive_smithing.makers_mark.subtitle", smith), left + WIDTH / 2, top + 22, SmithingGui.TEXT_DIM);
        SmithingGui.rule(g, left + 14, top + 33, WIDTH - 28);

        g.renderItem(piece, left + 14, top + 36 - 2);
        g.drawString(font, Component.translatable("screen.immersive_smithing.makers_mark.name"), left + 36, top + 40, SmithingGui.TEXT, false);
        if (maxLines > 0) {
            g.drawString(font, Component.translatable("screen.immersive_smithing.makers_mark.inscription"), left + 14, top + 72, SmithingGui.TEXT, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (mouseX >= left + 14 && mouseX < left + 30 && mouseY >= top + 34 && mouseY < top + 50) {
            g.renderTooltip(font, piece, mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
