package dev.emperor.hud.elements;

import dev.emperor.friend.Friends;
import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.util.Fx;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

public class TargetHudElement extends HudElement {
    private static LivingEntity target;
    private static long targetTime;
    private static float anim;
    private static float shownHealth = -1;

    public TargetHudElement() {
        super("Target HUD", true, 1, 90, 1, 38);
    }

    public static void setTarget(LivingEntity e) {
        if (e != target) shownHealth = -1;
        target = e;
        targetTime = System.currentTimeMillis();
    }

    private static boolean valid() {
        return target != null && target.isAlive() && !target.isRemoved() && mc.player != null
                && System.currentTimeMillis() - targetTime < 3000 && mc.player.distanceTo(target) < 24;
    }

    @Override
    public boolean hasContent() {
        if (!valid()) anim = Fx.approach(anim, 0f, 0.016f, 12f);
        return valid() || anim > 0.02f;
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return 148;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return 44;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        LivingEntity t = preview && !valid() ? mc.player : target;
        if (!preview) anim = Fx.approach(anim, valid() ? 1f : 0f, dt, 12f);
        float a = preview ? 1f : anim;
        if (t == null || a < 0.02f) return;

        float max = Math.max(1f, t.getMaxHealth());
        float hp = t.getHealth() + t.getAbsorptionAmount();
        shownHealth = shownHealth < 0 ? hp : Fx.approach(shownHealth, hp, dt, 8f);
        int y = (int) (this.y + (1 - Fx.easeOutBack(a)) * 12);

        Rounded.shadow(ctx, x, y, x + w, y + h, 8, 7, Theme.alpha(0x000000, (int) (100 * a)));
        Rounded.fill(ctx, x, y, x + w, y + h, 8, Theme.fade(0xE60E0E14, a));
        Rounded.outline(ctx, x, y, x + w, y + h, 8, Theme.alpha(Theme.main(), (int) (80 * a)));

        int hx = x + 6, hy = y + 6, hs = 32;
        Rounded.fill(ctx, hx - 1, hy - 1, hx + hs + 1, hy + hs + 1, 5, Theme.alpha(Theme.main(), (int) (120 * a)));
        if (t instanceof AbstractClientPlayerEntity pl) {
            PlayerSkinDrawer.draw(ctx, pl.getSkinTextures(), hx, hy, hs);
        } else {
            Rounded.fill(ctx, hx, hy, hx + hs, hy + hs, 4, Theme.fade(0xFF1C1C26, a));
            String ch = t.getName().getString().substring(0, 1).toUpperCase(Locale.ROOT);
            ctx.getMatrices().push();
            ctx.getMatrices().translate(hx + hs / 2f, hy + hs / 2f - 6, 0);
            ctx.getMatrices().scale(2f, 2f, 1f);
            ctx.drawCenteredTextWithShadow(tr, ch, 0, 0, Theme.fade(Theme.primary(), a));
            ctx.getMatrices().pop();
        }
        if (t.hurtTime > 0) ctx.fill(hx, hy, hx + hs, hy + hs, Theme.alpha(0xFF2020, (int) (t.hurtTime * 12 * a)));
        if (Fx.invisible(a)) return;

        int tx = hx + hs + 7;
        String name = t.getName().getString();
        if (tr.getWidth(name) > w - (tx - x) - 50) name = tr.trimToWidth(name, w - (tx - x) - 56) + "…";
        ctx.drawTextWithShadow(tr, Text.literal(name).formatted(Formatting.BOLD), tx, y + 7,
                Theme.fade(Friends.is(t) ? Theme.primary() : 0xFFFFFFFF, a));

        float myHp = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        boolean winning = myHp >= hp;
        ctx.drawTextWithShadow(tr, String.format(Locale.ROOT, "%.1f ❤", hp), tx, y + 18, Theme.fade(healthColor(hp / max), a));
        String dist = String.format(Locale.ROOT, "%.1fm", mc.player.distanceTo(t));
        ctx.drawTextWithShadow(tr, dist, x + w - 7 - tr.getWidth(dist), y + 18, Theme.fade(0xFF8C8C9A, a));
        String wl = t == mc.player ? "▲ You" : winning ? "▲ Winning" : "▼ Losing";
        ctx.drawTextWithShadow(tr, wl, x + w - 7 - tr.getWidth(wl), y + 7, Theme.fade(winning ? 0xFF4CE08A : 0xFFFF5C6C, a));

        // health bar: the white "ghost" trails behind the real value when damage is taken
        int bx = tx, by = y + 31, bw = x + w - 7 - tx, bh = 5;
        Rounded.fill(ctx, bx, by, bx + bw, by + bh, 2, Theme.fade(0xFF23232E, a));
        int ghost = (int) (bw * Math.min(1f, shownHealth / max));
        int cur = (int) (bw * Math.min(1f, hp / max));
        if (ghost > cur) Rounded.fill(ctx, bx, by, bx + ghost, by + bh, 2, Theme.fade(0xFFFFFFFF, a * 0.6f));
        if (cur > 0) Rounded.gradientH(ctx, bx, by, bx + cur, by + bh, 2, Theme.fade(Theme.primary(), a), Theme.fade(Theme.secondary(), a));
    }

    private static int healthColor(float frac) {
        frac = Fx.clamp01(frac);
        return frac > 0.5f ? Theme.lerp(0xFFFFD84C, 0xFF4CE08A, (frac - 0.5f) * 2f) : Theme.lerp(0xFFFF4C5C, 0xFFFFD84C, frac * 2f);
    }
}
