package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.util.Fx;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleListElement extends HudElement {
    private static final int ROW_H = 12;
    private final Map<Module, Float> slide = new HashMap<>();
    private final Map<Module, Float> rowY = new HashMap<>();

    public ModuleListElement() {
        super("Module list", true, 2, 5, 0, 5);
    }

    private static int rowWidth(TextRenderer tr, Module m) {
        String info = m.getInfo();
        return tr.getWidth(m.name) + (info == null ? 0 : tr.getWidth(" " + info)) + 10;
    }

    private List<Module> shown() {
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.category == Category.CLIENT || m.hidden) continue;
            if (m.isEnabled() || slide.getOrDefault(m, 0f) > 0.01f) out.add(m);
        }
        return out;
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        int w = 0;
        for (Module m : shown()) w = Math.max(w, rowWidth(tr, m));
        return Math.max(w, preview ? tr.getWidth("No active modules") + 10 : 1);
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        float h = 0;
        for (Module m : shown()) h += ROW_H * Fx.easeOutCubic(slide.getOrDefault(m, 0f));
        return Math.max((int) Math.ceil(h), preview ? ROW_H : 1);
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        for (Module m : ModuleManager.all()) {
            if (m.category == Category.CLIENT) continue;
            float s = Fx.approach(slide.getOrDefault(m, 0f), m.isEnabled() && !m.hidden ? 1f : 0f, dt, 12f);
            slide.put(m, s);
        }
        List<Module> list = shown();
        list.removeIf(m -> slide.get(m) <= 0.01f);
        for (Module m : new ArrayList<>(rowY.keySet())) if (!list.contains(m)) rowY.remove(m);
        list.sort((a, b) -> Integer.compare(rowWidth(tr, b), rowWidth(tr, a)));

        if (list.isEmpty() && preview) {
            ctx.fill(x, y, x + w, y + ROW_H, PANEL_BG);
            ctx.drawTextWithShadow(tr, "No active modules", x + 5, y + 2, 0xFF8C8C9A);
            return;
        }

        boolean right = rightAligned();
        float ty = y;
        int i = 0;
        for (Module m : list) {
            float e = Fx.easeOutCubic(slide.get(m));
            float ry = rowY.containsKey(m) ? Fx.approach(rowY.get(m), ty, dt, 16f) : ty;
            rowY.put(m, ry);
            int rw = rowWidth(tr, m);
            int top = Math.round(ry);
            int col = Theme.accent(i * 3);
            int rx;
            if (right) {
                int edge = x + w;
                rx = (int) (edge - rw * e);
                ctx.fill(rx, top, edge, top + ROW_H, Theme.fade(PANEL_BG, e));
                ctx.fill(edge, top, edge + 2, top + ROW_H, Theme.fade(col, e));
            } else {
                rx = (int) (x - rw * (1 - e));
                ctx.fill(rx, top, rx + rw, top + ROW_H, Theme.fade(PANEL_BG, e));
                ctx.fill(x - 2, top, x, top + ROW_H, Theme.fade(col, e));
            }
            if (!Fx.invisible(e)) {
                ctx.drawTextWithShadow(tr, m.name, rx + 5, top + 2, Theme.fade(col, e));
                String info = m.getInfo();
                if (info != null) ctx.drawTextWithShadow(tr, " " + info, rx + 5 + tr.getWidth(m.name), top + 2, Theme.fade(0xFF9A9AA8, e));
            }
            ty += ROW_H * e;
            i++;
        }
    }
}
