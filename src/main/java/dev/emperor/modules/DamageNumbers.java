package dev.emperor.modules;

import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Fx;
import dev.emperor.util.Projection;
import dev.emperor.hud.Theme;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/** Floating, popping damage and heal numbers above entities. */
public class DamageNumbers extends Module {
    private record Pop(Vec3d pos, String text, int color, long start, double dx, double dz, float size) {
    }

    private final NumberSetting scale = add(new NumberSetting("Scale", 1.2, 0.6, 2.5, 0.1));
    private final BoolSetting heals = add(new BoolSetting("Show heals", true));
    private final BoolSetting self = add(new BoolSetting("Show self", false));

    private final Map<Integer, Float> lastHp = new HashMap<>();
    private final List<Pop> pops = new ArrayList<>();
    private final Random rnd = new Random();

    public DamageNumbers() {
        super("DamageNumbers", "RPG-style popping damage numbers over hit entities.", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        lastHp.clear();
        pops.clear();
    }

    private static int colorFor(float dmg) {
        if (dmg >= 8) return 0xFFFF3B5C;
        if (dmg >= 4) return Theme.lerp(0xFFFFB84C, 0xFFFF3B5C, (dmg - 4) / 4f);
        return Theme.lerp(0xFFFFFFFF, 0xFFFFB84C, dmg / 4f);
    }

    @Override
    public void onTick() {
        if (mc.world == null) return;
        Map<Integer, Float> now = new HashMap<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || (e == mc.player && !self.get())) continue;
            if (mc.player.squaredDistanceTo(e) > 48 * 48) continue;
            float hp = le.getHealth() + le.getAbsorptionAmount();
            now.put(e.getId(), hp);
            Float prev = lastHp.get(e.getId());
            if (prev == null) continue;
            float diff = prev - hp;
            Vec3d at = e.getPos().add(0, e.getHeight() + 0.25, 0);
            double dx = (rnd.nextDouble() - 0.5) * 0.6, dz = (rnd.nextDouble() - 0.5) * 0.6;
            if (diff > 0.05f) {
                boolean big = diff >= 8;
                String t = (big ? "✦ " : "") + String.format(Locale.ROOT, diff % 1 < 0.05 ? "%.0f" : "%.1f", diff);
                pops.add(new Pop(at, t, colorFor(diff), System.currentTimeMillis(), dx, dz, big ? 1.35f : 1f));
            } else if (diff < -0.5f && heals.get()) {
                pops.add(new Pop(at, String.format(Locale.ROOT, "+%.0f", -diff), 0xFF4CE08A, System.currentTimeMillis(), dx, dz, 0.85f));
            }
        }
        lastHp.clear();
        lastHp.putAll(now);
        if (pops.size() > 80) pops.subList(0, pops.size() - 80).clear();
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        TextRenderer tr = mc.textRenderer;
        long now = System.currentTimeMillis();
        Iterator<Pop> it = pops.iterator();
        while (it.hasNext()) {
            Pop p = it.next();
            float t = (now - p.start) / 1100f;
            if (t >= 1f) {
                it.remove();
                continue;
            }
            Vec3d pos = p.pos.add(p.dx * t, Fx.easeOutCubic(t) * 0.9, p.dz * t);
            Vec3d s = Projection.toScreen(pos);
            if (s == null) continue;
            float alpha = 1f - Math.max(0f, (t - 0.65f) / 0.35f);
            if (Fx.invisible(alpha)) continue;
            float pop = t < 0.18f ? Fx.easeOutBack(t / 0.18f) : 1f - (t - 0.18f) * 0.25f;
            float sc = (float) (scale.f() * p.size * pop * Math.max(0.6, Math.min(1.6, 8 / Math.max(1, s.z))));
            ctx.getMatrices().push();
            ctx.getMatrices().translate(s.x, s.y, 0);
            ctx.getMatrices().scale(sc, sc, 1f);
            Text text = Text.literal(p.text).formatted(Formatting.BOLD);
            ctx.drawCenteredTextWithShadow(tr, text, 0, -4, Theme.fade(p.color, alpha));
            ctx.getMatrices().pop();
        }
    }
}
