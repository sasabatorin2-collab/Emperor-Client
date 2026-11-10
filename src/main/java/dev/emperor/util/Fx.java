package dev.emperor.util;

import dev.emperor.hud.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/** Small animation and text helpers used by the GUI and HUD. */
public final class Fx {
    private Fx() {
    }

    /** Frame-rate independent exponential approach of {@code cur} towards {@code target}. */
    public static float approach(float cur, float target, float dt, float speed) {
        float k = 1f - (float) Math.exp(-dt * speed);
        float v = cur + (target - cur) * k;
        return Math.abs(target - v) < 0.001f ? target : v;
    }

    public static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /** Text whose characters follow the animated theme gradient. Returns the drawn width. */
    public static int gradientText(DrawContext ctx, TextRenderer tr, String text, int x, int y, int phase, float alpha) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            ctx.drawTextWithShadow(tr, ch, cx, y, Theme.fade(Theme.accent(phase + i * 2), alpha));
            cx += tr.getWidth(ch);
        }
        return cx - x;
    }

    /** True when the text alpha would be too low; the font renderer treats alpha < 4 as opaque. */
    public static boolean invisible(float alpha) {
        return alpha < 0.06f;
    }
}
