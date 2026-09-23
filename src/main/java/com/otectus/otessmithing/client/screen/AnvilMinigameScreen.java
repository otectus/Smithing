package com.otectus.otessmithing.client.screen;

import com.otectus.otessmithing.config.ClientConfig;
import com.otectus.otessmithing.minigame.AnvilMinigame;
import com.otectus.otessmithing.minigame.AnvilPattern;
import com.otectus.otessmithing.minigame.AnvilRun;
import com.otectus.otessmithing.network.ModNetwork;
import com.otectus.otessmithing.network.packet.AnvilStrikePacket;
import com.otectus.otessmithing.network.packet.AnvilStrikeResultPacket;
import com.otectus.otessmithing.network.packet.CancelSessionPacket;
import com.otectus.otessmithing.network.packet.OpenAnvilScreenPacket;
import com.otectus.otessmithing.network.packet.SessionResultPacket;
import com.otectus.otessmithing.quality.SmithingQuality;
import com.otectus.otessmithing.registry.ModSounds;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Anvil screen: strike targets appear over a hot silhouette of the workpiece. A shrinking ring shows the
 * ideal moment. Strikes are predicted locally and corrected by the server, which alone decides the score and
 * whether the timer ran out (Faulty).
 */
public class AnvilMinigameScreen extends Screen {
    private enum Stage { READY, PLAYING, RESULT }

    private static final int WIDTH = 256;
    private static final int HEIGHT = 216;
    private static final int AREA = 176;
    private static final float OVERLAY_Z = 300F;

    private final OpenAnvilScreenPacket session;
    private final AnvilPattern pattern;
    private final AnvilRun run;
    private final long clientStartMs;
    private final RandomSource shakeRandom = RandomSource.create();
    private final Map<Integer, int[]> predictions = new HashMap<>();
    private final List<Spark> sparks = new ArrayList<>();
    private Stage stage = Stage.READY;
    private Button doneButton;
    private int left;
    private int top;
    private int sequence;
    private int cuedIndex = -1;
    private int cuedAttempt = -1;
    private float lastQuality = -1F;
    private long lastFeedbackMs;
    private long shakeUntil;
    private long flashUntil;
    private int resultScore;
    private boolean resultFaulty;
    private boolean closedByServer;

    private record Spark(float x, float y, float vx, float vy, long born) {}

    public AnvilMinigameScreen(OpenAnvilScreenPacket session) {
        super(Component.translatable("screen.otes_smithing.anvil"));
        this.session = session;
        this.pattern = session.pattern();
        this.run = new AnvilRun(pattern, session.seed());
        this.clientStartMs = Util.getMillis() + session.readyDelayMs();
    }

    public int sessionId() {
        return session.sessionId();
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        doneButton = Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + WIDTH / 2 - 40, top + HEIGHT - 28, 80, 20).build();
        doneButton.visible = stage == Stage.RESULT;
        addRenderableWidget(doneButton);
    }

    private int areaX() {
        return left + 12;
    }

    private int areaY() {
        return top + 28;
    }

    private int elapsed() {
        return (int) (Util.getMillis() - clientStartMs);
    }

    // ------------------------------------------------------------------ server events

    public void onStrikeResult(AnvilStrikeResultPacket packet) {
        if (packet.sessionId() != session.sessionId()) return;
        int[] predicted = predictions.remove(packet.sequence());
        boolean matches = predicted != null && predicted[0] == packet.index() && predicted[1] == packet.attempt()
                && predicted[2] == packet.spawnMs() && predicted[3] == packet.samples();
        if (!matches) {
            run.correct(packet.index(), packet.attempt(), packet.spawnMs(), packet.sum(), packet.samples());
            predictions.keySet().removeIf(seq -> seq > packet.sequence());
        }
        if (!packet.ignored()) {
            lastQuality = packet.hit() ? packet.quality() : 0F;
        }
    }

    public void onResult(SessionResultPacket packet) {
        if (packet.sessionId() != session.sessionId()) return;
        resultScore = packet.score();
        resultFaulty = packet.timedOut();
        stage = Stage.RESULT;
        doneButton.visible = true;
    }

    public void closeByServer() {
        closedByServer = true;
        onClose();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (stage == Stage.PLAYING && button == 0) {
            double nx = (mouseX - areaX()) / AREA;
            double ny = (mouseY - areaY()) / AREA;
            if (nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1) {
                strike((float) nx, (float) ny);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void strike(float x, float y) {
        int now = elapsed();
        if (now < 0 || now > session.allowedMs() || run.isComplete()) return;
        AnvilMinigame.Target target = run.currentTarget();
        AnvilRun.StrikeResult result = run.strike(now, x, y);
        if (result == null) return; // between targets or too fast: nothing is sent
        int seq = ++sequence;
        predictions.put(seq, new int[]{run.index(), run.attempt(), run.spawnMs(), run.samples()});
        ModNetwork.CHANNEL.sendToServer(new AnvilStrikePacket(session.sessionId(), seq, x, y, now));
        lastQuality = result.hit() ? result.quality() : 0F;
        lastFeedbackMs = Util.getMillis();
        if (result.hit()) {
            boolean perfect = result.quality() >= 0.9F;
            SmithingGui.playUi(perfect ? ModSounds.ANVIL_STRIKE_PERFECT.get() : ModSounds.ANVIL_STRIKE.get(), 0.9F + result.quality() * 0.2F, 0.9F);
            if (!ClientConfig.get(ClientConfig.REDUCED_SCREEN_SHAKE)) shakeUntil = Util.getMillis() + 120;
            if (perfect && !ClientConfig.get(ClientConfig.REDUCED_FLASHES)) flashUntil = Util.getMillis() + 140;
            int count = ClientConfig.scaleParticles(perfect ? 8 : 5);
            for (int i = 0; i < count; i++) {
                float angle = shakeRandom.nextFloat() * (float) Math.PI * 2F;
                float speed = 30F + shakeRandom.nextFloat() * 50F;
                sparks.add(new Spark(areaX() + target.x() * AREA, areaY() + target.y() * AREA,
                        (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed - 20F, Util.getMillis()));
            }
        } else {
            SmithingGui.playUi(ModSounds.ANVIL_MISS.get(), 1.0F, 0.7F);
        }
    }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void tick() {
        if (stage == Stage.READY && elapsed() >= 0) stage = Stage.PLAYING;
        long now = Util.getMillis();
        sparks.removeIf(s -> now - s.born() > 300);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (stage != Stage.RESULT && !closedByServer) {
            ModNetwork.CHANNEL.sendToServer(new CancelSessionPacket(session.sessionId()));
        }
        super.removed();
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g);
        long wall = Util.getMillis();
        g.pose().pushPose();
        if (wall < shakeUntil) {
            float amount = (shakeUntil - wall) / 120F * 2F;
            g.pose().translate((shakeRandom.nextFloat() - 0.5F) * amount, (shakeRandom.nextFloat() - 0.5F) * amount, 0);
        }
        SmithingGui.panel(g, left, top, WIDTH, HEIGHT);
        g.drawString(font, Component.translatable("screen.otes_smithing.anvil.title", session.target().getHoverName()),
                left + 12, top + 10, SmithingGui.TEXT_WARM, false);

        int ax = areaX();
        int ay = areaY();
        SmithingGui.well(g, ax - 2, ay - 2, AREA + 4, AREA + 4);
        g.fillGradient(ax, ay, ax + AREA, ay + AREA, 0xFF2A1810, 0xFF140C08);
        renderSilhouette(g, ax, ay);
        g.pose().pushPose();
        g.pose().translate(0, 0, OVERLAY_Z);

        switch (stage) {
            case READY -> {
                int remaining = -elapsed();
                g.fill(ax, ay, ax + AREA, ay + AREA, 0x80000000);
                g.drawCenteredString(font, Component.translatable("screen.otes_smithing.ready", Math.max(1, (remaining + 999) / 1000)),
                        ax + AREA / 2, ay + AREA / 2 - 10, SmithingGui.TEXT_WARM);
                g.drawCenteredString(font, Component.translatable("screen.otes_smithing.anvil.instructions"), ax + AREA / 2, ay + AREA / 2 + 6, SmithingGui.TEXT);
            }
            case PLAYING -> renderTargets(g, ax, ay);
            case RESULT -> renderResult(g, ax, ay);
        }
        renderSidebar(g);
        renderSparks(g, wall);
        if (wall < flashUntil) {
            int alpha = (int) (0x50 * (flashUntil - wall) / 140F);
            g.fill(ax, ay, ax + AREA, ay + AREA, (alpha << 24) | 0xFFF0C0);
        }
        g.pose().popPose();
        g.pose().popPose();
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderSilhouette(GuiGraphics g, int ax, int ay) {
        float scale = (AREA - 16) / 16F;
        g.pose().pushPose();
        g.pose().translate(ax + 8, ay + 8, 0);
        g.pose().scale(scale, scale, 1F);
        g.setColor(0.62F, 0.3F, 0.16F, 1.0F);
        g.renderItem(session.target(), 0, 0);
        g.setColor(1F, 1F, 1F, 1F);
        g.pose().popPose();
        // GUI items render at z ~150; everything meant to sit on top of the silhouette goes above that.
        g.pose().pushPose();
        g.pose().translate(0, 0, OVERLAY_Z);
        g.fill(ax, ay, ax + AREA, ay + AREA, 0x66140600);
        g.pose().popPose();
    }

    private void renderTargets(GuiGraphics g, int ax, int ay) {
        int now = elapsed();
        if (now > session.allowedMs() || run.isComplete()) {
            g.drawCenteredString(font, Component.translatable(now > session.allowedMs() ? "screen.otes_smithing.times_up" : "screen.otes_smithing.finishing"),
                    ax + AREA / 2, ay + AREA / 2, SmithingGui.TEXT);
            return;
        }
        run.advanceTo(now);
        float radius = pattern.radius() * AREA;
        float drawRadius = radius * SmithingGui.markerScale();
        // Faint preview of the next target helps the smith read the rhythm.
        if (run.index() + 1 < pattern.strikes()) {
            AnvilMinigame.Target next = AnvilMinigame.target(pattern, session.seed(), run.index() + 1, 0);
            SmithingGui.circle(g, ax + next.x() * AREA, ay + next.y() * AREA, drawRadius, true, 0x50FFFFFF);
        }
        int local = now - run.spawnMs();
        if (local < 0) return;
        AnvilMinigame.Target target = run.currentTarget();
        float cx = ax + target.x() * AREA;
        float cy = ay + target.y() * AREA;
        int ideal = AnvilMinigame.idealMs(pattern);
        float fade = local > ideal ? 1F - (local - ideal) / (float) Math.max(1, pattern.lifetimeMs() - ideal) * 0.6F : 1F;
        int alpha = Math.max(150, Math.round(255 * fade));
        // A dark halo keeps the target readable over any silhouette.
        SmithingGui.circle(g, cx, cy, drawRadius + 3, false, (Math.round(alpha * 0.7F) << 24));
        SmithingGui.circle(g, cx, cy, drawRadius, false, (Math.round(alpha * 0.8F) << 24) | (SmithingGui.targetColor() & 0xFFFFFF));
        SmithingGui.circle(g, cx, cy, drawRadius * 0.35F, false, (alpha << 24) | (SmithingGui.perfectColor() & 0xFFFFFF));
        // Approach ring: closes onto the target at the ideal moment.
        float approach = local < ideal ? 1F + 2F * (1F - local / (float) ideal) : 1F - 0.3F * (local - ideal) / (float) Math.max(1, pattern.lifetimeMs() - ideal);
        SmithingGui.circle(g, cx, cy, drawRadius * approach + 1, true, (alpha << 24));
        SmithingGui.circle(g, cx, cy, drawRadius * approach, true, (alpha << 24) | 0xFFFFFF);
        if (local >= ideal && (cuedIndex != run.index() || cuedAttempt != run.attempt())) {
            cuedIndex = run.index();
            cuedAttempt = run.attempt();
            SmithingGui.playCue();
        }
    }

    private void renderResult(GuiGraphics g, int ax, int ay) {
        g.fill(ax, ay, ax + AREA, ay + AREA, 0xA0000000);
        int cx = ax + AREA / 2;
        if (resultFaulty) {
            g.drawCenteredString(font, Component.translatable("screen.otes_smithing.anvil.faulty"), cx, ay + 60, SmithingGui.missColor());
            g.drawCenteredString(font, SmithingQuality.FAULTY.displayName(), cx, ay + 76, SmithingGui.missColor());
        } else {
            g.drawCenteredString(font, Component.translatable("screen.otes_smithing.anvil.complete"), cx, ay + 60, SmithingGui.TEXT_WARM);
            Component quality = SmithingQuality.fromScore(resultScore).displayName();
            if (ClientConfig.get(ClientConfig.SHOW_NUMERIC_QUALITY_SCORES)) quality = quality.copy().append(" (" + resultScore + ")");
            g.drawCenteredString(font, Component.translatable("screen.otes_smithing.anvil.efficacy", quality), cx, ay + 76, SmithingGui.TEXT);
        }
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.translatable("screen.otes_smithing.anvil.next"), AREA - 16);
        for (int i = 0; i < lines.size(); i++) {
            g.drawCenteredString(font, lines.get(i), cx, ay + 100 + i * 10, SmithingGui.TEXT_DIM);
        }
    }

    private void renderSidebar(GuiGraphics g) {
        int sx = left + 12 + AREA + 12;
        int sw = WIDTH - (sx - left) - 12;
        int now = Math.max(0, elapsed());
        float remaining = stage == Stage.READY ? 1F : 1F - now / (float) Math.max(1, session.allowedMs());
        // Vertical timer
        int barX = sx + sw / 2 - 5;
        int barTop = top + 28;
        int barH = 110;
        SmithingGui.well(g, barX - 1, barTop - 1, 12, barH + 2);
        int filled = Math.round(barH * Math.max(0F, Math.min(1F, remaining)));
        int color = remaining > 0.25F ? 0xFFE0A030 : SmithingGui.missColor();
        if (filled > 0) g.fill(barX, barTop + barH - filled, barX + 10, barTop + barH, color);

        Component strikes = Component.translatable("screen.otes_smithing.anvil.strikes", Math.min(run.index(), pattern.strikes()), pattern.strikes());
        g.drawCenteredString(font, strikes, sx + sw / 2, barTop + barH + 8, SmithingGui.TEXT);
        if (run.samples() > 0 && stage == Stage.PLAYING) {
            g.drawCenteredString(font, SmithingQuality.fromScore(run.score()).displayName(), sx + sw / 2, barTop + barH + 20, SmithingGui.TEXT_DIM);
        }
        if (lastQuality >= 0 && Util.getMillis() - lastFeedbackMs < 900) {
            g.drawCenteredString(font, SmithingGui.rating(lastQuality), sx + sw / 2, barTop + barH + 34, SmithingGui.ratingColor(lastQuality));
        }
    }

    private void renderSparks(GuiGraphics g, long wall) {
        for (Spark s : sparks) {
            float t = (wall - s.born()) / 1000F;
            if (t > 0.3F) continue;
            int x = Math.round(s.x() + s.vx() * t);
            int y = Math.round(s.y() + s.vy() * t + 120F * t * t);
            int alpha = Math.round(255 * (1F - t / 0.3F));
            g.fill(x, y, x + 2, y + 2, (alpha << 24) | 0xFFD070);
        }
    }
}
