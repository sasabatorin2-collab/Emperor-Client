package dev.emperor.hud;

import dev.emperor.EmperorClient;
import dev.emperor.friend.Friends;
import dev.emperor.module.Module;
import dev.emperor.module.ModuleManager;
import dev.emperor.modules.HudModule;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Hud {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final int PANEL_BG = 0xB80E0E14;

    private static final Map<Module, Float> SLIDE = new HashMap<>();
    private static final Map<Module, Float> ROW_Y = new HashMap<>();
    private static long lastFrame = System.nanoTime();

    private static LivingEntity target;
    private static long targetTime;
    private static float targetAnim;
    private static float shownHealth = -1;

    private Hud() {
    }

    /** Remembers the entity you are fighting so the Target HUD can show it. */
    public static void setTarget(LivingEntity e) {
        if (e != target) shownHealth = -1;
        target = e;
        targetTime = System.currentTimeMillis();
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        long nowNs = System.nanoTime();
        float dt = Math.min(0.1f, (nowNs - lastFrame) / 1e9f);
        lastFrame = nowNs;

        MinecraftClient mc = MinecraftClient.getInstance();
        HudModule h = ModuleManager.get(HudModule.class);
        if (h == null || !h.isEnabled() || mc.options.hudHidden || mc.player == null) return;
        TextRenderer tr = mc.textRenderer;

        if (h.watermark.get()) watermark(ctx, tr, mc);
        if (h.arrayList.get()) arrayList(ctx, tr, h, dt);
        if (h.info.get()) info(ctx, tr, mc);
        if (h.armor.get()) armor(ctx, tr, mc);
        if (h.targetHud.get()) targetHud(ctx, tr, mc, dt);
    }

    private static void watermark(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        PlayerListEntry me = mc.getNetworkHandler() == null ? null : mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
        String sep = "  ⏐  ";
        String stats = mc.getCurrentFps() + " fps" + sep + (me == null ? 0 : me.getLatency()) + " ms" + sep
                + LocalTime.now().format(TIME);
        String crown = "♛ ";
        String name = EmperorClient.NAME;
        String ver = " " + EmperorClient.VERSION;
        int w = tr.getWidth(crown) + tr.getWidth(Text.literal(name).formatted(Formatting.BOLD)) + tr.getWidth(ver)
                + tr.getWidth(sep) + tr.getWidth(stats);

        int x = 5, y = 5, pw = w + 14, ph = 17;
        Rounded.shadow(ctx, x, y, x + pw, y + ph, 6, 6, 0x50000000);
        Rounded.fill(ctx, x, y, x + pw, y + ph, 6, PANEL_BG);
        Rounded.gradientH(ctx, x + 4, y, x + pw - 4, y + 1, 0, Theme.gradient(0f), Theme.gradient(1f));
        Rounded.outline(ctx, x, y, x + pw, y + ph, 6, Theme.alpha(Theme.main(), 0x30));

        int cx = x + 7, ty = y + 5;
        ctx.drawTextWithShadow(tr, crown, cx, ty, Theme.primary());
        cx += tr.getWidth(crown);
        for (int i = 0; i < name.length(); i++) {
            Text ch = Text.literal(String.valueOf(name.charAt(i))).formatted(Formatting.BOLD);
            ctx.drawTextWithShadow(tr, ch, cx, ty, Theme.accent(i * 2));
            cx += tr.getWidth(ch);
        }
        ctx.drawTextWithShadow(tr, ver, cx, ty, 0xFF8C8C9A);
        cx += tr.getWidth(ver);
        ctx.drawTextWithShadow(tr, sep, cx, ty, 0xFF4A4A58);
        cx += tr.getWidth(sep);
        ctx.drawTextWithShadow(tr, stats, cx, ty, 0xFFE6E6EE);
    }

    private static int width(TextRenderer tr, Module m) {
        String info = m.getInfo();
        return tr.getWidth(m.name) + (info == null ? 0 : tr.getWidth(" " + info));
    }

    private static void arrayList(DrawContext ctx, TextRenderer tr, HudModule h, float dt) {
        List<Module> shown = new ArrayList<>();
        for (Module m : ModuleManager.all()) {
            if (m.category == dev.emperor.module.Category.CLIENT) continue;
            float s = Fx.approach(SLIDE.getOrDefault(m, 0f), m.isEnabled() ? 1f : 0f, dt, 12f);
            SLIDE.put(m, s);
            if (s > 0.01f) shown.add(m);
            else ROW_Y.remove(m);
        }
        shown.sort((a, b) -> Integer.compare(width(tr, b), width(tr, a)));

        int sw = ctx.getScaledWindowWidth();
        int rowH = 12;
        float y = 5;
        int i = 0;
        for (Module m : shown) {
            float s = SLIDE.get(m);
            float e = Fx.easeOutCubic(s);
            float ry = ROW_Y.containsKey(m) ? Fx.approach(ROW_Y.get(m), y, dt, 16f) : y;
            ROW_Y.put(m, ry);

            int w = width(tr, m) + 10;
            int x = (int) (sw - 5 - w * e);
            int top = Math.round(ry);
            int col = Theme.accent(i * 3);

            ctx.fill(x, top, sw - 5, top + rowH, Theme.fade(PANEL_BG, e));
            ctx.fill(sw - 5, top, sw - 3, top + rowH, Theme.fade(col, e));
            ctx.fill(x, top, x + 1, top + rowH, Theme.fade(Theme.alpha(col, 0x50), e));
            if (!Fx.invisible(e)) {
                ctx.drawTextWithShadow(tr, m.name, x + 5, top + 2, Theme.fade(col, e));
                String info = m.getInfo();
                if (info != null) {
                    ctx.drawTextWithShadow(tr, " " + info, x + 5 + tr.getWidth(m.name), top + 2, Theme.fade(0xFF9A9AA8, e));
                }
            }
            y += rowH * e;
            i++;
        }
    }

    private static void info(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        var p = mc.player;
        boolean nether = mc.world != null && mc.world.getRegistryKey() == World.NETHER;
        double f = nether ? 8.0 : 1.0 / 8.0;
        double bps = Math.hypot(p.getX() - p.prevX, p.getZ() - p.prevZ) * 20.0;
        Direction d = p.getHorizontalFacing();
        String axis = switch (d) {
            case NORTH -> "-Z";
            case SOUTH -> "+Z";
            case WEST -> "-X";
            default -> "+X";
        };

        List<String[]> lines = new ArrayList<>();
        lines.add(new String[]{"XYZ", String.format(Locale.ROOT, "%.1f  %.1f  %.1f", p.getX(), p.getY(), p.getZ())});
        lines.add(new String[]{nether ? "Overworld" : "Nether", String.format(Locale.ROOT, "%.1f  %.1f", p.getX() * f, p.getZ() * f)});
        lines.add(new String[]{"Speed", String.format(Locale.ROOT, "%.2f b/s", bps)});
        lines.add(new String[]{"Facing", d.asString().substring(0, 1).toUpperCase(Locale.ROOT) + d.asString().substring(1) + " (" + axis + ")"});

        int sh = ctx.getScaledWindowHeight();
        int y = sh - 6 - lines.size() * 10 - (mc.currentScreen instanceof ChatScreen ? 14 : 0);
        int i = 0;
        for (String[] l : lines) {
            int lw = tr.getWidth(l[0] + " ");
            ctx.drawTextWithShadow(tr, l[0] + " ", 5, y, Theme.accent(i * 3));
            ctx.drawTextWithShadow(tr, l[1], 5 + lw, y, 0xFFE6E6EE);
            y += 10;
            i++;
        }
    }

    private static void armor(DrawContext ctx, TextRenderer tr, MinecraftClient mc) {
        List<ItemStack> items = mc.player.getInventory().armor;
        boolean any = false;
        for (ItemStack s : items) any |= !s.isEmpty();
        if (!any) return;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int x = sw / 2 + 12;
        int y = sh - 56 - (mc.player.isSubmergedInWater() || mc.player.getAir() < mc.player.getMaxAir() ? 10 : 0);
        if (mc.interactionManager != null && !mc.interactionManager.hasStatusBars()) y += 16;
        for (int i = items.size() - 1; i >= 0; i--) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                ctx.drawItem(s, x, y);
                ctx.drawStackOverlay(tr, s, x, y);
            }
            x += 18;
        }
    }

    private static void targetHud(DrawContext ctx, TextRenderer tr, MinecraftClient mc, float dt) {
        boolean valid = target != null && target.isAlive() && !target.isRemoved()
                && System.currentTimeMillis() - targetTime < 3000 && mc.player.distanceTo(target) < 24;
        targetAnim = Fx.approach(targetAnim, valid ? 1f : 0f, dt, 12f);
        if (targetAnim < 0.02f || target == null) return;
        float a = targetAnim;

        float max = Math.max(1f, target.getMaxHealth());
        float hp = target.getHealth() + target.getAbsorptionAmount();
        shownHealth = shownHealth < 0 ? hp : Fx.approach(shownHealth, hp, dt, 8f);

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int w = 148, h = 44;
        int x = sw / 2 + 16;
        int y = (int) (sh / 2f + 16 + (1 - Fx.easeOutBack(a)) * 12);

        Rounded.shadow(ctx, x, y, x + w, y + h, 8, 7, Theme.alpha(0x000000, (int) (100 * a)));
        Rounded.fill(ctx, x, y, x + w, y + h, 8, Theme.fade(0xE60E0E14, a));
        Rounded.outline(ctx, x, y, x + w, y + h, 8, Theme.alpha(Theme.main(), (int) (80 * a)));

        // head
        int hx = x + 6, hy = y + 6, hs = 32;
        Rounded.fill(ctx, hx - 1, hy - 1, hx + hs + 1, hy + hs + 1, 5, Theme.alpha(Theme.main(), (int) (120 * a)));
        if (target instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(ctx, pl.getSkinTextures(), hx, hy, hs);
        } else {
            Rounded.fill(ctx, hx, hy, hx + hs, hy + hs, 4, Theme.fade(0xFF1C1C26, a));
            String ch = target.getName().getString().substring(0, 1).toUpperCase(Locale.ROOT);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(hx + hs / 2f, hy + hs / 2f - 6, 0);
            ctx.getMatrices().scale(2f, 2f, 1f);
            ctx.drawCenteredTextWithShadow(tr, ch, 0, 0, Theme.fade(Theme.primary(), a));
            ctx.getMatrices().pop();
        }
        if (target.hurtTime > 0) ctx.fill(hx, hy, hx + hs, hy + hs, Theme.alpha(0xFF2020, (int) (target.hurtTime * 12 * a)));

        if (Fx.invisible(a)) return;
        int tx = hx + hs + 7;
        String name = target.getName().getString();
        if (tr.getWidth(name) > w - (tx - x) - 6) name = tr.trimToWidth(name, w - (tx - x) - 12) + "…";
        ctx.drawTextWithShadow(tr, Text.literal(name).formatted(Formatting.BOLD), tx, y + 7,
                Theme.fade(Friends.is(target) ? Theme.primary() : 0xFFFFFFFF, a));

        float myHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean winning = myHp >= hp;
        String stat = String.format(Locale.ROOT, "%.1f ❤", hp);
        ctx.drawTextWithShadow(tr, stat, tx, y + 18, Theme.fade(healthColor(hp / max), a));
        String dist = String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(target));
        ctx.drawTextWithShadow(tr, dist, x + w - 7 - tr.getWidth(dist), y + 18, Theme.fade(0xFF8C8C9A, a));
        String wl = winning ? "▲ Winning" : "▼ Losing";
        ctx.drawTextWithShadow(tr, wl, x + w - 7 - tr.getWidth(wl), y + 7, Theme.fade(winning ? 0xFF4CE08A : 0xFFFF5C6C, a));

        // health bar: white "ghost" trails behind the real value when damage is taken
        int bx = tx, by = y + 31, bw = x + w - 7 - tx, bh = 5;
        Rounded.fill(ctx, bx, by, bx + bw, by + bh, 2, Theme.fade(0xFF23232E, a));
        int ghost = (int) (bw * Math.min(1f, shownHealth / max));
        int cur = (int) (bw * Math.min(1f, hp / max));
        if (ghost > cur) Rounded.fill(ctx, bx, by, bx + ghost, by + bh, 2, Theme.fade(0xFFFFFFFF, a * 0.6f));
        if (cur > 0) Rounded.gradientH(ctx, bx, by, bx + cur, by + bh, 2,
                Theme.fade(Theme.primary(), a), Theme.fade(Theme.secondary(), a));
    }

    private static int healthColor(float frac) {
        frac = Fx.clamp01(frac);
        return frac > 0.5f ? Theme.lerp(0xFFFFD84C, 0xFF4CE08A, (frac - 0.5f) * 2f) : Theme.lerp(0xFFFF4C5C, 0xFFFFD84C, frac * 2f);
    }
}
