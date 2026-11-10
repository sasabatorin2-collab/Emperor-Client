package dev.nova.hud;

import dev.nova.NovaClient;
import dev.nova.module.Module;
import dev.nova.module.ModuleManager;
import dev.nova.modules.HudModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Hud {
    public static final int ACCENT = 0xFF7C5CFF;

    private Hud() {
    }

    public static int color(boolean rainbow, int index) {
        if (!rainbow) return Theme.accent(index);
        float hue = ((System.currentTimeMillis() / 20 + index * 14L) % 360) / 360f;
        return Color.HSBtoRGB(hue, 0.55f, 1f);
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        HudModule h = ModuleManager.get(HudModule.class);
        if (h == null || !h.isEnabled() || mc.options.hudHidden || mc.player == null) return;

        TextRenderer tr = mc.textRenderer;
        boolean rb = h.rainbow.get();
        int y = 4;

        if (h.watermark.get()) {
            ctx.drawTextWithShadow(tr, NovaClient.NAME, 4, y, color(rb, 0));
            ctx.drawTextWithShadow(tr, " " + NovaClient.VERSION, 4 + tr.getWidth(NovaClient.NAME), y, 0xFFAAAAAA);
            y += 11;
        }
        if (h.fps.get()) {
            ctx.drawTextWithShadow(tr, "FPS: " + mc.getCurrentFps(), 4, y, 0xFFFFFFFF);
            y += 11;
        }
        if (h.coords.get()) {
            String c = String.format(Locale.ROOT, "XYZ: %.1f %.1f %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ());
            ctx.drawTextWithShadow(tr, c, 4, y, 0xFFFFFFFF);
        }

        if (h.arrayList.get()) {
            List<Module> active = new ArrayList<>();
            for (Module m : ModuleManager.all()) {
                if (m.isEnabled() && m != h) active.add(m);
            }
            active.sort((a, b) -> Integer.compare(width(tr, b), width(tr, a)));

            int sw = ctx.getScaledWindowWidth();
            int ry = 3;
            int i = 0;
            for (Module m : active) {
                int w = width(tr, m);
                int x = sw - w - 5;
                int col = color(rb, i++);
                ctx.fill(x - 2, ry - 1, sw, ry + 10, 0x90000000);
                ctx.fill(sw - 2, ry - 1, sw, ry + 10, col);
                ctx.drawTextWithShadow(tr, m.name, x, ry, col);
                String info = m.getInfo();
                if (info != null) {
                    ctx.drawTextWithShadow(tr, " " + info, x + tr.getWidth(m.name), ry, 0xFFAAAAAA);
                }
                ry += 11;
            }
        }
    }

    private static int width(TextRenderer tr, Module m) {
        String info = m.getInfo();
        return tr.getWidth(m.name) + (info == null ? 0 : tr.getWidth(" " + info));
    }
}
