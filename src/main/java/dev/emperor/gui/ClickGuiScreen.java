package dev.emperor.gui;

import dev.emperor.EmperorClient;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.modules.ClickGuiModule;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ModeSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.setting.Setting;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Animated, blurred, gradient ClickGUI with draggable panels, search and smooth settings. */
public class ClickGuiScreen extends Screen {
    // Shrinks on narrow screens so all six panels fit side by side.
    private static int PANEL_W = 128;
    private static final int GAP = 10;
    private static final int HEADER_H = 22;
    private static final int ROW_H = 17;
    private static final int SET_H = 15;
    private static final int SLIDER_H = 22;
    private static final int PAD = 5;
    private static final int TOP = 54;

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
        float anim = 1f;
    }

    // Static so positions / expanded state survive closing and reopening.
    private static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);
    private static final Map<Module, Boolean> EXPANDED = new HashMap<>();
    private static final Map<Object, Float> ANIM = new HashMap<>();
    private static boolean laidOut;
    private static int laidOutWidth;
    private static float[][] particles;

    private final List<Hit> hits = new ArrayList<>();
    private Hit activeSlider;
    private Category draggingPanel;
    private int dragOffX, dragOffY;
    private Module binding;
    private String search = "";
    private int scrollY = 0;
    private final long openedAt = System.currentTimeMillis();
    private boolean closing;
    private long closingAt;
    private long lastFrame = System.nanoTime();
    private float dt;
    private float fade = 1f;
    private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;
    private String hoverTip;
    private long hoverTipSince;

    public ClickGuiScreen() {
        super(Text.literal("Emperor"));
    }

    @Override
    protected void init() {
        Category[] cats = Category.values();
        if (!laidOut || PANELS.size() != cats.length || laidOutWidth != width) {
            PANEL_W = Math.max(100, Math.min(128, (width - 16 - (cats.length - 1) * GAP) / cats.length));
            int total = cats.length * PANEL_W + (cats.length - 1) * GAP;
            int startX = Math.max(8, (width - total) / 2);
            for (int i = 0; i < cats.length; i++) {
                Panel p = PANELS.computeIfAbsent(cats[i], c -> new Panel());
                p.x = startX + i * (PANEL_W + GAP);
                p.y = TOP;
            }
            laidOut = true;
            laidOutWidth = width;
        }
        if (particles == null) {
            Random r = new Random();
            particles = new float[80][];
            for (int i = 0; i < particles.length; i++) {
                particles[i] = new float[]{r.nextFloat(), r.nextFloat(), (r.nextFloat() - 0.5f) * 0.01f,
                        -(0.008f + r.nextFloat() * 0.02f), r.nextFloat() < 0.3f ? 2 : 1, r.nextFloat() * 6.28f};
            }
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static ClickGuiModule cfg() {
        return ModuleManager.get(ClickGuiModule.class);
    }

    private void sound(float pitch) {
        ClickGuiModule g = cfg();
        if (g != null && g.sounds.get() && client != null) {
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25f));
        }
    }

    private float anim(Object key, float target, float speed) {
        float v = Fx.approach(ANIM.getOrDefault(key, target == 1f ? 0f : target), target, dt, speed);
        ANIM.put(key, v);
        return v;
    }

    private float peek(Object key) {
        return ANIM.getOrDefault(key, 0f);
    }

    private int c(int argb) {
        return Theme.fade(argb, fade);
    }

    private void hit(int x, int y, int w, int h, Click click, Drag drag, String tip) {
        int top = Math.max(y, clipTop);
        int bottom = Math.min(y + h, clipBottom);
        if (bottom - top < 2) return;
        hits.add(new Hit(x, top, w, bottom - top, click, drag, tip));
    }

    // ------------------------------------------------------------------ background

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ClickGuiModule g = cfg();
        if (g != null && g.blur.get() && client != null && client.world != null) applyBlur();
        int top = Theme.alpha(0x06060A, (int) (0x90 * fade));
        int bottom = Theme.alpha(Theme.lerp(0xFF06060A, Theme.secondary(), 0.22f), (int) (0xC8 * fade));
        ctx.fillGradient(0, 0, width, height, top, bottom);
    }

    private void renderParticles(DrawContext ctx) {
        ClickGuiModule g = cfg();
        if (g != null && !g.particles.get()) return;
        long now = System.currentTimeMillis();
        for (float[] p : particles) {
            p[0] += p[2] * dt;
            p[1] += p[3] * dt;
            if (p[1] < -0.02f) {
                p[1] = 1.02f;
                p[0] = (float) Math.random();
            }
            if (p[0] < -0.02f) p[0] = 1.02f;
            if (p[0] > 1.02f) p[0] = -0.02f;
            float tw = 0.35f + 0.35f * (float) Math.sin(now / 600.0 + p[5]);
            int x = (int) (p[0] * width), y = (int) (p[1] * height), s = (int) p[4];
            int col = Theme.alpha(Theme.accent((int) (p[5] * 8)), (int) (255 * tw * fade));
            if (s > 1) Rounded.fill(ctx, x - 1, y - 1, x + 2, y + 2, 1, Theme.fade(col, 0.35f));
            ctx.fill(x, y, x + 1, y + 1, col);
        }
    }

    private void renderTitle(DrawContext ctx, TextRenderer tr) {
        float a = fade;
        if (Fx.invisible(a)) return;
        String title = "EMPEROR";
        int cx = width / 2;
        float scale = 2.2f;
        int tw = (int) (tr.getWidth(Text.literal("♛ " + title).formatted(Formatting.BOLD)) * scale);
        int ty = 10;

        Rounded.shadow(ctx, cx - tw / 2 + 10, ty + 6, cx + tw / 2 - 10, ty + 16, 8, 14,
                Theme.alpha(Theme.main(), (int) (60 * a)));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx - tw / 2f, ty, 0);
        ctx.getMatrices().scale(scale, scale, 1f);
        int x = 0;
        Text crown = Text.literal("♛ ").formatted(Formatting.BOLD);
        ctx.drawTextWithShadow(tr, crown, x, 0, Theme.fade(Theme.primary(), a));
        x += tr.getWidth(crown);
        for (int i = 0; i < title.length(); i++) {
            Text ch = Text.literal(String.valueOf(title.charAt(i))).formatted(Formatting.BOLD);
            ctx.drawTextWithShadow(tr, ch, x, 0, Theme.fade(Theme.accent(i * 2), a));
            x += tr.getWidth(ch);
        }
        ctx.getMatrices().pop();

        String sub = "C L I E N T   ·   v" + EmperorClient.VERSION;
        ctx.drawCenteredTextWithShadow(tr, sub, cx, ty + 24, Theme.fade(0xFF8C8C9A, a));
        Rounded.gradientH(ctx, cx - 60, ty + 35, cx, ty + 36, 0, Theme.alpha(Theme.primary(), 0), Theme.fade(Theme.primary(), a));
        Rounded.gradientH(ctx, cx, ty + 35, cx + 60, ty + 36, 0, Theme.fade(Theme.secondary(), a), Theme.alpha(Theme.secondary(), 0));
    }

    // ------------------------------------------------------------------ layout helpers

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

    private static int settingsHeight(Module m) {
        int h = 2;
        for (Setting<?> s : m.settings) h += settingHeight(s);
        return h + 2;
    }

    private int bodyHeight(Category c) {
        int h = 2;
        for (Module m : modulesOf(c)) {
            h += ROW_H;
            h += (int) (settingsHeight(m) * peek(expandKey(m)));
        }
        return h + PAD;
    }

    private static Object expandKey(Module m) {
        return List.of("expand", m);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long nowNs = System.nanoTime();
        dt = Math.min(0.1f, (nowNs - lastFrame) / 1e9f);
        lastFrame = nowNs;
        long now = System.currentTimeMillis();

        float openT = Fx.clamp01((now - openedAt) / 450f);
        if (closing) {
            fade = 1f - Fx.easeOutCubic((now - closingAt) / 160f);
            if (fade <= 0.01f) {
                closing = false;
                super.close();
                return;
            }
        } else {
            fade = Fx.easeOutCubic(Math.min(1f, (now - openedAt) / 220f));
        }

        super.render(ctx, mouseX, mouseY, delta);
        hits.clear();
        TextRenderer tr = textRenderer;

        renderParticles(ctx);
        renderTitle(ctx, tr);

        int maxBottom = 0;
        float globalFade = fade;
        Category[] cats = Category.values();
        for (int i = 0; i < cats.length; i++) {
            Category cat = cats[i];
            Panel p = PANELS.get(cat);
            float pa = closing ? 1f : Fx.clamp01(openT * 1.9f - i * 0.11f);
            int slide = (int) ((1f - Fx.easeOutBack(pa)) * -26);
            fade = globalFade * Fx.clamp01(pa * 1.6f);
            int bottom = drawPanel(ctx, tr, cat, p, p.x, p.y + scrollY + slide, mouseX, mouseY);
            maxBottom = Math.max(maxBottom, bottom - scrollY);
        }
        fade = globalFade;

        drawTooltip(ctx, tr, mouseX, mouseY, now);
        drawSearch(ctx, tr, now);

        String help = "LMB toggle  ·  RMB settings  ·  MMB bind  ·  Scroll  ·  Type to search  ·  RShift close";
        ctx.drawCenteredTextWithShadow(tr, help, width / 2, height - 12, c(0xFF5E5E70));

        int minScroll = Math.min(0, height - 60 - maxBottom);
        if (scrollY < minScroll) scrollY = minScroll;
    }

    private int drawPanel(DrawContext ctx, TextRenderer tr, Category cat, Panel p, int x, int y, int mx, int my) {
        p.anim = Fx.approach(p.anim, p.open ? 1f : 0f, dt, 14f);
        int r = Theme.radius();
        float op = Theme.opacity();
        int body = (int) (bodyHeight(cat) * p.anim);
        int h = HEADER_H + body;
        List<Module> mods = modulesOf(cat);
        boolean dimmed = !search.isEmpty() && mods.isEmpty();
        if (dimmed) fade *= 0.35f;

        // shadow, faint accent glow, glass body
        Rounded.shadow(ctx, x, y, x + PANEL_W, y + h, r, 9, c(0x78000000));
        Rounded.shadow(ctx, x, y, x + PANEL_W, y + h, r, 3, c(Theme.alpha(Theme.main(), 0x30)));
        Rounded.fill(ctx, x, y, x + PANEL_W, y + h, r, Theme.alpha(0x0D0D13, (int) (255 * op * fade)));

        // gradient header with a soft sheen
        boolean flat = body > 1;
        Rounded.gradientH(ctx, x, y, x + PANEL_W, y + HEADER_H, r,
                c(Theme.alpha(Theme.gradient(0f), 0xE6)), c(Theme.alpha(Theme.gradient(1f), 0xE6)), true, !flat);
        Rounded.fill(ctx, x, y, x + PANEL_W, y + HEADER_H / 2, r, c(0x1CFFFFFF), true, false);
        ctx.fill(x, y + HEADER_H - 1, x + PANEL_W, y + HEADER_H, c(0x30000000));

        if (!Fx.invisible(fade)) {
            ctx.drawTextWithShadow(tr, cat.icon, x + 8, y + 7, c(0xFFFFFFFF));
            ctx.drawTextWithShadow(tr, Text.literal(cat.label).formatted(Formatting.BOLD), x + 21, y + 7, c(0xFFFFFFFF));
            int on = 0;
            for (Module m : ModuleManager.byCategory(cat)) if (m.isEnabled()) on++;
            String cnt = on + "/" + ModuleManager.byCategory(cat).size();
            int cw = tr.getWidth(cnt) + 8;
            Rounded.fill(ctx, x + PANEL_W - 7 - cw, y + 5, x + PANEL_W - 7, y + 17, 5, c(0x50000000));
            ctx.drawTextWithShadow(tr, cnt, x + PANEL_W - 3 - cw, y + 7, c(0xFFFFFFFF));
        }
        Rounded.outline(ctx, x, y, x + PANEL_W, y + h, r, c(Theme.alpha(Theme.main(), 0x38)));

        hit(x, y, PANEL_W, HEADER_H, (cx, cy, b) -> {
            if (b == 0) {
                draggingPanel = cat;
                dragOffX = (int) cx - p.x;
                dragOffY = (int) cy - (p.y + scrollY);
            } else if (b == 1) {
                p.open = !p.open;
                sound(p.open ? 1.2f : 0.9f);
            }
        }, null, null);

        if (body > 0) {
            int top = y + HEADER_H;
            int prevTop = clipTop, prevBottom = clipBottom;
            clipTop = Math.max(clipTop, top);
            clipBottom = Math.min(clipBottom, top + body);
            ctx.enableScissor(x, top, x + PANEL_W, top + body);
            int ry = top + 2;
            for (Module m : mods) ry = drawModule(ctx, tr, m, x, ry, mx, my);
            ctx.disableScissor();
            clipTop = prevTop;
            clipBottom = prevBottom;
        }
        if (dimmed) fade /= 0.35f;
        return y + h;
    }

    private int drawModule(DrawContext ctx, TextRenderer tr, Module m, int x, int ry, int mx, int my) {
        int r = Math.max(0, Theme.radius() - 2);
        boolean hover = mx >= x + 4 && mx < x + PANEL_W - 4 && my >= ry && my < ry + ROW_H
                && my >= clipTop && my < clipBottom;
        float t = anim(List.of("on", m), m.isEnabled() ? 1f : 0f, 12f);
        float hv = anim(List.of("hover", m), hover ? 1f : 0f, 18f);

        if (hv > 0.01f) Rounded.fill(ctx, x + 4, ry, x + PANEL_W - 4, ry + ROW_H - 1, r, c(Theme.alpha(0xFFFFFF, (int) (20 * hv))));
        if (t > 0.01f) {
            int fw = (int) ((PANEL_W - 8) * Fx.easeOutCubic(t));
            Rounded.gradientH(ctx, x + 4, ry, x + 4 + fw, ry + ROW_H - 1, r,
                    c(Theme.alpha(Theme.accent(0), (int) (0x78 * t))), c(Theme.alpha(Theme.accent(10), (int) (0x30 * t))));
            int bh = (int) ((ROW_H - 7) * t);
            int mid = ry + (ROW_H - 1) / 2;
            Rounded.fill(ctx, x + 5, mid - bh / 2, x + 7, mid + bh / 2 + 1, 1, c(Theme.primary()));
        }

        if (!Fx.invisible(fade)) {
            int nameCol = Theme.lerp(0xFF8E8EA0, 0xFFFFFFFF, Math.max(t, hv * 0.7f));
            ctx.drawTextWithShadow(tr, m.name, x + 11 + (int) (2 * t), ry + 5, c(nameCol));

            int right = x + PANEL_W - 9;
            if (binding == m) {
                String s = (System.currentTimeMillis() / 300) % 2 == 0 ? "press a key" : "press a key_";
                ctx.drawTextWithShadow(tr, s, right - tr.getWidth("press a key_"), ry + 5, c(Theme.primary()));
            } else {
                if (!m.settings.isEmpty()) {
                    float e = peek(expandKey(m));
                    String arrow = e > 0.5f ? "▾" : "▸";
                    ctx.drawTextWithShadow(tr, arrow, right - tr.getWidth(arrow), ry + 5, c(Theme.lerp(0xFF6A6A7C, Theme.primary(), e)));
                    right -= tr.getWidth(arrow) + 4;
                }
                if (m.getKey() >= 0) {
                    String k = keyName(m.getKey());
                    int kw = tr.getWidth(k);
                    Rounded.fill(ctx, right - kw - 6, ry + 3, right, ry + ROW_H - 4, 3, c(0x60000000));
                    ctx.drawTextWithShadow(tr, k, right - kw - 3, ry + 5, c(0xFFB0B0C0));
                }
            }
        }

        hit(x, ry, PANEL_W, ROW_H, (cx, cy, b) -> {
            if (b == 0) {
                m.toggle();
                sound(m.isEnabled() ? 1.4f : 1.0f);
            } else if (b == 1 && !m.settings.isEmpty()) {
                EXPANDED.put(m, !EXPANDED.getOrDefault(m, false));
                sound(1.2f);
            } else if (b == 2) {
                binding = m;
                sound(1.6f);
            }
        }, null, m.description);
        ry += ROW_H;

        float ex = anim(expandKey(m), EXPANDED.getOrDefault(m, false) ? 1f : 0f, 14f);
        if (ex > 0.005f && !m.settings.isEmpty()) {
            int full = settingsHeight(m);
            int vis = (int) (full * ex);
            int prevTop = clipTop, prevBottom = clipBottom;
            clipTop = Math.max(clipTop, ry);
            clipBottom = Math.min(clipBottom, ry + vis);
            ctx.enableScissor(x, ry, x + PANEL_W, ry + vis);
            Rounded.fill(ctx, x + 4, ry, x + PANEL_W - 4, ry + full, r, c(0x48000000));
            ctx.fill(x + 4, ry + 2, x + 5, ry + full - 2, c(Theme.alpha(Theme.primary(), 0x90)));
            int sy = ry + 2;
            for (Setting<?> s : m.settings) sy = drawSetting(ctx, tr, s, x, sy, mx, my);
            ctx.disableScissor();
            clipTop = prevTop;
            clipBottom = prevBottom;
            ry += vis;
        }
        return ry;
    }

    private int drawSetting(DrawContext ctx, TextRenderer tr, Setting<?> s, int x, int y, int mx, int my) {
        int left = x + 11;
        int right = x + PANEL_W - 10;
        boolean text = !Fx.invisible(fade);

        if (s instanceof BoolSetting bs) {
            float t = anim(bs, bs.get() ? 1f : 0f, 16f);
            if (text) ctx.drawTextWithShadow(tr, bs.name, left, y + 4, c(0xFFC4C4D0));
            int tx = right - 18, ty = y + 3;
            int track = Theme.lerp(0xFF2A2A36, Theme.primary(), t);
            Rounded.fill(ctx, tx, ty, tx + 18, ty + 9, 4, c(track));
            if (t > 0.05f) Rounded.shadow(ctx, tx, ty, tx + 18, ty + 9, 4, 2, c(Theme.alpha(Theme.primary(), (int) (0x50 * t))));
            int kx = (int) (tx + 1 + 9 * t);
            Rounded.fill(ctx, kx, ty + 1, kx + 7, ty + 8, 3, c(0xFFFFFFFF));
            hit(x, y, PANEL_W, SET_H, (cx, cy, b) -> {
                bs.toggle();
                sound(bs.get() ? 1.5f : 1.1f);
            }, null, null);
        } else if (s instanceof ModeSetting ms) {
            if (text) {
                ctx.drawTextWithShadow(tr, ms.name, left, y + 4, c(0xFFC4C4D0));
                String v = ms.get();
                int vw = tr.getWidth(v);
                ctx.drawTextWithShadow(tr, "›", right - tr.getWidth("›"), y + 4, c(0xFF6A6A7C));
                ctx.drawTextWithShadow(tr, v, right - 6 - vw, y + 4, c(Theme.accent(3)));
                ctx.drawTextWithShadow(tr, "‹", right - 10 - vw - tr.getWidth("‹"), y + 4, c(0xFF6A6A7C));
            }
            hit(x, y, PANEL_W, SET_H, (cx, cy, b) -> {
                if (b == 1) ms.prev();
                else ms.next();
                sound(1.3f);
            }, null, null);
        } else if (s instanceof NumberSetting ns) {
            float target = (float) ((ns.d() - ns.min) / (ns.max - ns.min));
            float frac = anim(ns, target, 20f);
            int tx = left, tw = right - left;
            boolean hover = mx >= x && mx < x + PANEL_W && my >= y && my < y + SLIDER_H;
            if (text) {
                ctx.drawTextWithShadow(tr, ns.name, left, y + 3, c(0xFFC4C4D0));
                String v = ns.step >= 1 ? String.format(Locale.ROOT, "%.0f", ns.d())
                        : ns.step >= 0.1 ? String.format(Locale.ROOT, "%.1f", ns.d())
                        : String.format(Locale.ROOT, "%.2f", ns.d());
                ctx.drawTextWithShadow(tr, v, right - tr.getWidth(v), y + 3, c(0xFFFFFFFF));
            }
            int by = y + 15;
            Rounded.fill(ctx, tx, by, tx + tw, by + 3, 1, c(0xFF2A2A36));
            int fw = (int) (tw * Fx.clamp01(frac));
            if (fw > 0) Rounded.gradientH(ctx, tx, by, tx + fw, by + 3, 1, c(Theme.primary()), c(Theme.secondary()));
            int kx = tx + fw;
            if (hover) Rounded.circle(ctx, kx, by + 1, 5, c(Theme.alpha(Theme.primary(), 0x50)));
            Rounded.circle(ctx, kx, by + 1, 3, c(0xFFFFFFFF));
            Drag drag = cx -> {
                double f = Math.max(0, Math.min(1, (cx - tx) / tw));
                ns.set(ns.min + f * (ns.max - ns.min));
            };
            hit(x, y, PANEL_W, SLIDER_H, (cx, cy, b) -> {
                if (b == 0) drag.on(cx);
            }, drag, null);
        }
        return y + settingHeight(s);
    }

    private void drawTooltip(DrawContext ctx, TextRenderer tr, int mx, int my, long now) {
        String tip = null;
        if (draggingPanel == null && activeSlider == null) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (h.contains(mx, my)) {
                    tip = h.tip();
                    break;
                }
            }
        }
        if (tip == null || !tip.equals(hoverTip)) {
            hoverTip = tip;
            hoverTipSince = now;
        }
        if (tip == null) return;
        float a = Fx.clamp01((now - hoverTipSince - 250) / 150f) * fade;
        if (Fx.invisible(a)) return;
        int w = tr.getWidth(tip) + 12;
        int x = Math.min(mx + 10, width - w - 4);
        int y = my + 10;
        Rounded.shadow(ctx, x, y, x + w, y + 16, 5, 4, Theme.alpha(0, (int) (90 * a)));
        Rounded.fill(ctx, x, y, x + w, y + 16, 5, Theme.fade(0xF20E0E14, a));
        Rounded.outline(ctx, x, y, x + w, y + 16, 5, Theme.alpha(Theme.main(), (int) (110 * a)));
        ctx.drawTextWithShadow(tr, tip, x + 6, y + 4, Theme.fade(0xFFE6E6EE, a));
    }

    private void drawSearch(DrawContext ctx, TextRenderer tr, long now) {
        int sw = 200, sh = 18;
        int sx = (width - sw) / 2;
        int sy = height - 40;
        float focus = anim("search", search.isEmpty() ? 0f : 1f, 12f);
        Rounded.shadow(ctx, sx, sy, sx + sw, sy + sh, 8, 5, c(Theme.alpha(Theme.main(), (int) (0x18 + 0x30 * focus))));
        Rounded.fill(ctx, sx, sy, sx + sw, sy + sh, 8, c(0xE00E0E14));
        Rounded.outline(ctx, sx, sy, sx + sw, sy + sh, 8, c(Theme.alpha(Theme.main(), (int) (0x40 + 0x80 * focus))));
        if (Fx.invisible(fade)) return;
        ctx.drawTextWithShadow(tr, "⌕", sx + 8, sy + 5, c(Theme.primary()));
        boolean blink = (now / 500) % 2 == 0;
        String shown = search.isEmpty() ? "Search modules..." : search + (blink ? "_" : "");
        ctx.drawTextWithShadow(tr, shown, sx + 20, sy + 5, c(search.isEmpty() ? 0xFF6A6A7C : 0xFFFFFFFF));
        if (!search.isEmpty()) {
            int n = 0;
            for (Category cat : Category.values()) n += modulesOf(cat).size();
            String res = n + (n == 1 ? " result" : " results");
            ctx.drawTextWithShadow(tr, res, sx + sw - 8 - tr.getWidth(res), sy + 5, c(0xFF6A6A7C));
        }
    }

    private static String keyName(int key) {
        try {
            return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        } catch (Exception e) {
            return "?";
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
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
            p.x = Math.max(-PANEL_W + 20, Math.min(width - 20, (int) mouseX - dragOffX));
            p.y = Math.max(0, (int) mouseY - dragOffY - scrollY);
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
        scrollY = Math.min(0, scrollY + (int) (verticalAmount * 20));
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
            sound(1.8f);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) {
            search = "";
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;
            closingAt = System.currentTimeMillis();
        }
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }
}
