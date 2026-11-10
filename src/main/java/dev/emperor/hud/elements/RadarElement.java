package dev.emperor.hud.elements;

import dev.emperor.friend.Friends;
import dev.emperor.hud.HudElement;
import dev.emperor.hud.Theme;
import dev.emperor.module.ModuleManager;
import dev.emperor.modules.HudModule;
import dev.emperor.util.Rounded;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Rotating mini radar: you are in the centre, up is the direction you are looking. */
public class RadarElement extends HudElement {
    private static final int SIZE = 92;

    public RadarElement() {
        super("Radar", true, 0, 5, 0, 74);
    }

    @Override
    public int width(TextRenderer tr, boolean preview) {
        return SIZE;
    }

    @Override
    public int height(TextRenderer tr, boolean preview) {
        return SIZE;
    }

    @Override
    public void render(DrawContext ctx, TextRenderer tr, float dt, boolean preview) {
        HudModule hm = ModuleManager.get(HudModule.class);
        double range = hm == null ? 48 : hm.radarRange.d();
        int cx = x + SIZE / 2, cy = y + SIZE / 2, r = SIZE / 2 - 4;
        float delta = mc.getRenderTickCounter().getTickDelta(true);

        Rounded.shadow(ctx, x, y, x + SIZE, y + SIZE, SIZE / 2, 6, 0x50000000);
        Rounded.circle(ctx, cx, cy, SIZE / 2, PANEL_BG);
        Rounded.outline(ctx, x, y, x + SIZE, y + SIZE, SIZE / 2, Theme.alpha(Theme.main(), 0x70));
        Rounded.outline(ctx, cx - r / 2, cy - r / 2, cx + r / 2, cy + r / 2, r / 2, 0x22FFFFFF);
        ctx.fill(cx - r, cy, cx + r, cy + 1, 0x18FFFFFF);
        ctx.fill(cx, cy - r, cx + 1, cy + r, 0x18FFFFFF);

        // sweeping scanner beam
        double sweep = (System.currentTimeMillis() % 3000) / 3000.0 * Math.PI * 2;
        for (int i = 0; i < 14; i++) {
            double a = sweep - i * 0.045;
            Rounded.line(ctx, cx, cy, (float) (cx + Math.cos(a) * r), (float) (cy + Math.sin(a) * r),
                    Theme.alpha(Theme.primary(), (int) (60 * (1 - i / 14f))));
        }

        float yaw = mc.player.getYaw(delta);
        double yr = Math.toRadians(yaw);
        double fx = -Math.sin(yr), fz = Math.cos(yr);
        double rx = -Math.cos(yr), rz = -Math.sin(yr);
        Vec3d me = mc.player.getLerpedPos(delta);

        // cardinal letters on the rim (north = -Z)
        String[] dirs = {"N", "E", "S", "W"};
        double[][] vec = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < 4; i++) {
            double sx = vec[i][0] * rx + vec[i][1] * rz;
            double sy = -(vec[i][0] * fx + vec[i][1] * fz);
            int lx = (int) (cx + sx * (r - 2)), ly = (int) (cy + sy * (r - 2));
            ctx.drawCenteredTextWithShadow(tr, dirs[i], lx, ly - 4, i == 0 ? Theme.primary() : 0xFF8C8C9A);
        }

        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            int col;
            int s = 1;
            if (e instanceof PlayerEntity) {
                col = Friends.is(e) ? Theme.primary() : 0xFF55FFFF;
                s = 2;
            } else if (e instanceof HostileEntity) col = 0xFFFF5C6C;
            else if (e instanceof LivingEntity) col = 0xFF4CE08A;
            else if (e instanceof ItemEntity) col = 0xFFFFD84C;
            else continue;
            Vec3d p = e.getLerpedPos(delta);
            double dx = p.x - me.x, dz = p.z - me.z;
            double sx = (dx * rx + dz * rz) / range * r;
            double sy = -(dx * fx + dz * fz) / range * r;
            if (sx * sx + sy * sy > (r - 3) * (r - 3)) continue;
            int px = (int) (cx + sx), py = (int) (cy + sy);
            Rounded.fill(ctx, px - s, py - s, px + s + 1, py + s + 1, s, col);
        }

        // you: arrow pointing up plus a soft view cone
        Rounded.line(ctx, cx, cy, cx - 14, cy - 22, Theme.alpha(Theme.primary(), 0x50));
        Rounded.line(ctx, cx, cy, cx + 14, cy - 22, Theme.alpha(Theme.primary(), 0x50));
        for (int i = 0; i < 5; i++) ctx.fill(cx - i / 2, cy - 3 + i, cx + i / 2 + 1, cy - 2 + i, 0xFFFFFFFF);
    }
}
