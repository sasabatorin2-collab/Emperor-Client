package dev.emperor.gui;

import dev.emperor.command.Commands;
import dev.emperor.hud.Theme;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * macOS-style quick launcher: fuzzy-search modules and toggle them with Enter,
 * or type a command starting with "." and run it directly.
 */
public class SpotlightScreen extends Screen {
    private static final int W = 360, INPUT_H = 28, ROW_H = 24, MAX_ROWS = 8;

    private record Result(Module module, String command, String desc) {
    }

    private String query = "";
    private int selected;
    private List<Result> results = new ArrayList<>();
    private final Map<Object, Float> anim = new HashMap<>();
    private final long openedAt = System.currentTimeMillis();
    private long lastFrame = System.nanoTime();
    private float selY;
    private int boxX, listY;

    public SpotlightScreen() {
        super(Text.literal("Spotlight"));
        refresh();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Higher is better; negative means no match. */
    private static int score(String text, String q) {
        String t = text.toLowerCase(Locale.ROOT);
        if (t.equals(q)) return 1000;
        if (t.startsWith(q)) return 500 - t.length();
        int idx = t.indexOf(q);
        if (idx >= 0) return 300 - idx;
        int ti = 0, gaps = 0;
        for (char c : q.toCharArray()) {
            int f = t.indexOf(c, ti);
            if (f < 0) return -1;
            gaps += f - ti;
            ti = f + 1;
        }
        return 100 - gaps;
    }

    private void refresh() {
        List<Result> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.startsWith(Commands.PREFIX)) {
            out.add(new Result(null, query.trim(), "Run command"));
            String typed = q.substring(Commands.PREFIX.length());
            for (String[] c : Commands.USAGE) {
                if (c[0].startsWith(typed.split(" ")[0])) out.add(new Result(null, Commands.PREFIX + c[0], c[1]));
            }
        } else if (q.isEmpty()) {
            List<Module> mods = new ArrayList<>(ModuleManager.all());
            mods.sort(Comparator.comparing((Module m) -> !m.isEnabled()).thenComparing(m -> m.name));
            for (Module m : mods) out.add(new Result(m, null, m.description));
        } else {
            List<Module> mods = new ArrayList<>();
            Map<Module, Integer> scores = new HashMap<>();
            for (Module m : ModuleManager.all()) {
                int s = Math.max(score(m.name, q), score(m.description, q) / 4);
                if (s > 0) {
                    mods.add(m);
                    scores.put(m, s);
                }
            }
            mods.sort((a, b) -> scores.get(b) - scores.get(a));
            for (Module m : mods) out.add(new Result(m, null, m.description));
        }
        results = out;
        selected = Math.min(selected, Math.max(0, results.size() - 1));
    }

    private int visibleRows() {
        return Math.min(MAX_ROWS, results.size());
    }

    private int firstRow() {
        return Math.max(0, Math.min(selected - MAX_ROWS + 1, results.size() - MAX_ROWS));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        float open = Fx.easeOutBack(Fx.clamp01((System.currentTimeMillis() - openedAt) / 260f));
        float a = Fx.clamp01((System.currentTimeMillis() - openedAt) / 160f);

        ctx.fillGradient(0, 0, width, height, Theme.alpha(0x06060A, (int) (0x70 * a)), Theme.alpha(0x06060A, (int) (0xB0 * a)));

        int rows = visibleRows();
        int h = INPUT_H + (rows > 0 ? rows * ROW_H + 10 : 0) + 18;
        boxX = (width - W) / 2;
        int y = (int) (height / 4f - (1 - open) * 20);
        Rounded.shadow(ctx, boxX, y, boxX + W, y + h, 10, 12, Theme.alpha(0, (int) (0x90 * a)));
        Rounded.shadow(ctx, boxX, y, boxX + W, y + h, 10, 4, Theme.alpha(Theme.main(), (int) (0x40 * a)));
        Rounded.fill(ctx, boxX, y, boxX + W, y + h, 10, Theme.fade(0xF20E0E14, a));
        Rounded.outline(ctx, boxX, y, boxX + W, y + h, 10, Theme.alpha(Theme.main(), (int) (0x70 * a)));
        if (Fx.invisible(a)) return;

        // input line
        ctx.getMatrices().push();
        ctx.getMatrices().translate(boxX + 12, y + 9, 0);
        ctx.getMatrices().scale(1.25f, 1.25f, 1f);
        ctx.drawTextWithShadow(textRenderer, "⌕", 0, 0, Theme.primary());
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        String shown = query.isEmpty() ? "Search modules or type .command" : query + (blink ? "_" : "");
        ctx.drawTextWithShadow(textRenderer, shown, 12, 0, query.isEmpty() ? 0xFF6A6A7C : 0xFFFFFFFF);
        ctx.getMatrices().pop();
        Rounded.gradientH(ctx, boxX + 10, y + INPUT_H, boxX + W - 10, y + INPUT_H + 1, 0, Theme.primary(), Theme.secondary());

        listY = y + INPUT_H + 6;
        int first = firstRow();
        float target = listY + (selected - first) * ROW_H;
        selY = selY == 0 ? target : Fx.approach(selY, target, dt, 22f);
        if (rows > 0) {
            Rounded.gradientH(ctx, boxX + 6, (int) selY, boxX + W - 6, (int) selY + ROW_H - 2, 6,
                    Theme.alpha(Theme.primary(), 0x50), Theme.alpha(Theme.secondary(), 0x28));
            ctx.fill(boxX + 6, (int) selY + 4, boxX + 8, (int) selY + ROW_H - 6, Theme.primary());
        }

        for (int i = 0; i < rows; i++) {
            Result r = results.get(first + i);
            int ry = listY + i * ROW_H;
            if (r.module != null) {
                Module m = r.module;
                float on = anim.merge(m, 0f, (o, n) -> o);
                on = Fx.approach(on, m.isEnabled() ? 1f : 0f, dt, 14f);
                anim.put(m, on);
                ctx.drawTextWithShadow(textRenderer, m.category.icon, boxX + 14, ry + 7, Theme.accent(i * 2));
                ctx.drawTextWithShadow(textRenderer, Text.literal(m.name).formatted(Formatting.BOLD), boxX + 28, ry + 3, 0xFFFFFFFF);
                String d = textRenderer.getWidth(r.desc) > W - 120 ? textRenderer.trimToWidth(r.desc, W - 126) + "…" : r.desc;
                ctx.drawTextWithShadow(textRenderer, d, boxX + 28, ry + 13, 0xFF7A7A8C);
                // ON/OFF pill
                int px = boxX + W - 44, py = ry + 6;
                Rounded.fill(ctx, px, py, px + 30, py + 11, 5, Theme.lerp(0xFF2A2A36, Theme.primary(), on));
                ctx.drawCenteredTextWithShadow(textRenderer, m.isEnabled() ? "ON" : "OFF", px + 15, py + 2,
                        Theme.lerp(0xFF9A9AA8, 0xFF0E0E14, on));
                if (m.getKey() >= 0) {
                    String k = dev.emperor.util.Keys.name(m.getKey());
                    int kw = textRenderer.getWidth(k) + 6;
                    Rounded.fill(ctx, px - kw - 4, py, px - 4, py + 11, 3, 0x60000000);
                    ctx.drawTextWithShadow(textRenderer, k, px - kw - 1, py + 2, 0xFFB0B0C0);
                }
            } else {
                ctx.drawTextWithShadow(textRenderer, "›", boxX + 15, ry + 7, Theme.primary());
                ctx.drawTextWithShadow(textRenderer, Text.literal(r.command).formatted(Formatting.BOLD), boxX + 28, ry + 3, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, r.desc, boxX + 28, ry + 13, 0xFF7A7A8C);
            }
        }

        String foot = results.isEmpty() ? "No matches" : "↑↓ select  ·  Enter toggle / run  ·  Shift+Enter settings  ·  Esc";
        ctx.drawCenteredTextWithShadow(textRenderer, foot, width / 2, y + h - 13, 0xFF5E5E70);
    }


    private void activate(boolean shift) {
        if (results.isEmpty()) return;
        Result r = results.get(selected);
        if (r.module != null) {
            if (shift) {
                client.setScreen(ClickGuiScreen.focus(r.module));
                return;
            }
            r.module.toggle();
            refreshKeepSelection(r.module);
        } else {
            if (selected == 0 || r.command.contains(" ") || query.trim().equals(r.command)) {
                String cmd = selected == 0 ? query.trim() : r.command;
                close();
                Commands.handle(cmd);
            } else {
                query = r.command + " ";
                refresh();
            }
        }
    }

    private void refreshKeepSelection(Module m) {
        if (query.isEmpty()) {
            refresh();
            for (int i = 0; i < results.size(); i++) if (results.get(i).module == m) selected = i;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int i = (int) ((my - listY) / ROW_H);
        if (mx >= boxX && mx < boxX + W && my >= listY && i >= 0 && i < visibleRows()) {
            selected = firstRow() + i;
            activate(button == 1);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (!results.isEmpty()) selected = Math.floorMod(selected - (int) Math.signum(v), results.size());
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127 && query.length() < 64) {
            query += chr;
            selected = 0;
            refresh();
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_TAB -> {
                if (!results.isEmpty()) selected = (selected + 1) % results.size();
                return true;
            }
            case GLFW.GLFW_KEY_UP -> {
                if (!results.isEmpty()) selected = Math.floorMod(selected - 1, results.size());
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                activate((modifiers & GLFW.GLFW_MOD_SHIFT) != 0);
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!query.isEmpty()) {
                    query = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : query.substring(0, query.length() - 1);
                    selected = 0;
                    refresh();
                }
                return true;
            }
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
    }

    @Override
    public void removed() {
        ModuleManager.save();
    }
}
