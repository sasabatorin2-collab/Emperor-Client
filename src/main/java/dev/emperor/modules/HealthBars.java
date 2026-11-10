package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.setting.NumberSetting;
import dev.emperor.util.Fx;
import dev.emperor.util.Projection;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Smooth health bars over mobs with a trailing "damage ghost". */
public class HealthBars extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", 24, 8, 64, 2));
    private final BoolSetting onlyDamaged = add(new BoolSetting("Only damaged", false));
    private final BoolSetting numbers = add(new BoolSetting("Numbers", true));
    private final Map<Integer, Float> ghost = new HashMap<>();
    private long lastNs = System.nanoTime();

    public HealthBars() {
        super("HealthBars", "Animated health bars above every mob.", Category.RENDER);
    }

    @Override
    protected void onDisable() {
        ghost.clear();
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;
        TextRenderer tr = mc.textRenderer;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e instanceof PlayerEntity || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            if (mc.player.squaredDistanceTo(e) > range.d() * range.d()) continue;
            float max = Math.max(1f, le.getMaxHealth());
            float hp = le.getHealth() + le.getAbsorptionAmount();
            if (onlyDamaged.get() && hp >= max) continue;
            Vec3d s = Projection.toScreen(le.getLerpedPos(delta).add(0, le.getHeight() + 0.35, 0));
            if (s == null) continue;

            float g = ghost.getOrDefault(e.getId(), hp);
            g = g < hp ? hp : Fx.approach(g, hp, dt, 5f);
            ghost.put(e.getId(), g);

            float scale = (float) Math.max(0.55, Math.min(1.2, 10 / Math.max(1, s.z)));
            ctx.getMatrices().push();
            ctx.getMatrices().translate(s.x, s.y, 0);
            ctx.getMatrices().scale(scale, scale, 1f);
            int w = 40, h = 5;
            int frac = (int) (w * Math.min(1f, hp / max));
            int gw = (int) (w * Math.min(1f, g / max));
            Rounded.fill(ctx, -w / 2 - 1, -1, w / 2 + 1, h + 1, 3, 0xC00E0E14);
            if (gw > frac) Rounded.fill(ctx, -w / 2, 0, -w / 2 + gw, h, 2, 0xC0FFFFFF);
            int c1 = hp / max > 0.5f ? 0xFF4CE08A : hp / max > 0.25f ? 0xFFFFD84C : 0xFFFF4C5C;
            if (frac > 0) Rounded.gradientH(ctx, -w / 2, 0, -w / 2 + frac, h, 2, c1, Theme.lerp(c1, 0xFFFFFFFF, 0.35f));
            if (numbers.get()) {
                ctx.getMatrices().scale(0.75f, 0.75f, 1f);
                ctx.drawCenteredTextWithShadow(tr, String.format(Locale.ROOT, "%.0f / %.0f", hp, max), 0, -11, 0xFFE6E6EE);
            }
            ctx.getMatrices().pop();
        }
        ghost.keySet().removeIf(id -> mc.world.getEntityById(id) == null);
    }
}
