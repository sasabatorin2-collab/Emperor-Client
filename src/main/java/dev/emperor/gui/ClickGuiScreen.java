package dev.emperor.gui;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.setting.Setting;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Rounded, searchable, scrollable ClickGUI with animated toggles. */
public class ClickGuiScreen extends Screen {
    private static final int PANEL_W = 122;
    private static final int HEADER_H = 18;
    private static final int ROW_H = 15;
    private static final int SET_H = 14;
    private static final int SLIDER_H = 19;
    private static final int PAD = 4;

    private static final int BG = 0x141418;
    private static final int HEADER_BG = 0x20202A;
    private static final int SET_BG = 0x0C0C10;

    private interface Click {
        void on(double mx, double my, int button);
    }

    private interface Drag {
        void on(double mx);
    }

    private record Hit(int x, int y, int w, int h, Click click, Drag drag, String tip) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private static final class Panel {
        int x, y;
        boolean open = true;
    }

    // Static so positions / expanded state survive closing and reopening.
    private static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);
    private static final Map<Module, Boolean> EXPANDED = new HashMap<>();
    private static final Map<Module, Float> ANIM = new HashMap<>();

    private final List<Hit> hits = new ArrayList<>();
    private Hit activeSlider;
    private Category draggingPanel;
    private int dragOffX, dragOffY;
    private Module binding;
    private String search = "";
    private int scrollY = 0;
    private final long openedAt = System.currentTimeMillis();

    public ClickGuiScreen() {
        super(Text.literal("Emperor"));
        int i = 0;
        for (Category c : Category.values()) {
            if (!PANELS.containsKey(c)) {
                Panel p = new Panel();
                p.x = 8 + i * (PANEL_W + 6);
                p.y = 12;
                PANELS.put(c, p);
            }
            i++;
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float f = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
        ctx.fill(0, 0, width, height, ((int) (0x78 * f)) << 24);
    }

    private List<Module> modulesOf(Category c) {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase(Locale.ROOT);
        for (Module m : ModuleManager.byCategory(c)) {
            if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
        }
        return out;
    }

    private static int settingHeight(Setting<?> s) {
        return s instanceof NumberSetting ? SLIDER_H : SET_H;
    }

    private int panelHeight(Category c) {
        int h = HEADER_H;
        if (!PANELS.get(c).open) return h;
        for (Module m : modulesOf(c)) {
            h += ROW_H;
            if (EXPANDED.getOrDefault(m, false)) {
                for (Setting<?> s : m.settings) h += settingHeight(s);
            }
        }
        return h + PAD;
    }

    private static int withAlpha(int rgb, float alpha) {
        return Theme.alpha(rgb, (int) (255 * Math.max(0f, Math.min(1f, alpha))));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        hits.clear();
        TextRenderer tr = textRenderer;
        int radius = Theme.radius();
        float op = Theme.opacity();
        int maxH = 0;

        for (Category cat : Category.values()) {
            Panel p = PANELS.get(cat);
            int x = p.x;
            int y = p.y + scrollY;
            int h = panelHeight(cat);
            maxH = Math.max(maxH, h + p.y);

            // body + header
            Rounded.fill(ctx, x, y, x + PANEL_W, y + h, radius, withAlpha(BG, op));
            Rounded.fill(ctx, x, y, x + PANEL_W, y + HEADER_H, radius, withAlpha(HEADER_BG, Math.min(1f, op + 0.05f)), true, !p.open);
            ctx.fill(x + 6, y + HEADER_H - 1, x + PANEL_W - 6, y + HEADER_H, Theme.accent(cat.ordinal() * 4));
            ctx.drawTextWithShadow(tr, cat.label, x + 7, y + 5, 0xFFFFFFFF);
            ctx.drawTextWithShadow(tr, p.open ? "-" : "+", x + PANEL_W - 11, y + 5, 0xFFAAAAAA);

            final Category fc = cat;
            hits.add(new Hit(x, y, PANEL_W, HEADER_H, (mx, my, b) -> {
                if (b == 0) {
                    draggingPanel = fc;
                    dragOffX = (int) mx - p.x;
                    dragOffY = (int) my - (p.y + scrollY);
                } else if (b == 1) {
                    p.open = !p.open;
                }
            }, null, null));

            if (!p.open) continue;
            int ry = y + HEADER_H;

            for (Module m : modulesOf(cat)) {
                boolean hover = mouseX >= x && mouseX < x + PANEL_W && mouseY >= ry && mouseY < ry + ROW_H;
                float target = m.isEnabled() ? 1f : 0f;
                float a = ANIM.getOrDefault(m, 0f);
                a += (target - a) * 0.28f;
                ANIM.put(m, a);

                if (a > 0.02f) {
                    Rounded.fill(ctx, x + 3, ry + 1, x + PANEL_W - 3, ry + ROW_H - 1, Math.max(0, radius - 2),
                            Theme.alpha(Theme.accent(m.name.length()), (int) (0x66 * a)));
                }
                if (hover) {
                    Rounded.fill(ctx, x + 3, ry + 1, x + PANEL_W - 3, ry + ROW_H - 1, Math.max(0, radius - 2), 0x26FFFFFF);
                }
                ctx.drawTextWithShadow(tr, m.name, x + 8, ry + 4, m.isEnabled() ? 0xFFFFFFFF : 0xFFB8B8C0);

                String right;
                if (binding == m) right = "[...]";
                else if (m.getKey() >= 0) right = "[" + keyName(m.getKey()) + "]";
                else right = m.settings.isEmpty() ? "" : (EXPANDED.getOrDefault(m, false) ? "v" : ">");
                ctx.drawTextWithShadow(tr, right, x + PANEL_W - 8 - tr.getWidth(right), ry + 4, 0xFF8A8A96);

                final Module fm = m;
                hits.add(new Hit(x, ry, PANEL_W, ROW_H, (mx, my, b) -> {
                    if (b == 0) fm.toggle();
                    else if (b == 1) EXPANDED.put(fm, !EXPANDED.getOrDefault(fm, false));
                    else if (b == 2) binding = fm;
                }, null, m.description));
                ry += ROW_H;

                if (EXPANDED.getOrDefault(m, false)) {
                    Rounded.fill(ctx, x + 3, ry, x + PANEL_W - 3, ry + m.settings.stream().mapToInt(ClickGuiScreen::settingHeight).sum(),
                            Math.max(0, radius - 2), withAlpha(SET_BG, 0.85f));
                    for (Setting<?> s : m.settings) ry = drawSetting(ctx, tr, s, x, ry);
                }
            }
        }

        // Tooltip
        for (Hit h : hits) {
            if (h.tip() != null && h.contains(mouseX, mouseY)) {
                int w = tr.getWidth(h.tip());
                int tx = Math.min(mouseX + 8, width - w - 10);
                Rounded.fill(ctx, tx, mouseY + 8, tx + w + 8, mouseY + 22, 4, 0xF0101014);
                ctx.drawTextWithShadow(tr, h.tip(), tx + 4, mouseY + 11, 0xFFFFFFFF);
                break;
            }
        }

        // Search bar
        int sw = 170;
        int sx = (width - sw) / 2;
        int sy = height - 34;
        Rounded.fill(ctx, sx, sy, sx + sw, sy + 16, 6, 0xC0141418);
        String shown = search.isEmpty() ? "Type to search modules..." : search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        ctx.drawTextWithShadow(tr, shown, sx + 7, sy + 4, search.isEmpty() ? 0xFF777780 : 0xFFFFFFFF);

        String help = "LMB toggle  |  RMB settings  |  MMB bind key  |  Backspace clear bind  |  Wheel scroll  |  RShift close";
        ctx.drawTextWithShadow(tr, help, (width - tr.getWidth(help)) / 2, height - 12, 0xFF77778A);

        // clamp scroll once we know the tallest panel
        int minScroll = Math.min(0, height - 50 - maxH);
        if (scrollY < minScroll) scrollY = minScroll;
    }

    private int drawSetting(DrawContext ctx, TextRenderer tr, Setting<?> s, int x, int y) {
        int radius = Math.max(0, Theme.radius() - 2);
        int accent = Theme.main();

        if (s instanceof BoolSetting bs) {
            ctx.drawTextWithShadow(tr, bs.name, x + 9, y + 3, 0xFFCCCCD4);
            int bx = x + PANEL_W - 25;
            Rounded.fill(ctx, bx, y + 3, bx + 16, y + 10, 3, bs.get() ? accent : 0xFF2C2C38);
            int kx = bs.get() ? bx + 10 : bx + 1;
            Rounded.fill(ctx, kx, y + 4, kx + 5, y + 9, 2, 0xFFFFFFFF);
            hits.add(new Hit(x, y, PANEL_W, SET_H, (mx, my, b) -> bs.toggle(), null, null));
        } else if (s instanceof ModeSetting ms) {
            ctx.drawTextWithShadow(tr, ms.name, x + 9, y + 3, 0xFFCCCCD4);
            String v = ms.get();
            ctx.drawTextWithShadow(tr, v, x + PANEL_W - 9 - tr.getWidth(v), y + 3, accent);
            hits.add(new Hit(x, y, PANEL_W, SET_H, (mx, my, b) -> ms.next(), null, null));
        } else if (s instanceof NumberSetting ns) {
            double frac = (ns.d() - ns.min) / (ns.max - ns.min);
            int tx = x + 9;
            int tw = PANEL_W - 18;
            ctx.drawTextWithShadow(tr, ns.name, x + 9, y + 3, 0xFFCCCCD4);
            String v = ns.step >= 1 ? String.format(Locale.ROOT, "%.0f", ns.d()) : String.format(Locale.ROOT, "%.2f", ns.d());
            ctx.drawTextWithShadow(tr, v, x + PANEL_W - 9 - tr.getWidth(v), y + 3, 0xFFFFFFFF);
            Rounded.fill(ctx, tx, y + 14, tx + tw, y + 17, 1, 0xFF2C2C38);
            int fw = (int) (tw * frac);
            if (fw > 0) Rounded.fill(ctx, tx, y + 14, tx + fw, y + 17, 1, accent);
            Rounded.fill(ctx, tx + fw - 2, y + 12, tx + fw + 2, y + 19, 2, 0xFFFFFFFF);
            Drag drag = mx -> {
                double f = Math.max(0, Math.min(1, (mx - tx) / tw));
                ns.set(ns.min + f * (ns.max - ns.min));
            };
            hits.add(new Hit(x, y, PANEL_W, SLIDER_H, (mx, my, b) -> {
                if (b == 0) drag.on(mx);
            }, drag, null));
        }
        return y + settingHeight(s);
    }

    private static String keyName(int key) {
        try {
            return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "?";
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        binding = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mouseX, mouseY)) {
                h.click().on(mouseX, mouseY, button);
                if (h.drag() != null && button == 0) activeSlider = h;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingPanel != null) {
            Panel p = PANELS.get(draggingPanel);
            p.x = (int) mouseX - dragOffX;
            p.y = (int) mouseY - dragOffY - scrollY;
            return true;
        }
        if (activeSlider != null) {
            activeSlider.drag().on(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingPanel = null;
        activeSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollY = Math.min(0, scrollY + (int) (verticalAmount * 18));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null) return true;
        if (chr >= 32 && chr != 127 && search.length() < 24) {
            search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                binding.setKey(-1);
            } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                binding.setKey(keyCode);
            }
            binding = null;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }
}
