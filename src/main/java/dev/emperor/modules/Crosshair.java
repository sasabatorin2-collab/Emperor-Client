package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Custom dynamic crosshair with hit markers and an attack-cooldown arc. */
public class Crosshair extends Module {
    private final ModeSetting style = add(new ModeSetting("Style", "Cross", "Cross", "Circle", "Dot", "T-Cross"));
    private final NumberSetting size = add(new NumberSetting("Size", 5, 2, 12, 1));
    private final NumberSetting gap = add(new NumberSetting("Gap", 3, 0, 8, 1));
    private final BoolSetting dynamic = add(new BoolSetting("Dynamic gap", true));
    private final BoolSetting themed = add(new BoolSetting("Theme colour", true));
    private final BoolSetting hitMarker = add(new BoolSetting("Hit marker", true));
    private final BoolSetting cooldown = add(new BoolSetting("Cooldown bar", true));

    private static long hitAt;
    private float spread;
    private long lastNs = System.nanoTime();

    public Crosshair() {
        super("Crosshair", "Dynamic custom crosshair with hit markers.", Category.RENDER);
    }

    /** Vanilla crosshair is hidden while this draws. */
    public static boolean active() {
        Crosshair c = ModuleManager.get(Crosshair.class);
        MinecraftClient mc = MinecraftClient.getInstance();
        return c != null && c.isEnabled() && mc.options.getPerspective().isFirstPerson();
    }

    public static void onHit() {
        hitAt = System.currentTimeMillis();
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        if (!active() || mc.player.isSpectator()) return;
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;

        int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2;
        float speed = (float) mc.player.getVelocity().horizontalLength() * 12f + (mc.player.isOnGround() ? 0 : 2f);
        spread = Fx.approach(spread, dynamic.get() ? Math.min(6f, speed) : 0f, dt, 14f);
        int g = gap.i() + Math.round(spread);
        int s = size.i();
        int col = themed.get() ? Theme.main() : 0xFFFFFFFF;
        int shadow = 0x90000000;

        switch (style.get()) {
            case "Circle" -> {
                int r = g + 2;
                Rounded.outline(ctx, cx - r - 1, cy - r - 1, cx + r + 2, cy + r + 2, r + 1, shadow);
                Rounded.outline(ctx, cx - r, cy - r, cx + r + 1, cy + r + 1, r, col);
                ctx.fill(cx, cy, cx + 1, cy + 1, col);
            }
            case "Dot" -> {
                Rounded.circle(ctx, cx, cy, 2, shadow);
                ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, col);
            }
            default -> {
                boolean t = style.is("T-Cross");
                bar(ctx, cx - g - s, cy, cx - g, cy + 1, col, shadow);
                bar(ctx, cx + g + 1, cy, cx + g + 1 + s, cy + 1, col, shadow);
                bar(ctx, cx, cy + g + 1, cx + 1, cy + g + 1 + s, col, shadow);
                if (!t) bar(ctx, cx, cy - g - s, cx + 1, cy - g, col, shadow);
                ctx.fill(cx, cy, cx + 1, cy + 1, col);
            }
        }

        if (hitMarker.get()) {
            float ht = (System.currentTimeMillis() - hitAt) / 350f;
            if (ht < 1f) {
                int a = (int) (255 * (1 - ht));
                int hc = Theme.alpha(0xFFFFFF, a);
                float o = 4 + ht * 3, l = 4;
                Rounded.line(ctx, cx - o, cy - o, cx - o - l, cy - o - l, hc);
                Rounded.line(ctx, cx + o + 1, cy - o, cx + o + 1 + l, cy - o - l, hc);
                Rounded.line(ctx, cx - o, cy + o + 1, cx - o - l, cy + o + 1 + l, hc);
                Rounded.line(ctx, cx + o + 1, cy + o + 1, cx + o + 1 + l, cy + o + 1 + l, hc);
            }
        }

        if (cooldown.get()) {
            float p = mc.player.getAttackCooldownProgress(delta);
            if (p < 1f) {
                int w = 16, y = cy + g + s + 5;
                ctx.fill(cx - w / 2, y, cx + w / 2, y + 2, 0x80000000);
                Rounded.gradientH(ctx, cx - w / 2, y, cx - w / 2 + (int) (w * p), y + 2, 0, Theme.primary(), Theme.secondary());
            }
        }
    }

    private static void bar(DrawContext ctx, int x1, int y1, int x2, int y2, int col, int shadow) {
        ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, shadow);
        ctx.fill(x1, y1, x2, y2, col);
    }
}
