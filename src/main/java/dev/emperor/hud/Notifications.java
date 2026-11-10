package dev.emperor.hud;

import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.modules.HudModule;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Animated toast notifications stacked in the bottom-right corner. */
public final class Notifications {
    public enum Kind {
        ENABLED(0xFF4CE08A, "✔"),
        DISABLED(0xFFFF5C6C, "✖"),
        INFO(0, "♛");

        final int color;
        final String icon;

        Kind(int color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    private static final class Toast {
        final String title;
        final String text;
        final Kind kind;
        final long start = System.currentTimeMillis();
        final long duration;
        float y = -1;

        Toast(String title, String text, Kind kind, long duration) {
            this.title = title;
            this.text = text;
            this.kind = kind;
            this.duration = duration;
        }
    }

    private static final List<Toast> TOASTS = new ArrayList<>();
    private static final int W = 148;
    private static final int H = 30;
    private static long lastFrame = System.nanoTime();

    private Notifications() {
    }

    private static boolean enabled() {
        HudModule h = ModuleManager.get(HudModule.class);
        return h != null && h.notifications.get();
    }

    public static void push(String title, String text, Kind kind) {
        if (MinecraftClient.getInstance().player == null) return;
        // Replace a pending toast with the same title so spam-toggling stays tidy.
        TOASTS.removeIf(t -> t.title.equals(title));
        TOASTS.add(new Toast(title, text, kind, 2200));
        while (TOASTS.size() > 6) TOASTS.remove(0);
    }

    public static void moduleToggled(Module m) {
        if (!enabled() || !m.showNotifications()) return;
        push(m.name, m.isEnabled() ? "Module enabled" : "Module disabled",
                m.isEnabled() ? Kind.ENABLED : Kind.DISABLED);
    }

    public static void render(DrawContext ctx) {
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastFrame) / 1e9f);
        lastFrame = nowNs;
        if (TOASTS.isEmpty()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        long now = System.currentTimeMillis();

        // Stack above any visible HUD widget occupying the bottom-right corner.
        int base = sh - 8;
        HudModule hm = ModuleManager.get(HudModule.class);
        if (hm != null && hm.isEnabled()) {
            for (HudElement e : Hud.ELEMENTS) {
                if (!e.visible.get() || !e.hasContent() || e.h <= 1) continue;
                if (e.x + e.w > sw - W - 10 && e.y > sh / 2) base = Math.min(base, e.y - 6);
            }
        }

        float slot = 0;
        for (int i = TOASTS.size() - 1; i >= 0; i--) {
            Toast t = TOASTS.get(i);
            long age = now - t.start;
            float in = Fx.easeOutBack(age / 320f);
            float out = age > t.duration ? Fx.easeOutCubic((age - t.duration) / 260f) : 0f;
            float vis = Math.max(0f, Math.min(1f, in) - out);

            float targetY = base - H - slot * (H + 5);
            t.y = t.y < 0 ? targetY : Fx.approach(t.y, targetY, dt, 14f);
            slot += 1f - out;

            int x = (int) (sw - 6 - W * (in - out * 1.1f));
            int y = Math.round(t.y);
            int accent = t.kind == Kind.INFO ? Theme.main() : t.kind.color;

            Rounded.shadow(ctx, x, y, x + W, y + H, 6, 5, Theme.alpha(0x000000, (int) (90 * vis)));
            Rounded.fill(ctx, x, y, x + W, y + H, 6, Theme.alpha(0x0F0F15, (int) (235 * vis)));
            Rounded.outline(ctx, x, y, x + W, y + H, 6, Theme.alpha(accent, (int) (70 * vis)));

            // icon bubble
            Rounded.circle(ctx, x + 15, y + 15, 9, Theme.alpha(accent, (int) (60 * vis)));
            if (!Fx.invisible(vis)) {
                ctx.drawCenteredTextWithShadow(tr, t.kind.icon, x + 15, y + 11, Theme.fade(accent, vis));
                ctx.drawTextWithShadow(tr, Text.literal(t.title).formatted(Formatting.BOLD), x + 30, y + 6,
                        Theme.fade(0xFFFFFFFF, vis));
                ctx.drawTextWithShadow(tr, t.text, x + 30, y + 17, Theme.fade(0xFF9A9AA8, vis));
            }

            // remaining-time bar
            float left = 1f - Math.min(1f, age / (float) t.duration);
            int bw = (int) ((W - 12) * left);
            if (bw > 0) Rounded.gradientH(ctx, x + 6, y + H - 3, x + 6 + bw, y + H - 2, 0,
                    Theme.fade(Theme.primary(), vis), Theme.fade(Theme.secondary(), vis));
        }

        Iterator<Toast> it = TOASTS.iterator();
        while (it.hasNext()) {
            Toast t = it.next();
            if (now - t.start > t.duration + 280) it.remove();
        }
    }
}
