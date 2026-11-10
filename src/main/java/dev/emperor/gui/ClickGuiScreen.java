package dev.emperor.gui;

import dev.emperor.EmperorClient;
import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.modules.ClickGuiModule;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.ColorSetting;
import dev.emperor.hud.Notifications;
import dev.emperor.util.Keys;
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
    private Module lastBinding;
    // The key that completes a bind also produces a char event; drop it so it does not land in search.
    private boolean swallowChar;
    private String search = "";
    private int scrollY = 0;
    private int scrollTarget = 0;
    private float scrollAnim = 0f;
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
        // Favourites float to the top, otherwise registration order is kept (stable sort).
        out.sort((a, b) -> Boolean.compare(b.favorite, a.favorite));
        return out;
    }

    private static final int DESC_H = 13;
    /** Description + Bind + Mode + Show-in-list rows shown above every module's own settings. */
    private static final int META_H = DESC_H + SET_H * 3;
    private static final int COLOR_OPEN_H = SET_H + 36;
    private static final Map<ColorSetting, Boolean> COLOR_OPEN = new HashMap<>();

    private int settingHeight(Setting<?> s) {
        if (s instanceof ColorSetting cs) return (int) (SET_H + (COLOR_OPEN_H - SET_H) * peek(List.of("color", cs)));
        return s instanceof NumberSetting ? SLIDER_H : SET_H;
    }

    private int settingsHeight(Module m) {
        int h = 2 + META_H;
        for (Setting<?> s : m.settings) h += settingHeight(s);
        return h + 3;
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
        scrollAnim = Fx.approach(scrollAnim, scrollTarget, dt, 16f);
        scrollY = Math.round(scrollAnim);

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
        // Panels scroll inside the area above the search bar and dock.
        int viewBottom = height - 70;
        clipBottom = viewBottom;
        ctx.enableScissor(0, 0, width, viewBottom);
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
        ctx.disableScissor();
        clipBottom = Integer.MAX_VALUE;
        fade = globalFade;
        if (maxBottom + scrollY > viewBottom) {
            ctx.fillGradient(0, viewBottom - 14, width, viewBottom, 0x00000000, c(0x70000000));
        }

        drawSearch(ctx, tr, now);
        drawDock(ctx, tr, mouseX, mouseY);
        drawRipples(ctx, now);
        drawTooltip(ctx, tr, mouseX, mouseY, now);

        String help = "LMB toggle  ·  RMB settings  ·  MMB bind  ·  Scroll  ·  Type to search  ·  RShift close";
        ctx.drawCenteredTextWithShadow(tr, help, width / 2, height - 11, c(0xFF5E5E70));

        int minScroll = Math.min(0, viewBottom - 8 - maxBottom);
        if (scrollTarget < minScroll) scrollTarget = minScroll;
    }

    // ------------------------------------------------------------------ dock & ripples

    private record DockButton(String icon, String label, Click action) {
    }

    private void drawDock(DrawContext ctx, TextRenderer tr, int mx, int my) {
        ClickGuiModule g = cfg();
        List<DockButton> buttons = List.of(
                new DockButton("✎", "HUD Editor", (x, y, b) -> client.setScreen(new HudEditorScreen(this))),
                new DockButton("⌕", "Spotlight", (x, y, b) -> client.setScreen(new SpotlightScreen())),
                new DockButton("⚙", "Binds", (x, y, b) -> client.setScreen(new KeybindsScreen(this))),
                new DockButton("◐", "Theme: " + (g == null ? "-" : g.theme.get()), (x, y, b) -> {
                    if (g == null) return;
                    if (b == 1) g.theme.prev();
                    else g.theme.next();
                }),
                new DockButton("⟲", "Reset layout", (x, y, b) -> {
                    laidOut = false;
                    init();
                }));
        int bh = 18, gap = 4, pad = 4;
        int total = pad * 2 - gap;
        for (DockButton d : buttons) total += tr.getWidth(d.icon + "  " + d.label) + 16 + gap;
        int x = (width - total) / 2, y = height - 40;
        Rounded.shadow(ctx, x, y, x + total, y + bh + pad * 2, 9, 6, c(0x60000000));
        Rounded.fill(ctx, x, y, x + total, y + bh + pad * 2, 9, c(0xE00E0E14));
        Rounded.outline(ctx, x, y, x + total, y + bh + pad * 2, 9, c(Theme.alpha(Theme.main(), 0x50)));
        int bx = x + pad;
        for (int i = 0; i < buttons.size(); i++) {
            DockButton d = buttons.get(i);
            int bw = tr.getWidth(d.icon + "  " + d.label) + 16;
            int by = y + pad;
            boolean hover = mx >= bx && mx < bx + bw && my >= by && my < by + bh;
            float hv = anim(List.of("dock", i), hover ? 1f : 0f, 18f);
            if (hv > 0.01f) Rounded.gradientH(ctx, bx, by, bx + bw, by + bh, 6,
                    c(Theme.alpha(Theme.primary(), (int) (0x70 * hv))), c(Theme.alpha(Theme.secondary(), (int) (0x70 * hv))));
            else Rounded.fill(ctx, bx, by, bx + bw, by + bh, 6, c(0x30FFFFFF));
            if (!Fx.invisible(fade)) {
                ctx.drawTextWithShadow(tr, d.icon, bx + 8, by + 5, c(Theme.lerp(Theme.primary(), 0xFFFFFFFF, hv)));
                ctx.drawTextWithShadow(tr, d.label, bx + 8 + tr.getWidth(d.icon + "  "), by + 5, c(0xFFE6E6EE));
            }
            DockButton fd = d;
            hit(bx, by, bw, bh, (cx, cy, b) -> {
                fd.action.on(cx, cy, b);
                sound(1.3f);
            }, null, null);
            bx += bw + gap;
        }
    }

    private static final class Ripple {
        final int x, y;
        final long start = System.currentTimeMillis();

        Ripple(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private final List<Ripple> ripples = new ArrayList<>();

    private void drawRipples(DrawContext ctx, long now) {
        ripples.removeIf(r -> now - r.start > 450);
        for (Ripple r : ripples) {
            float t = (now - r.start) / 450f;
            int rad = (int) (3 + Fx.easeOutCubic(t) * 16);
            int col = Theme.alpha(Theme.primary(), (int) (200 * (1 - t)));
            Rounded.outline(ctx, r.x - rad, r.y - rad, r.x + rad + 1, r.y + rad + 1, rad, col);
            if (t < 0.4f) Rounded.circle(ctx, r.x, r.y, (int) (rad * 0.5f), Theme.alpha(Theme.primary(), (int) (90 * (1 - t / 0.4f))));
        }
    }

    /** Opens the menu with a module searched and its settings expanded. */
    public static ClickGuiScreen focus(Module m) {
        ClickGuiScreen s = new ClickGuiScreen();
        s.search = m.name.toLowerCase(Locale.ROOT);
        EXPANDED.put(m, true);
        return s;
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
            int nx = x + 11 + (int) (2 * t);
            if (m.favorite) {
                ctx.drawTextWithShadow(tr, "★", nx, ry + 5, c(0xFFFFD36E));
                nx += tr.getWidth("★ ");
            }
            drawName(ctx, tr, m.name, nx, ry + 5, c(nameCol));

            int right = x + PANEL_W - 9;
            float e = peek(expandKey(m));
            String arrow = e > 0.5f ? "▾" : "▸";
            ctx.drawTextWithShadow(tr, arrow, right - tr.getWidth(arrow), ry + 5, c(Theme.lerp(0xFF6A6A7C, Theme.primary(), e)));
            right -= tr.getWidth(arrow) + 4;
            if (binding == m) {
                String s = "...";
                int kw = tr.getWidth(s);
                Rounded.fill(ctx, right - kw - 6, ry + 3, right, ry + ROW_H - 4, 3, c(Theme.alpha(Theme.primary(), 0x90)));
                ctx.drawTextWithShadow(tr, s, right - kw - 3, ry + 5, c(0xFFFFFFFF));
            } else if (m.getKey() >= 0) {
                String k = Keys.name(m.getKey()) + (m.holdMode ? " ⏷" : "");
                int kw = tr.getWidth(k);
                Rounded.fill(ctx, right - kw - 6, ry + 3, right, ry + ROW_H - 4, 3, c(0x60000000));
                ctx.drawTextWithShadow(tr, k, right - kw - 3, ry + 5, c(0xFFB0B0C0));
                right -= kw + 9;
            }
            if (hv > 0.05f && !m.favorite && binding != m) {
                // hover star to pin the module
                int sx = right - tr.getWidth("☆");
                ctx.drawTextWithShadow(tr, "☆", sx, ry + 5, c(Theme.alpha(0xFFFFD36E, (int) (0xB0 * hv))));
            }
        }

        hit(x, ry, PANEL_W, ROW_H, (cx, cy, b) -> {
            if (b == 0) {
                m.toggle();
                sound(m.isEnabled() ? 1.4f : 1.0f);
            } else if (b == 1) {
                EXPANDED.put(m, !EXPANDED.getOrDefault(m, false));
                sound(1.2f);
            } else if (b == 2) {
                binding = m;
                sound(1.6f);
            }
        }, null, m.description);
        // star hit-box (added after the row so it wins)
        if (hover || m.favorite) {
            int starX = x + PANEL_W - 9 - tr.getWidth("▸") - 4;
            if (m.getKey() >= 0 && binding != m) starX -= tr.getWidth(Keys.name(m.getKey()) + (m.holdMode ? " ⏷" : "")) + 9;
            int fx = m.favorite ? x + 9 + (int) (2 * t) : starX - tr.getWidth("☆") - 1;
            hit(fx, ry + 2, tr.getWidth("☆") + 3, ROW_H - 4, (cx, cy, b) -> {
                if (b != 0) return;
                m.favorite = !m.favorite;
                sound(m.favorite ? 1.8f : 1.0f);
            }, null, m.favorite ? "Unpin from top" : "Pin to top");
        }
        ry += ROW_H;

        float ex = anim(expandKey(m), EXPANDED.getOrDefault(m, false) ? 1f : 0f, 14f);
        if (ex > 0.005f) {
            int full = settingsHeight(m);
            int vis = (int) (full * ex);
            int prevTop = clipTop, prevBottom = clipBottom;
            clipTop = Math.max(clipTop, ry);
            clipBottom = Math.min(clipBottom, ry + vis);
            ctx.enableScissor(x, ry, x + PANEL_W, ry + vis);
            Rounded.fill(ctx, x + 4, ry, x + PANEL_W - 4, ry + full, r, c(0x48000000));
            ctx.fill(x + 4, ry + 2, x + 5, ry + full - 2, c(Theme.alpha(Theme.primary(), 0x90)));
            int sy = drawMeta(ctx, tr, m, x, ry + 2, mx, my);
            if (!m.settings.isEmpty()) {
                ctx.fill(x + 10, sy, x + PANEL_W - 10, sy + 1, c(0x18FFFFFF));
            }
            for (Setting<?> s : m.settings) sy = drawSetting(ctx, tr, s, x, sy, mx, my);
            ctx.disableScissor();
            clipTop = prevTop;
            clipBottom = prevBottom;
            ry += vis;
        }
        return ry;
    }

    /** Name with the current search query highlighted. */
    private void drawName(DrawContext ctx, TextRenderer tr, String name, int x, int y, int color) {
        int idx = search.isEmpty() ? -1 : name.toLowerCase(Locale.ROOT).indexOf(search.toLowerCase(Locale.ROOT));
        if (idx < 0) {
            ctx.drawTextWithShadow(tr, name, x, y, color);
            return;
        }
        String a = name.substring(0, idx), b = name.substring(idx, idx + search.length()), d = name.substring(idx + search.length());
        int cx = x;
        ctx.drawTextWithShadow(tr, a, cx, y, color);
        cx += tr.getWidth(a);
        int bw = tr.getWidth(b);
        Rounded.fill(ctx, cx - 1, y - 1, cx + bw + 1, y + 9, 2, c(Theme.alpha(Theme.primary(), 0x60)));
        ctx.drawTextWithShadow(tr, b, cx, y, c(0xFFFFFFFF));
        ctx.drawTextWithShadow(tr, d, cx + bw, y, color);
    }

    /** Description, Bind, Bind mode and Show-in-list rows. */
    private int drawMeta(DrawContext ctx, TextRenderer tr, Module m, int x, int y, int mx, int my) {
        int left = x + 11, right = x + PANEL_W - 10;
        boolean text = !Fx.invisible(fade);
        if (text) {
            String d = m.description;
            int max = right - left;
            if (tr.getWidth(d) > max) d = tr.trimToWidth(d, max - tr.getWidth("…")) + "…";
            ctx.getMatrices().push();
            ctx.getMatrices().translate(left, y + 3, 0);
            ctx.drawTextWithShadow(tr, d, 0, 0, c(0xFF6E6E80));
            ctx.getMatrices().pop();
        }
        hit(x, y, PANEL_W, DESC_H, (cx, cy, b) -> {
        }, null, m.description);
        y += DESC_H;

        // Bind row: click to listen, Esc cancels, Backspace/Delete clears, mouse buttons work too
        boolean listening = binding == m;
        float la = anim(List.of("listen", m), listening ? 1f : 0f, 14f);
        if (text) {
            ctx.drawTextWithShadow(tr, "Bind", left, y + 4, c(0xFFC4C4D0));
            String k = listening ? ((System.currentTimeMillis() / 400) % 2 == 0 ? "Press a key…" : "Press a key") : Keys.name(m.getKey());
            int kw = tr.getWidth("Press a key…");
            int bx = right - Math.max(kw, tr.getWidth(k)) - 10;
            float pulse = listening ? 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 140.0) : 0f;
            Rounded.fill(ctx, bx, y + 1, right, y + SET_H - 2, 4, c(Theme.lerp(0xFF23232E, Theme.primary(), la * (0.55f + 0.3f * pulse))));
            Rounded.outline(ctx, bx, y + 1, right, y + SET_H - 2, 4, c(Theme.alpha(Theme.main(), (int) (0x50 + 0x90 * la))));
            int col = m.getKey() < 0 && !listening ? 0xFF6A6A7C : 0xFFFFFFFF;
            ctx.drawCenteredTextWithShadow(tr, k, (bx + right) / 2, y + 4, c(col));
        }
        hit(x, y, PANEL_W, SET_H, (cx, cy, b) -> {
            if (b == 0) {
                binding = lastBinding == m ? null : m;
                sound(1.6f);
            } else if (b == 1) {
                m.setKey(-1);
                sound(0.9f);
            }
        }, null, "LMB: set key (keyboard or mouse button) · RMB: clear · Esc: cancel");
        y += SET_H;

        // Toggle / Hold
        float ht = anim(List.of("hold", m), m.holdMode ? 1f : 0f, 16f);
        if (text) {
            ctx.drawTextWithShadow(tr, "Mode", left, y + 4, c(0xFFC4C4D0));
            int pw = 60, px = right - pw;
            Rounded.fill(ctx, px, y + 1, right, y + SET_H - 2, 5, c(0xFF23232E));
            int kx = (int) (px + 1 + (pw / 2 - 1) * ht);
            Rounded.gradientH(ctx, kx, y + 2, kx + pw / 2 - 1, y + SET_H - 3, 4, c(Theme.primary()), c(Theme.secondary()));
            pillText(ctx, tr, "Toggle", px + pw / 4, y + 4, c(Theme.lerp(0xFF0E0E14, 0xFF8E8EA0, ht)), ht < 0.5f);
            pillText(ctx, tr, "Hold", px + pw * 3 / 4, y + 4, c(Theme.lerp(0xFF8E8EA0, 0xFF0E0E14, ht)), ht >= 0.5f);
        }
        hit(x, y, PANEL_W, SET_H, (cx, cy, b) -> {
            m.holdMode = !m.holdMode;
            if (m.holdMode && m.isEnabled()) m.setEnabled(false);
            sound(1.3f);
        }, null, "Toggle: press to switch on/off · Hold: on only while the key is held");
        y += SET_H;

        // Show in HUD list
        float vt = anim(List.of("visible", m), m.hidden ? 0f : 1f, 16f);
        if (text) ctx.drawTextWithShadow(tr, "Show in list", left, y + 4, c(0xFFC4C4D0));
        toggle(ctx, right, y, vt);
        hit(x, y, PANEL_W, SET_H, (cx, cy, b) -> {
            m.hidden = !m.hidden;
            sound(m.hidden ? 1.1f : 1.5f);
        }, null, "Show this module in the HUD module list when it is on");
        return y + SET_H;
    }

    /** Dark text on a bright pill reads badly with a drop shadow, so the selected label is drawn flat. */
    static void pillText(DrawContext ctx, TextRenderer tr, String s, int cx, int y, int color, boolean selected) {
        int x = cx - tr.getWidth(s) / 2;
        if (selected) ctx.drawText(tr, s, x, y, color, false);
        else ctx.drawTextWithShadow(tr, s, x, y, color);
    }

    private void toggle(DrawContext ctx, int right, int y, float t) {
        int tx = right - 18, ty = y + 3;
        Rounded.fill(ctx, tx, ty, tx + 18, ty + 9, 4, c(Theme.lerp(0xFF2A2A36, Theme.primary(), t)));
        if (t > 0.05f) Rounded.shadow(ctx, tx, ty, tx + 18, ty + 9, 4, 2, c(Theme.alpha(Theme.primary(), (int) (0x50 * t))));
        int kx = (int) (tx + 1 + 9 * t);
        Rounded.fill(ctx, kx, ty + 1, kx + 7, ty + 8, 3, c(0xFFFFFFFF));
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
        } else if (s instanceof ColorSetting cs) {
            float open = anim(List.of("color", cs), COLOR_OPEN.getOrDefault(cs, false) ? 1f : 0f, 14f);
            if (text) ctx.drawTextWithShadow(tr, cs.name, left, y + 4, c(0xFFC4C4D0));
            Rounded.fill(ctx, right - 22, y + 2, right, y + SET_H - 3, 4, c(0xFF000000));
            Rounded.fill(ctx, right - 21, y + 3, right - 1, y + SET_H - 4, 3, c(cs.get()));
            if (text && tr.getWidth(cs.name) + 70 < right - left) {
                String hex = String.format("#%06X", cs.get() & 0xFFFFFF);
                ctx.drawTextWithShadow(tr, hex, right - 26 - tr.getWidth(hex), y + 4, c(0xFF6A6A7C));
            }
            hit(x, y, PANEL_W, SET_H, (cx, cy, b) -> {
                COLOR_OPEN.put(cs, !COLOR_OPEN.getOrDefault(cs, false));
                sound(1.3f);
            }, null, "Click to edit colour");
            if (open > 0.02f) {
                float[] hsb = cs.hsb();
                int tw = right - left;
                int[] rows = {y + SET_H + 2, y + SET_H + 14, y + SET_H + 26};
                for (int i = 0; i < 3; i++) {
                    int ry = rows[i];
                    for (int px = 0; px < tw; px++) {
                        float f = px / (float) (tw - 1);
                        int col = switch (i) {
                            case 0 -> java.awt.Color.HSBtoRGB(f, 1f, 1f);
                            case 1 -> java.awt.Color.HSBtoRGB(hsb[0], f, Math.max(0.2f, hsb[2]));
                            default -> java.awt.Color.HSBtoRGB(hsb[0], hsb[1], f);
                        };
                        ctx.fill(left + px, ry, left + px + 1, ry + 6, c(col | 0xFF000000));
                    }
                    float v = hsb[i];
                    int kx = left + (int) (v * (tw - 1));
                    Rounded.fill(ctx, kx - 2, ry - 1, kx + 3, ry + 7, 2, c(0xFFFFFFFF));
                    final int idx = i;
                    Drag drag = cx -> {
                        float f = (float) Math.max(0, Math.min(1, (cx - left) / (double) tw));
                        float[] h = cs.hsb();
                        h[idx] = idx == 0 ? Math.min(f, 0.999f) : f;
                        cs.setHsb(h[0], h[1], h[2]);
                    };
                    hit(x, ry - 3, PANEL_W, 12, (cx, cy, b) -> {
                        if (b == 0) drag.on(cx);
                    }, drag, null);
                }
            }
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
        int sy = height - 64;
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

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return true;
        // While listening, middle / side mouse buttons become the bind.
        if (binding != null && button >= 2) {
            binding.setKey(Keys.mouse(button));
            Notifications.push(binding.name, "Bound to " + Keys.name(binding.getKey()), Notifications.Kind.INFO);
            binding = null;
            sound(1.8f);
            return true;
        }
        lastBinding = binding;
        binding = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (h.contains(mouseX, mouseY)) {
                h.click().on(mouseX, mouseY, button);
                ripples.add(new Ripple((int) mouseX, (int) mouseY));
                if (h.drag() != null && button == 0) activeSlider = h;
                lastBinding = null;
                return true;
            }
        }
        lastBinding = null;
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
        scrollTarget = Math.min(0, scrollTarget + (int) (verticalAmount * 28));
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (binding != null || swallowChar) {
            swallowChar = false;
            return true;
        }
        if (chr >= 32 && chr != 127 && search.length() < 24) {
            search += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        swallowChar = false;
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
                binding.setKey(-1);
                Notifications.push(binding.name, "Keybind cleared", Notifications.Kind.DISABLED);
            } else if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                binding.setKey(keyCode);
                Notifications.push(binding.name, "Bound to " + Keys.name(keyCode), Notifications.Kind.INFO);
            }
            binding = null;
            swallowChar = true;
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
