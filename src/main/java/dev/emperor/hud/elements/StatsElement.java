package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.util.Session;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.Locale;

public class StatsElement extends HudElement {
    public StatsElement() {
        super("Session stats", true, 0, 5, 0, 172);
    }

    private String[][] rows() {
        String dist = Session.distance >= 1000 ? String.format(Locale.ROOT, "%.2f km", Session.distance / 1000)
                : String.format(Locale.ROOT, "%.0f m", Session.distance);
        return new String[][]{
                {"⌚", "Playtime", Session.playtime()},
                {"⚔", "Kills", String.valueOf(Session.kills)},
                {"☠", "Deaths", String.valueOf(Session.deaths)},
                {"✦", "K/D", Session.kd()},
                {"⛏", "Blocks", String.valueOf(Session.blocks)},
                {"➶", "Distance", dist},
        };
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        int w = tr.getWidth("♛ Session") + 12;
        for (String[] r : rows()) w = Math.max(w, tr.getWidth(r[0] + " " + r[1]) + tr.getWidth(r[2]) + 26);
        return w;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return 18 + rows().length * 10 + 4;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        panel(ctx, x, y, w, h, 1f);
        title(ctx, tr, "♛", "Session", x + 6, y + 5);
        ctx.fill(x + 6, y + 16, x + w - 6, y + 17, 0x20FFFFFF);
        int ry = y + 20, i = 0;
        for (String[] r : rows()) {
            ctx.drawTextWithShadow(tr, r[0], x + 6, ry, Theme.accent(i * 3));
            ctx.drawTextWithShadow(tr, r[1], x + 18, ry, 0xFF9A9AA8);
            ctx.drawTextWithShadow(tr, r[2], x + w - 6 - tr.getWidth(r[2]), ry, 0xFFFFFFFF);
            ry += 10;
            i++;
        }
    }
}
