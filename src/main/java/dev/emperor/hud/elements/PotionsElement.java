package dev.emperor.hud.elements;

import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Active effects with icons, timers and draining bars (replaces the vanilla icons). */
public class PotionsElement extends HudElement {
    private static final int ROW = 22;
    private static final List<StatusEffectInstance> DUMMY = List.of(
            new StatusEffectInstance(StatusEffects.SPEED, 1800, 1), new StatusEffectInstance(StatusEffects.STRENGTH, 900, 0));
    private final Map<RegistryEntry<StatusEffect>, Integer> maxDuration = new HashMap<>();

    public PotionsElement() {
        super("Potions", true, 2, 5, 1, 0);
    }

    private List<StatusEffectInstance> effects(boolean preview) {
        List<StatusEffectInstance> list = new ArrayList<>(mc.player.getStatusEffects());
        if (list.isEmpty() && preview) return DUMMY;
        list.sort((a, b) -> Integer.compare(b.getDuration(), a.getDuration()));
        return list;
    }

    @Override
    public boolean hasContent() {
        return !mc.player.getStatusEffects().isEmpty();
    }

    private static String label(StatusEffectInstance e) {
        String n = e.getEffectType().value().getName().getString();
        int lvl = e.getAmplifier() + 1;
        String[] roman = {"", "", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
        return n + (lvl < roman.length ? roman[lvl] : " " + lvl);
    }

    private static String time(StatusEffectInstance e) {
        if (e.isInfinite()) return "∞";
        int s = e.getDuration() / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        int w = 90;
        for (StatusEffectInstance e : effects(preview)) w = Math.max(w, tr.getWidth(label(e)) + 30);
        return w;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return Math.max(1, effects(preview).size() * (ROW + 2) - 2);
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        int ry = y;
        for (StatusEffectInstance e : effects(preview)) {
            int color = e.getEffectType().value().getColor() | 0xFF000000;
            int max = Math.max(e.getDuration(), maxDuration.getOrDefault(e.getEffectType(), 0));
            maxDuration.put(e.getEffectType(), max);

            Rounded.fill(ctx, x, ry, x + w, ry + ROW, 5, PANEL_BG);
            ctx.fill(rightAligned() ? x + w - 2 : x, ry + 3, rightAligned() ? x + w : x + 2, ry + ROW - 3, color);
            ctx.drawSpriteStretched(RenderLayer::getGuiTextured, mc.getStatusEffectSpriteManager().getSprite(e.getEffectType()),
                    x + 5, ry + 2, 18, 18);
            ctx.drawTextWithShadow(tr, label(e), x + 26, ry + 3, 0xFFFFFFFF);
            boolean ending = !e.isInfinite() && e.getDuration() < 200 && (System.currentTimeMillis() / 300) % 2 == 0;
            ctx.drawTextWithShadow(tr, time(e), x + 26, ry + 12, ending ? 0xFFFF5C6C : 0xFF9A9AA8);
            if (!e.isInfinite() && max > 0) {
                int bw = (int) ((w - 70) * (e.getDuration() / (float) max));
                int bx = x + w - 6 - (w - 70);
                Rounded.fill(ctx, bx, ry + 15, x + w - 6, ry + 17, 1, 0x40FFFFFF);
                if (bw > 0) Rounded.gradientH(ctx, bx, ry + 15, bx + bw, ry + 17, 1, color, Theme.lerp(color, 0xFFFFFFFF, 0.4f));
            }
            ry += ROW + 2;
        }
    }
}
