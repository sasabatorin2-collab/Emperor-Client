package dev.emperor.util;

import dev.emperor.hud.Theme;
import net.minecraft.client.gui.DrawContext;

/** Rounded shapes built from 1px scanlines (no textures or shaders needed). */
public final class Rounded {
    private Rounded() {
    }

    /** Horizontal inset of a rounded corner on the given row (0 for rows outside the corners). */
    private static int inset(int row, int len, int r) {
        if (r <= 0) return 0;
        int i;
        if (row < r) i = row;
        else if (row >= len - r) i = len - 1 - row;
        else return 0;
        double d = r - i - 0.5;
        return r - (int) Math.round(Math.sqrt(r * r - d * d));
    }

    public static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        fill(ctx, x1, y1, x2, y2, r, color, true, true);
    }

    public static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color,
                            boolean roundTop, boolean roundBottom) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0 || (color >>> 24) == 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            int in = inset(i, h, r);
            if (roundTop) ctx.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, color);
            else ctx.fill(x1, y1 + i, x2, y1 + i + 1, color);
            if (roundBottom) ctx.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, color);
            else ctx.fill(x1, y2 - i - 1, x2, y2 - i, color);
        }
        ctx.fill(x1, y1 + r, x2, y2 - r, color);
    }

    /** 1px rounded border that does not overlap the interior (safe for translucent colours). */
    public static void outline(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 2 || h <= 2 || (color >>> 24) == 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        for (int row = 0; row < h; row++) {
            int out = inset(row, h, r);
            int y = y1 + row;
            if (row == 0 || row == h - 1) {
                ctx.fill(x1 + out, y, x2 - out, y + 1, color);
                continue;
            }
            int inner = r > 1 ? inset(row - 1, h - 2, r - 1) + 1 : 1;
            int seg = Math.max(1, inner - out);
            ctx.fill(x1 + out, y, x1 + out + seg, y + 1, color);
            ctx.fill(x2 - out - seg, y, x2 - out, y + 1, color);
        }
    }

    /** Rounded rectangle with a left-to-right colour gradient. */
    public static void gradientH(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int c1, int c2) {
        gradientH(ctx, x1, y1, x2, y2, r, c1, c2, true, true);
    }

    public static void gradientH(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int c1, int c2,
                                 boolean roundTop, boolean roundBottom) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        for (int col = 0; col < w; col++) {
            int in = inset(col, w, r);
            int top = roundTop ? y1 + in : y1;
            int bottom = roundBottom ? y2 - in : y2;
            int c = Theme.lerp(c1, c2, w == 1 ? 0f : col / (float) (w - 1));
            ctx.fill(x1 + col, top, x1 + col + 1, bottom, c);
        }
    }

    /** Soft drop shadow / glow made from stacked translucent rounded layers. */
    public static void shadow(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int size, int color) {
        int baseA = (color >>> 24) & 0xFF;
        if (size <= 0 || baseA == 0) return;
        int layer = Theme.alpha(color, Math.max(1, baseA / size));
        for (int i = size; i >= 1; i--) {
            fill(ctx, x1 - i, y1 - i, x2 + i, y2 + i, r + i, layer);
        }
    }

    public static void circle(DrawContext ctx, int cx, int cy, int radius, int color) {
        fill(ctx, cx - radius, cy - radius, cx + radius, cy + radius, radius, color);
    }

    /** Any-angle 1px line, stepped per pixel (fine for short GUI strokes). */
    public static void line(DrawContext ctx, float x1, float y1, float x2, float y2, int color) {
        float dx = x2 - x1, dy = y2 - y1;
        int steps = (int) Math.max(1, Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        for (int i = 0; i <= steps; i++) {
            int px = Math.round(x1 + dx * i / steps), py = Math.round(y1 + dy * i / steps);
            ctx.fill(px, py, px + 1, py + 1, color);
        }
    }
}
