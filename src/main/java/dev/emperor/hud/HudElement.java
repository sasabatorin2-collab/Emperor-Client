package dev.emperor.hud;

import com.google.gson.JsonObject;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.Rounded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * A draggable HUD widget. Position is stored as an anchor (0 = left/top, 1 = centre, 2 = right/bottom)
 * plus an offset from that anchor, so widgets stick to their screen edge at any resolution.
 */
public abstract class HudElement {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final int PANEL_BG = 0xB80E0E14;

    public final String name;
    public final BoolSetting visible;
    private final int defAx, defOx, defAy, defOy;
    public int ax, ox, ay, oy;
    public int x, y, w, h;

    protected HudElement(String name, boolean on, int ax, int ox, int ay, int oy) {
        this.name = name;
        this.visible = new BoolSetting(name, on);
        this.defAx = ax;
        this.defOx = ox;
        this.defAy = ay;
        this.defOy = oy;
        reset();
    }

    public void reset() {
        ax = defAx;
        ox = defOx;
        ay = defAy;
        oy = defOy;
    }

    public abstract int width(TextRenderer tr, boolean preview);

    public abstract int height(TextRenderer tr, boolean preview);

    public abstract void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview);

    /** False when there is nothing to draw right now (the editor always draws). */
    public boolean hasContent() {
        return true;
    }

    public boolean rightAligned() {
        return ax == 2;
    }

    public void layout(TextRenderer tr, int sw, int sh, boolean preview) {
        w = width(tr, preview);
        h = height(tr, preview);
        x = switch (ax) {
            case 0 -> ox;
            case 1 -> sw / 2 - w / 2 + ox;
            default -> sw - w - ox;
        };
        y = switch (ay) {
            case 0 -> oy;
            case 1 -> sh / 2 - h / 2 + oy;
            default -> sh - h - oy;
        };
    }

    /** Places the element at an absolute position, picking the nearest anchor. */
    public void moveTo(int nx, int ny, int sw, int sh) {
        int cx = nx + w / 2, cy = ny + h / 2;
        ax = cx < sw / 3 ? 0 : cx > sw * 2 / 3 ? 2 : 1;
        ay = cy < sh / 3 ? 0 : cy > sh * 2 / 3 ? 2 : 1;
        ox = switch (ax) {
            case 0 -> nx;
            case 1 -> nx + w / 2 - sw / 2;
            default -> sw - w - nx;
        };
        oy = switch (ay) {
            case 0 -> ny;
            case 1 -> ny + h / 2 - sh / 2;
            default -> sh - h - ny;
        };
        x = nx;
        y = ny;
    }

    public JsonObject save() {
        JsonObject o = new JsonObject();
        o.addProperty("ax", ax);
        o.addProperty("ox", ox);
        o.addProperty("ay", ay);
        o.addProperty("oy", oy);
        return o;
    }

    public void load(JsonObject o) {
        if (o.has("ax")) ax = o.get("ax").getAsInt();
        if (o.has("ox")) ox = o.get("ox").getAsInt();
        if (o.has("ay")) ay = o.get("ay").getAsInt();
        if (o.has("oy")) oy = o.get("oy").getAsInt();
    }

    /** Standard glass panel used by HUD widgets. */
    protected static void panel(DrawContext ctx, int x, int y, int w, int h, float a) {
        Rounded.shadow(ctx, x, y, x + w, y + h, 6, 6, Theme.alpha(0, (int) (0x50 * a)));
        Rounded.fill(ctx, x, y, x + w, y + h, 6, Theme.fade(PANEL_BG, a));
        Rounded.gradientH(ctx, x + 4, y, x + w - 4, y + 1, 0, Theme.fade(Theme.gradient(0f), a), Theme.fade(Theme.gradient(1f), a));
        Rounded.outline(ctx, x, y, x + w, y + h, 6, Theme.alpha(Theme.main(), (int) (0x30 * a)));
    }

    protected static void title(DrawContext ctx, TextRenderer tr, String icon, String text, int x, int y) {
        ctx.drawTextWithShadow(tr, icon, x, y, Theme.primary());
        ctx.drawTextWithShadow(tr, net.minecraft.text.Text.literal(text).formatted(net.minecraft.util.Formatting.BOLD),
                x + tr.getWidth(icon) + 3, y, 0xFFFFFFFF);
    }
}
