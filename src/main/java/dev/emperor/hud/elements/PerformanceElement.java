package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.util.Rounded;
import dev.emperor.util.TickRate;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.Locale;

/** Live FPS graph plus server TPS, ping and a lag warning when the server stops responding. */
public class PerformanceElement extends HudElement {
    private static final int W = 128, GRAPH_H = 30, SAMPLES = 64;
    private final float[] fps = new float[SAMPLES];
    private int head;
    private long lastSample;

    public PerformanceElement() {
        super("Performance", true, 2, 5, 2, 5);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return W;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return GRAPH_H + 30;
    }

    private static int tpsColor(float tps) {
        return tps > 18 ? 0xFF4CE08A : tps > 14 ? 0xFFFFD84C : 0xFFFF4C5C;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        long now = System.currentTimeMillis();
        if (now - lastSample > 100) {
            lastSample = now;
            fps[head] = mc.getCurrentFps();
            head = (head + 1) % SAMPLES;
        }
        panel(ctx, x, y, w, h, 1f);
        title(ctx, tr, "⚡", "Performance", x + 6, y + 5);

        float max = 30;
        for (float f : fps) max = Math.max(max, f);
        max *= 1.15f;
        int gx = x + 6, gy = y + 17, gw = W - 12;
        ctx.fill(gx, gy + GRAPH_H, gx + gw, gy + GRAPH_H + 1, 0x30FFFFFF);
        float step = gw / (float) (SAMPLES - 1);
        float px = -1, py = -1;
        for (int i = 0; i < SAMPLES; i++) {
            float v = fps[(head + i) % SAMPLES];
            float cx = gx + i * step;
            float cy = gy + GRAPH_H - v / max * GRAPH_H;
            int col = Theme.gradient(i / (float) SAMPLES);
            // soft area under the curve
            ctx.fillGradient((int) cx, (int) cy, (int) (cx + step) + 1, gy + GRAPH_H, Theme.alpha(col, 0x50), Theme.alpha(col, 0x00));
            if (px >= 0) Rounded.line(ctx, px, py, cx, cy, col);
            px = cx;
            py = cy;
        }

        PlayerListEntry me = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        int ping = me == null ? 0 : me.getLatency();
        float tps = TickRate.tps();
        float lag = TickRate.sinceLastPacket();
        int ty = gy + GRAPH_H + 4;
        String f = mc.getCurrentFps() + " fps";
        String t = mc.isInSingleplayer() ? "" : String.format(Locale.ROOT, "%.1f tps", tps);
        String p = ping + " ms";
        ctx.drawTextWithShadow(tr, f, gx, ty, 0xFFFFFFFF);
        if (!t.isEmpty()) ctx.drawCenteredTextWithShadow(tr, t, x + W / 2 + 4, ty, tpsColor(tps));
        ctx.drawTextWithShadow(tr, p, gx + gw - tr.getWidth(p), ty, ping < 80 ? 0xFF4CE08A : ping < 180 ? 0xFFFFD84C : 0xFFFF4C5C);

        if (lag > 1.5f && !mc.isInSingleplayer()) {
            String l = String.format(Locale.ROOT, "⚠ Server not responding %.1fs", lag);
            int lw = tr.getWidth(l) + 10;
            int lx = x + W - lw, ly = y - 15;
            Rounded.fill(ctx, lx, ly, lx + lw, ly + 12, 4, 0xE0300A10);
            ctx.drawTextWithShadow(tr, l, lx + 5, ly + 2, 0xFFFF4C5C);
        }
    }
}
