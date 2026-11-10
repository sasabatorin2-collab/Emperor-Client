package dev.emperor.modules;

import dev.emperor.hud.Theme;
import dev.emperor.module.Category;
import dev.emperor.module.Module;
import dev.emperor.setting.BoolSetting;
import dev.emperor.util.Projection;
import dev.emperor.util.RenderUtil;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Predicts where every projectile already in the air will land: enemy pearls, arrows, potions, tridents.
 * Landing spots near you turn red.
 */
public class ProjectileTracker extends Module {
    private record Landing(Vec3d pos, String label, int color, float seconds) {
    }

    private final BoolSetting pearls = add(new BoolSetting("Pearls", true));
    private final BoolSetting arrows = add(new BoolSetting("Arrows", true));
    private final BoolSetting potions = add(new BoolSetting("Potions", true));
    private final BoolSetting others = add(new BoolSetting("Others", false));
    private final BoolSetting labels = add(new BoolSetting("Labels", true));
    private final List<Landing> landings = new ArrayList<>();

    public ProjectileTracker() {
        super("ProjectileTracker", "Shows where pearls, arrows and potions in the air will land.", Category.RENDER);
    }

    private String kind(Entity e) {
        if (e instanceof EnderPearlEntity) return pearls.get() ? "Pearl" : null;
        if (e instanceof TridentEntity) return arrows.get() ? "Trident" : null;
        if (e instanceof ArrowEntity || e instanceof PersistentProjectileEntity) return arrows.get() ? "Arrow" : null;
        if (e instanceof PotionEntity) return potions.get() ? "Potion" : null;
        return others.get() ? e.getName().getString() : null;
    }

    @Override
    public void onRender3d(float delta) {
        landings.clear();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof ProjectileEntity)) continue;
            Vec3d vel = e.getVelocity();
            if (vel.lengthSquared() < 0.0004) continue; // stuck in a block
            String kind = kind(e);
            if (kind == null) continue;
            double gravity = e.getFinalGravity();
            double drag = e.isTouchingWater() ? 0.6 : 0.99;

            Vec3d pos = e.getLerpedPos(delta);
            Vec3d prev = pos;
            int col = Theme.accent(e.getId() % 20);
            Vec3d end = null;
            int ticks = 0;
            for (; ticks < 200; ticks++) {
                Vec3d next = prev.add(vel);
                BlockHitResult bh = mc.world.raycast(new RaycastContext(prev, next, RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE, e));
                if (bh.getType() != HitResult.Type.MISS) next = bh.getPos();
                RenderUtil.line(prev.x, prev.y, prev.z, next.x, next.y, next.z, col, 0.8f);
                if (bh.getType() != HitResult.Type.MISS) {
                    end = next;
                    break;
                }
                prev = next;
                vel = vel.multiply(drag).subtract(0, gravity, 0);
                if (prev.y < mc.world.getBottomY() - 8) break;
            }
            if (end == null) continue;
            boolean near = end.squaredDistanceTo(mc.player.getPos()) < 16;
            int c = near ? 0xFFFF4C5C : col;
            double r = 0.3;
            RenderUtil.box(new Box(end.x - r, end.y - r, end.z - r, end.x + r, end.y + r, end.z + r), c, true, true);
            RenderUtil.ring(end.x, end.y + 0.03, end.z, 0.6 + (System.currentTimeMillis() % 800) / 800.0 * 0.4, 24, 0.8f, i -> c);
            landings.add(new Landing(end, kind, c, ticks / 20f));
        }
    }

    @Override
    public void onRender2d(DrawContext ctx, float delta) {
        if (!labels.get()) return;
        TextRenderer tr = mc.textRenderer;
        for (Landing l : landings) {
            Vec3d s = Projection.toScreen(l.pos.add(0, 0.7, 0));
            if (s == null) continue;
            String t = l.label + String.format(Locale.ROOT, " · %.1fs", l.seconds);
            int w = tr.getWidth(t) + 10;
            int x = (int) s.x - w / 2, y = (int) s.y - 6;
            Rounded.fill(ctx, x, y, x + w, y + 12, 4, 0xC00E0E14);
            Rounded.outline(ctx, x, y, x + w, y + 12, 4, Theme.alpha(l.color, 0xC0));
            ctx.drawTextWithShadow(tr, t, x + 5, y + 2, l.color);
        }
    }
}
