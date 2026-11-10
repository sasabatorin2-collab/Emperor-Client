package dev.emperor.util;

import net.minecraft.client.gui.DrawContext;

/** Rounded rectangles built from 1px scanlines (no textures or shaders needed). */
public final class Rounded {
    private Rounded() {
    }

    public static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
        fill(ctx, x1, y1, x2, y2, r, color, true, true);
    }

    public static void fill(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color,
                            boolean roundTop, boolean roundBottom) {
        int w = x2 - x1;
        int h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        if (r <= 0) {
            ctx.fill(x1, y1, x2, y2, color);
            return;
        }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            if (roundTop) ctx.fill(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            else ctx.fill(x1, y1 + i, x2, y1 + i + 1, color);
            if (roundBottom) ctx.fill(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
            else ctx.fill(x1, y2 - i - 1, x2, y2 - i, color);
        }
        ctx.fill(x1, y1 + r, x2, y2 - r, color);
    }
}
