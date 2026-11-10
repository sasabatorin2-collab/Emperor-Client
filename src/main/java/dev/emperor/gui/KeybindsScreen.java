package dev.emperor.gui;

import dev.emperor.hud.Notifications;
import dev.emperor.hud.Theme;
import dev.emperor.macro.Macros;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.util.Fx;
import dev.emperor.util.Keys;
import dev.emperor.util.Rounded;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Every keybind in one place: click a chip and press a key (or mouse button). */
public class KeybindsScreen extends Screen {
    private static final int W = 380, ROW = 20, HEADER = 50;

    private interface Action {
        void run(int button);
    }

    private record Hit(int x, int y, int w, int h, Action action) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final Screen parent;
    private final List<Hit> hits = new ArrayList<>();
    private final Map<Object, Float> anim = new HashMap<>();
    private final long openedAt = System.currentTimeMillis();
    private long lastNs = System.nanoTime();
    private float dt;
    private Module listening;
    private boolean swallowChar;
    private String filter = "";
    private boolean boundOnly;
    private float scroll, scrollTarget;
    private int contentH;

    public KeybindsScreen(Screen parent) {
        super(Text.literal("Keybinds"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private float anim(Object key, float target, float speed) {
        float v = Fx.approach(anim.getOrDefault(key, target), target, dt, speed);
        anim.put(key, v);
        return v;
    }

    private void click(float pitch) {
        client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.25f));
    }

    private List<Module> modules() {
        List<Module> out = new ArrayList<>();
        String q = filter.toLowerCase(Locale.ROOT);
        for (Category c : Category.values()) {
            for (Module m : ModuleManager.byCategory(c)) {
                if (boundOnly && m.getKey() < 0) continue;
                if (q.isEmpty() || m.name.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
            }
        }
        return out;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (client.world != null) applyBlur();
        ctx.fillGradient(0, 0, width, height, 0x9006060A, Theme.alpha(Theme.lerp(0xFF06060A, Theme.secondary(), 0.2f), 0xD0));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        super.render(ctx, mouseX, mouseY, delta);
        hits.clear();
        float open = Fx.easeOutBack(Fx.clamp01((System.currentTimeMillis() - openedAt) / 300f));
        scroll = Fx.approach(scroll, scrollTarget, dt, 16f);

        int x = (width - W) / 2;
        int top = (int) (24 + (1 - open) * 20), bottom = height - 24;
        Rounded.shadow(ctx, x, top, x + W, bottom, 10, 12, 0x90000000);
        Rounded.shadow(ctx, x, top, x + W, bottom, 10, 4, Theme.alpha(Theme.main(), 0x40));
        Rounded.fill(ctx, x, top, x + W, bottom, 10, 0xF20E0E14);
        Rounded.gradientH(ctx, x, top, x + W, top + 24, 10, Theme.alpha(Theme.gradient(0f), 0xE0), Theme.alpha(Theme.gradient(1f), 0xE0), true, false);
        Rounded.outline(ctx, x, top, x + W, bottom, 10, Theme.alpha(Theme.main(), 0x70));
        ctx.drawTextWithShadow(textRenderer, Text.literal("⌨  Keybinds").formatted(Formatting.BOLD), x + 10, top + 8, 0xFFFFFFFF);
        int bound = 0;
        for (Module m : ModuleManager.all()) if (m.getKey() >= 0) bound++;
        String cnt = bound + " bound · " + Macros.all().size() + " macros";
        ctx.drawTextWithShadow(textRenderer, cnt, x + W - 10 - textRenderer.getWidth(cnt), top + 8, 0xFFFFFFFF);

        // filter bar + "bound only" switch
        int fy = top + 28;
        Rounded.fill(ctx, x + 8, fy, x + W - 110, fy + 16, 6, 0xFF17171F);
        Rounded.outline(ctx, x + 8, fy, x + W - 110, fy + 16, 6, Theme.alpha(Theme.main(), filter.isEmpty() ? 0x30 : 0xA0));
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        ctx.drawTextWithShadow(textRenderer, filter.isEmpty() ? "⌕ Type to filter..." : "⌕ " + filter + (blink ? "_" : ""),
                x + 14, fy + 4, filter.isEmpty() ? 0xFF6A6A7C : 0xFFFFFFFF);
        float bo = anim("boundOnly", boundOnly ? 1f : 0f, 16f);
        int bx = x + W - 104;
        Rounded.fill(ctx, bx, fy, x + W - 8, fy + 16, 6, Theme.lerp(0xFF17171F, Theme.alpha(Theme.primary(), 0xFF), bo * 0.6f));
        ctx.drawCenteredTextWithShadow(textRenderer, boundOnly ? "✔ Bound only" : "Bound only", (bx + x + W - 8) / 2, fy + 4, 0xFFE6E6EE);
        hits.add(new Hit(bx, fy, x + W - 8 - bx, 16, b -> {
            boundOnly = !boundOnly;
            click(1.3f);
        }));

        int listTop = top + HEADER, listBottom = bottom - 22;
        ctx.enableScissor(x, listTop, x + W, listBottom);
        int y = listTop + 2 - Math.round(scroll);
        Category lastCat = null;
        for (Module m : modules()) {
            if (m.category != lastCat) {
                lastCat = m.category;
                ctx.drawTextWithShadow(textRenderer, Text.literal(m.category.icon + "  " + m.category.label.toUpperCase(Locale.ROOT))
                        .formatted(Formatting.BOLD), x + 12, y + 5, Theme.accent(m.category.ordinal() * 3));
                ctx.fill(x + 12 + textRenderer.getWidth(m.category.icon + "  " + m.category.label.toUpperCase(Locale.ROOT)) + 8 + 6, y + 9,
                        x + W - 12, y + 10, 0x20FFFFFF);
                y += 16;
            }
            y = row(ctx, m, x, y, mouseX, mouseY, listTop, listBottom);
        }

        // macros
        y += 6;
        ctx.drawTextWithShadow(textRenderer, Text.literal("⚡  MACROS").formatted(Formatting.BOLD), x + 12, y + 5, Theme.primary());
        y += 16;
        if (Macros.all().isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, "Add one in chat:  .macro add f6 /home   or   .macro add g gg", x + 14, y + 5, 0xFF6A6A7C);
            y += ROW;
        }
        for (Macros.Macro mc : new ArrayList<>(Macros.all())) {
            boolean hv = mouseY >= y && mouseY < y + ROW && mouseY >= listTop && mouseY < listBottom && mouseX >= x && mouseX < x + W;
            if (hv) Rounded.fill(ctx, x + 6, y, x + W - 6, y + ROW - 2, 5, 0x14FFFFFF);
            String k = Keys.name(mc.key);
            int kw = textRenderer.getWidth(k) + 12;
            Rounded.fill(ctx, x + 12, y + 3, x + 12 + kw, y + ROW - 5, 4, 0xFF23232E);
            ctx.drawCenteredTextWithShadow(textRenderer, k, x + 12 + kw / 2, y + 6, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, "→ " + textRenderer.trimToWidth(mc.text, W - kw - 70), x + 20 + kw, y + 6, 0xFFC4C4D0);
            int dx = x + W - 26;
            ctx.drawTextWithShadow(textRenderer, "✖", dx, y + 6, hv ? 0xFFFF5C6C : 0xFF6A6A7C);
            if (y + ROW > listTop && y < listBottom) hits.add(new Hit(dx - 4, y, 16, ROW, b -> {
                Macros.remove(mc);
                ModuleManager.save();
                click(0.9f);
            }));
            y += ROW;
        }
        ctx.disableScissor();
        contentH = y + Math.round(scroll) - listTop;
        float maxScroll = Math.max(0, contentH - (listBottom - listTop) + 6);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        if (maxScroll > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(20, (int) (trackH * trackH / (float) (contentH + 6)));
            int barY = listTop + (int) ((trackH - barH) * (scroll / maxScroll));
            Rounded.fill(ctx, x + W - 5, barY, x + W - 2, barY + barH, 1, Theme.alpha(Theme.main(), 0xA0));
        }

        String help = listening != null ? "Press a key or mouse button for " + listening.name + "  ·  Esc cancel  ·  Backspace clear"
                : "Click a key to rebind  ·  RMB clear  ·  Mode switches Toggle / Hold  ·  Esc back";
        ctx.drawCenteredTextWithShadow(textRenderer, help, width / 2, bottom - 15, listening != null ? Theme.primary() : 0xFF6A6A7C);
    }

    private int row(DrawContext ctx, Module m, int x, int y, int mx, int my, int listTop, int listBottom) {
        boolean visible = y + ROW > listTop && y < listBottom;
        boolean hv = my >= y && my < y + ROW && my >= listTop && my < listBottom && mx >= x && mx < x + W;
        float h = anim(List.of("h", m), hv ? 1f : 0f, 18f);
        if (h > 0.01f) Rounded.fill(ctx, x + 6, y, x + W - 6, y + ROW - 2, 5, Theme.alpha(0xFFFFFF, (int) (0x14 * h)));
        float on = anim(List.of("on", m), m.isEnabled() ? 1f : 0f, 12f);
        Rounded.circle(ctx, x + 16, y + 9, 3, Theme.lerp(0xFF3A3A48, Theme.primary(), on));
        ctx.drawTextWithShadow(textRenderer, m.name, x + 26, y + 6, Theme.lerp(0xFFB0B0C0, 0xFFFFFFFF, Math.max(on, h)));

        // mode pill
        int pw = 70, px = x + W - 16 - pw;
        float ht = anim(List.of("hold", m), m.holdMode ? 1f : 0f, 16f);
        Rounded.fill(ctx, px, y + 3, px + pw, y + ROW - 5, 5, 0xFF1A1A23);
        int kx = (int) (px + 1 + (pw / 2 - 1) * ht);
        Rounded.gradientH(ctx, kx, y + 4, kx + pw / 2 - 1, y + ROW - 6, 4, Theme.alpha(Theme.primary(), 0xC0), Theme.alpha(Theme.secondary(), 0xC0));
        ClickGuiScreen.pillText(ctx, textRenderer, "Toggle", px + pw / 4, y + 6, Theme.lerp(0xFF0E0E14, 0xFF7A7A8C, ht), ht < 0.5f);
        ClickGuiScreen.pillText(ctx, textRenderer, "Hold", px + pw * 3 / 4, y + 6, Theme.lerp(0xFF7A7A8C, 0xFF0E0E14, ht), ht >= 0.5f);
        if (visible) hits.add(new Hit(px, y, pw, ROW, b -> {
            m.holdMode = !m.holdMode;
            if (m.holdMode && m.isEnabled()) m.setEnabled(false);
            click(1.3f);
        }));

        // key chip
        boolean listen = listening == m;
        float la = anim(List.of("l", m), listen ? 1f : 0f, 14f);
        String k = listen ? "Press a key…" : Keys.name(m.getKey());
        int cw = 86, cx = px - 8 - cw;
        float pulse = listen ? 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 140.0) : 0f;
        Rounded.fill(ctx, cx, y + 3, cx + cw, y + ROW - 5, 5, Theme.lerp(0xFF23232E, Theme.primary(), la * (0.5f + 0.3f * pulse)));
        Rounded.outline(ctx, cx, y + 3, cx + cw, y + ROW - 5, 5, Theme.alpha(Theme.main(), (int) (0x40 + 0x90 * Math.max(la, h))));
        ctx.drawCenteredTextWithShadow(textRenderer, k, cx + cw / 2, y + 6, m.getKey() < 0 && !listen ? 0xFF5E5E70 : 0xFFFFFFFF);
        if (visible) hits.add(new Hit(cx, y, cw, ROW, b -> {
            if (b == 1) {
                m.setKey(-1);
                listening = null;
                click(0.9f);
            } else if (b == 0) {
                listening = listening == m ? null : m;
                click(1.6f);
            }
        }));
        return y + ROW;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (listening != null && button >= 2) {
            finish(Keys.mouse(button));
            return true;
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(mx, my)) {
                hits.get(i).action().run(button);
                return true;
            }
        }
        listening = null;
        return super.mouseClicked(mx, my, button);
    }

    private void finish(int code) {
        Module m = listening;
        listening = null;
        m.setKey(code);
        if (code >= 0) Notifications.push(m.name, "Bound to " + Keys.name(code), Notifications.Kind.INFO);
        click(1.8f);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        scrollTarget -= (float) v * 30;
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (listening != null || swallowChar) {
            swallowChar = false;
            return true;
        }
        if (chr >= 32 && chr != 127 && filter.length() < 24) {
            filter += chr;
            scrollTarget = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        swallowChar = false;
        if (listening != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) listening = null;
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) finish(-1);
            else finish(keyCode);
            swallowChar = true;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !filter.isEmpty()) {
            filter = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : filter.substring(0, filter.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !filter.isEmpty()) {
            filter = "";
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }
}
