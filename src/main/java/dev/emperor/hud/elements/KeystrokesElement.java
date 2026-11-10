package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/** WASD / mouse / space keystrokes with live CPS counters. */
public class KeystrokesElement extends HudElement {
    private static final int K = 22, G = 2;
    private final Map<String, Float> anim = new HashMap<>();
    private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
    private boolean wasL, wasR;

    public KeystrokesElement() {
        super("Keystrokes", false, 2, 5, 1, 40);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return K * 3 + G * 2;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return K * 3 + G * 3 + 12;
    }

    private static int cps(ArrayDeque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private void key(DrawContext ctx, TextRenderer tr, String id, String label, int kx, int ky, int kw, int kh, boolean down, float dt) {
        float a = Fx.approach(anim.getOrDefault(id, 0f), down ? 1f : 0f, dt, 22f);
        anim.put(id, a);
        Rounded.fill(ctx, kx, ky, kx + kw, ky + kh, 4, PANEL_BG);
        if (a > 0.01f) Rounded.gradientH(ctx, kx, ky, kx + kw, ky + kh, 4,
                Theme.alpha(Theme.primary(), (int) (0xD0 * a)), Theme.alpha(Theme.secondary(), (int) (0xD0 * a)));
        Rounded.outline(ctx, kx, ky, kx + kw, ky + kh, 4, Theme.alpha(Theme.main(), 0x30));
        ctx.drawCenteredTextWithShadow(tr, label, kx + kw / 2, ky + (kh - 8) / 2, Theme.lerp(0xFFE6E6EE, 0xFF0E0E14, a * 0.8f));
    }

    private static boolean down(KeyBinding kb) {
        return kb.isPressed();
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        long now = System.currentTimeMillis();
        boolean l = down(mc.options.attackKey), r = down(mc.options.useKey);
        if (l && !wasL) left.add(now);
        if (r && !wasR) right.add(now);
        wasL = l;
        wasR = r;

        int x0 = x, y0 = y;
        key(ctx, tr, "w", "W", x0 + K + G, y0, K, K, down(mc.options.forwardKey), dt);
        key(ctx, tr, "a", "A", x0, y0 + K + G, K, K, down(mc.options.leftKey), dt);
        key(ctx, tr, "s", "S", x0 + K + G, y0 + K + G, K, K, down(mc.options.backKey), dt);
        key(ctx, tr, "d", "D", x0 + 2 * (K + G), y0 + K + G, K, K, down(mc.options.rightKey), dt);
        int mw = (w - G) / 2;
        key(ctx, tr, "l", "LMB", x0, y0 + 2 * (K + G), mw, K, l, dt);
        key(ctx, tr, "r", "RMB", x0 + mw + G, y0 + 2 * (K + G), mw, K, r, dt);
        ctx.drawCenteredTextWithShadow(tr, cps(left, now) + " cps", x0 + mw / 2, y0 + 2 * (K + G) + K - 9, 0xFF9A9AA8);
        ctx.drawCenteredTextWithShadow(tr, cps(right, now) + " cps", x0 + mw + G + mw / 2, y0 + 2 * (K + G) + K - 9, 0xFF9A9AA8);
        key(ctx, tr, "space", "▬▬▬", x0, y0 + 3 * (K + G), w, 12, down(mc.options.jumpKey), dt);
    }
}
