package dev.emperor.gui;

import dev.emperor.hud.Hud;
import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.module.ModuleManager;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Drag HUD widgets around; positions snap to edges and the screen centre. */
public class HudEditorScreen extends Screen {
    private static final int SNAP = 5, MARGIN = 4;
    private final Screen parent;
    private HudElement dragging;
    private int offX, offY;
    private int guideX = -1, guideY = -1;
    private final long openedAt = System.currentTimeMillis();

    public HudEditorScreen(Screen parent) {
        super(Text.literal("HUD Editor"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x40000000);
        int grid = 0x0CFFFFFF;
        for (int gx = 0; gx < width; gx += 20) ctx.fill(gx, 0, gx + 1, height, grid);
        for (int gy = 0; gy < height; gy += 20) ctx.fill(0, gy, width, gy + 1, grid);
        ctx.fill(width / 2, 0, width / 2 + 1, height, 0x20FFFFFF);
        ctx.fill(0, height / 2, width, height / 2 + 1, 0x20FFFFFF);
    }

    private HudElement at(double mx, double my) {
        for (int i = Hud.ELEMENTS.size() - 1; i >= 0; i--) {
            HudElement e = Hud.ELEMENTS.get(i);
            if (mx >= e.x - 2 && mx < e.x + e.w + 2 && my >= e.y - 2 && my < e.y + e.h + 2) return e;
        }
        return null;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        Hud.renderElements(ctx, true);
        HudElement hover = dragging != null ? dragging : at(mouseX, mouseY);

        for (HudElement e : Hud.ELEMENTS) {
            boolean on = e.visible.get();
            boolean hv = e == hover;
            int x1 = e.x - 2, y1 = e.y - 2, x2 = e.x + e.w + 2, y2 = e.y + e.h + 2;
            if (!on) {
                Rounded.fill(ctx, x1, y1, x2, y2, 4, 0xB0000000);
                ctx.drawCenteredTextWithShadow(textRenderer, "hidden", (x1 + x2) / 2, (y1 + y2) / 2 - 4, 0xFF8C8C9A);
            }
            if (hv) Rounded.fill(ctx, x1, y1, x2, y2, 4, Theme.alpha(Theme.main(), 0x22));
            Rounded.outline(ctx, x1, y1, x2, y2, 4, hv ? Theme.main() : on ? Theme.alpha(Theme.main(), 0x70) : 0x60FFFFFF);
            if (hv) {
                String label = e.name + (on ? "" : " (hidden)");
                int lw = textRenderer.getWidth(label) + 8;
                int ly = y1 - 13 < 0 ? y2 + 2 : y1 - 13;
                Rounded.fill(ctx, x1, ly, x1 + lw, ly + 11, 3, Theme.main());
                ctx.drawTextWithShadow(textRenderer, label, x1 + 4, ly + 2, 0xFFFFFFFF);
            }
        }

        if (dragging != null) {
            if (guideX >= 0) ctx.fill(guideX, 0, guideX + 1, height, Theme.primary());
            if (guideY >= 0) ctx.fill(0, guideY, width, guideY + 1, Theme.primary());
        }

        float a = Fx.clamp01((System.currentTimeMillis() - openedAt) / 250f);
        String help = "✎ HUD Editor   ·   Drag to move   ·   RMB hide/show   ·   MMB reset   ·   R reset all   ·   Esc back";
        int hw = textRenderer.getWidth(help) + 20;
        int hx = (width - hw) / 2, hy = height - 26;
        Rounded.shadow(ctx, hx, hy, hx + hw, hy + 18, 8, 6, Theme.alpha(0, (int) (0x60 * a)));
        Rounded.fill(ctx, hx, hy, hx + hw, hy + 18, 8, Theme.fade(0xE60E0E14, a));
        Rounded.outline(ctx, hx, hy, hx + hw, hy + 18, 8, Theme.alpha(Theme.main(), (int) (0x90 * a)));
        if (!Fx.invisible(a)) ctx.drawTextWithShadow(textRenderer, help, hx + 10, hy + 5, Theme.fade(0xFFE6E6EE, a));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        HudElement e = at(mx, my);
        if (e == null) return super.mouseClicked(mx, my, button);
        if (button == 0) {
            dragging = e;
            offX = (int) mx - e.x;
            offY = (int) my - e.y;
        } else if (button == 1) {
            e.visible.toggle();
        } else if (button == 2) {
            e.reset();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging == null) return super.mouseDragged(mx, my, button, dx, dy);
        HudElement e = dragging;
        int nx = (int) mx - offX, ny = (int) my - offY;
        guideX = guideY = -1;
        if (Math.abs(nx - MARGIN) < SNAP) nx = MARGIN;
        else if (Math.abs(nx + e.w - (width - MARGIN)) < SNAP) nx = width - MARGIN - e.w;
        else if (Math.abs(nx + e.w / 2 - width / 2) < SNAP) {
            nx = width / 2 - e.w / 2;
            guideX = width / 2;
        }
        if (Math.abs(ny - MARGIN) < SNAP) ny = MARGIN;
        else if (Math.abs(ny + e.h - (height - MARGIN)) < SNAP) ny = height - MARGIN - e.h;
        else if (Math.abs(ny + e.h / 2 - height / 2) < SNAP) {
            ny = height / 2 - e.h / 2;
            guideY = height / 2;
        }
        nx = Math.max(0, Math.min(width - e.w, nx));
        ny = Math.max(0, Math.min(height - e.h, ny));
        e.moveTo(nx, ny, width, height);
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_R) {
            for (HudElement e : Hud.ELEMENTS) e.reset();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (client != null) client.setScreen(parent);
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }
}
